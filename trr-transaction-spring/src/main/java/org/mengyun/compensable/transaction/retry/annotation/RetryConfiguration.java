package org.mengyun.compensable.transaction.retry.annotation;

import org.mengyun.compensable.transaction.SystemException;
import org.mengyun.compensable.transaction.TransactionManager;
import org.mengyun.compensable.transaction.recovery.DefaultRecoverConfig;
import org.mengyun.compensable.transaction.recovery.RecoverConfig;
import org.mengyun.compensable.transaction.recovery.RecoverScheduledJob;
import org.mengyun.compensable.transaction.recovery.TransactionRecovery;
import org.mengyun.compensable.transaction.repository.CachableTransactionRepository;
import org.mengyun.compensable.transaction.repository.TransactionRepository;
import org.mengyun.compensable.transaction.support.TransactionConfigurator;
import org.aopalliance.aop.Advice;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.aop.support.annotation.AnnotationClassFilter;
import org.springframework.aop.support.annotation.AnnotationMethodMatcher;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;

import javax.annotation.PostConstruct;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("serial")
public class RetryConfiguration extends AbstractPointcutAdvisor implements IntroductionAdvisor, BeanFactoryAware, TransactionConfigurator {

    private Advice advice;

    private Pointcut pointcut;

    private BeanFactory beanFactory;

    private TransactionManager transactionManager;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired(required = false)
    private RecoverConfig recoverConfig = DefaultRecoverConfig.INSTANCE;

    @Autowired(required = false)
    private Scheduler scheduler;


    @PostConstruct
    public void init() {
        Set<Class<? extends Annotation>> retryableAnnotationTypes = new LinkedHashSet<Class<? extends Annotation>>(1);
        retryableAnnotationTypes.add(Retryable.class);
        this.pointcut = buildPointcut(retryableAnnotationTypes);
        this.advice = buildAdvice();
        if (this.advice instanceof BeanFactoryAware) {
            ((BeanFactoryAware) this.advice).setBeanFactory(beanFactory);
        }

        transactionManager = new TransactionManager();
        transactionManager.setTransactionRepository(transactionRepository);

        if (transactionRepository instanceof CachableTransactionRepository) {
            ((CachableTransactionRepository) transactionRepository).setExpireDuration(recoverConfig.getRecoverDuration());
        }

        TransactionRecovery transactionRecovery = new TransactionRecovery();
        transactionRecovery.setTransactionConfigurator(this);

        RecoverScheduledJob recoveryScheduledJob = new RecoverScheduledJob();
        recoveryScheduledJob.setJobName("retryRecoverJob");
        recoveryScheduledJob.setTriggerName("retryTrigger");

        recoveryScheduledJob.setTransactionRecovery(transactionRecovery);
        recoveryScheduledJob.setCronExpression(getRecoverConfig().getCronExpression());

        if (scheduler == null) {
            SchedulerFactory schedulerFactory = new org.quartz.impl.StdSchedulerFactory();
            try {
                scheduler = schedulerFactory.getScheduler();
            } catch (SchedulerException e) {
                throw new SystemException(e);
            }
        }

        recoveryScheduledJob.setScheduler(scheduler);
        recoveryScheduledJob.init();
    }


    /**
     * Set the {@code BeanFactory} to be used when looking up executors by qualifier.
     */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public ClassFilter getClassFilter() {
        return pointcut.getClassFilter();
    }

    @Override
    public Class<?>[] getInterfaces() {
        return new Class[]{org.mengyun.compensable.transaction.retry.interceptor.Retryable.class};
    }

    @Override
    public void validateInterfaces() throws IllegalArgumentException {
    }

    @Override
    public Advice getAdvice() {
        return this.advice;
    }

    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }

    protected Advice buildAdvice() {
        AnnotationAwareRetryOperationsInterceptor interceptor = new AnnotationAwareRetryOperationsInterceptor();

        interceptor.setTransactionRepository(transactionRepository);

        return interceptor;
    }

    /**
     * Calculate a pointcut for the given retry annotation types, if any.
     *
     * @param retryAnnotationTypes the retry annotation types to introspect
     * @return the applicable Pointcut object, or {@code null} if none
     */
    protected Pointcut buildPointcut(Set<Class<? extends Annotation>> retryAnnotationTypes) {
        ComposablePointcut result = null;
        for (Class<? extends Annotation> retryAnnotationType : retryAnnotationTypes) {
            Pointcut filter = new AnnotationClassOrMethodPointcut(retryAnnotationType);
            if (result == null) {
                result = new ComposablePointcut(filter);
            } else {
                result.union(filter);
            }
        }
        return result;
    }

    @Override
    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    @Override
    public TransactionRepository getTransactionRepository() {
        return transactionRepository;
    }

    @Override
    public RecoverConfig getRecoverConfig() {

        return recoverConfig;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    private final class AnnotationClassOrMethodPointcut extends StaticMethodMatcherPointcut {

        private final MethodMatcher methodResolver;

        AnnotationClassOrMethodPointcut(Class<? extends Annotation> annotationType) {
            this.methodResolver = new AnnotationMethodMatcher(annotationType);
            setClassFilter(new AnnotationClassOrMethodFilter(annotationType));
        }

        @Override
        public boolean matches(Method method, Class<?> targetClass) {
            return getClassFilter().matches(targetClass) || this.methodResolver.matches(method, targetClass);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof AnnotationClassOrMethodPointcut)) {
                return false;
            }
            AnnotationClassOrMethodPointcut otherAdvisor = (AnnotationClassOrMethodPointcut) other;
            return ObjectUtils.nullSafeEquals(this.methodResolver, otherAdvisor.methodResolver);
        }

    }

    private final class AnnotationClassOrMethodFilter extends AnnotationClassFilter {

        private final AnnotationMethodsResolver methodResolver;

        AnnotationClassOrMethodFilter(Class<? extends Annotation> annotationType) {
            super(annotationType, true);
            this.methodResolver = new AnnotationMethodsResolver(annotationType);
        }

        @Override
        public boolean matches(Class<?> clazz) {
            return super.matches(clazz) || this.methodResolver.hasAnnotatedMethods(clazz);
        }

    }

    private static class AnnotationMethodsResolver {

        private Class<? extends Annotation> annotationType;

        public AnnotationMethodsResolver(Class<? extends Annotation> annotationType) {
            this.annotationType = annotationType;
        }

        public boolean hasAnnotatedMethods(Class<?> clazz) {
            final AtomicBoolean found = new AtomicBoolean(false);
            ReflectionUtils.doWithMethods(clazz,
                    new MethodCallback() {
                        @Override
                        public void doWith(Method method) throws IllegalArgumentException,
                                IllegalAccessException {
                            if (found.get()) {
                                return;
                            }
                            Annotation annotation = AnnotationUtils.findAnnotation(method,
                                    annotationType);
                            if (annotation != null) {
                                found.set(true);
                            }
                        }
                    });
            return found.get();
        }

    }

}
