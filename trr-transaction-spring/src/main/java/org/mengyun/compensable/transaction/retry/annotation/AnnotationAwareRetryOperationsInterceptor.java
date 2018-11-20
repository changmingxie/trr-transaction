package org.mengyun.compensable.transaction.retry.annotation;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.mengyun.compensable.transaction.TransactionManager;
import org.mengyun.compensable.transaction.repository.TransactionRepository;
import org.mengyun.compensable.transaction.retry.backoff.*;
import org.mengyun.compensable.transaction.retry.interceptor.MethodInvocationRecoverer;
import org.mengyun.compensable.transaction.retry.interceptor.RetryInterceptorBuilder;
import org.mengyun.compensable.transaction.retry.policy.RetryPolicy;
import org.mengyun.compensable.transaction.retry.policy.SimpleRetryPolicy;
import org.mengyun.compensable.transaction.retry.support.RetryTemplate;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class AnnotationAwareRetryOperationsInterceptor implements IntroductionInterceptor, BeanFactoryAware {

    private static final TemplateParserContext PARSER_CONTEXT = new TemplateParserContext();

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    private final StandardEvaluationContext evaluationContext = new StandardEvaluationContext();

    private final Map<Method, MethodInterceptor> delegates = new HashMap<Method, MethodInterceptor>();

    private Sleeper sleeper;

    private TransactionRepository transactionRepository;

    private BeanFactory beanFactory;

    /**
     * @param sleeper the sleeper to set
     */
    public void setSleeper(Sleeper sleeper) {
        this.sleeper = sleeper;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
        this.evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
    }

    @Override
    public boolean implementsInterface(Class<?> intf) {
        return org.mengyun.compensable.transaction.retry.interceptor.Retryable.class.isAssignableFrom(intf);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        MethodInterceptor delegate = getDelegate(invocation.getThis(), invocation.getMethod());
        if (delegate != null) {
            return delegate.invoke(invocation);
        } else {
            return invocation.proceed();
        }
    }

    private MethodInterceptor getDelegate(Object target, Method method) {
        if (!this.delegates.containsKey(method)) {
            synchronized (this.delegates) {
                if (!this.delegates.containsKey(method)) {
                    Retryable retryable = AnnotationUtils.findAnnotation(method, Retryable.class);
                    if (retryable == null) {
                        retryable = AnnotationUtils.findAnnotation(method.getDeclaringClass(), Retryable.class);
                    }
                    if (retryable == null) {
                        retryable = findAnnotationOnTarget(target, method);
                    }
                    if (retryable == null) {
                        return this.delegates.put(method, null);
                    }
                    MethodInterceptor delegate;
                    if (StringUtils.hasText(retryable.interceptor())) {
                        delegate = this.beanFactory.getBean(retryable.interceptor(), MethodInterceptor.class);
                    } else {
                        delegate = getStatelessInterceptor(target, method, retryable);
                    }

                    this.delegates.put(method, delegate);
                }
            }
        }
        return this.delegates.get(method);
    }

    private Retryable findAnnotationOnTarget(Object target, Method method) {
        try {
            Method targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
            return AnnotationUtils.findAnnotation(targetMethod, Retryable.class);
        } catch (Exception e) {
            return null;
        }
    }

    private MethodInterceptor getStatelessInterceptor(Object target, Method method, Retryable retryable) {
        RetryTemplate template = createTemplate();
        template.setRetryPolicy(getRetryPolicy(retryable));
        template.setBackOffPolicy(getBackoffPolicy(retryable.backoff()));
        template.setTransactionManager(new TransactionManager(this.transactionRepository));
        return RetryInterceptorBuilder.stateless()
                .retryOperations(template)
                .recoverer(getRecoverer(target, method, retryable.recover()))
                .rollback(getRollbacker(target, method, retryable.rollback()))
                .build();
    }


    private RetryTemplate createTemplate() {
        RetryTemplate template = new RetryTemplate();
        return template;
    }

    private MethodInvocationRecoverer getRecoverer(Object target, Method method, String recover) {
        if (StringUtils.hasText(recover)) {
            return new SimpleMethodInvocationRecoverer(target, ReflectionUtils.findMethod(target.getClass(), recover, method.getParameterTypes()));
        }
        return null;
    }

    private MethodInvocationRecoverer getRollbacker(Object target, Method method, String rollback) {
        if (StringUtils.hasText(rollback)) {
            return new SimpleMethodInvocationRecoverer(target, ReflectionUtils.findMethod(target.getClass(), rollback, method.getParameterTypes()));
        }
        return null;
    }

    private RetryPolicy getRetryPolicy(Annotation retryable) {
        Map<String, Object> attrs = AnnotationUtils.getAnnotationAttributes(retryable);
        @SuppressWarnings("unchecked")
        Class<? extends Throwable>[] includes = (Class<? extends Throwable>[]) attrs.get("value");
        if (includes.length == 0) {
            @SuppressWarnings("unchecked")
            Class<? extends Throwable>[] value = (Class<? extends Throwable>[]) attrs.get("include");
            includes = value;
        }
        @SuppressWarnings("unchecked")
        Class<? extends Throwable>[] excludes = (Class<? extends Throwable>[]) attrs.get("exclude");
        Integer maxAttempts = (Integer) attrs.get("maxAttempts");
        String maxAttemptsExpression = (String) attrs.get("maxAttemptsExpression");
        if (StringUtils.hasText(maxAttemptsExpression)) {
            maxAttempts = PARSER.parseExpression(resolve(maxAttemptsExpression), PARSER_CONTEXT)
                    .getValue(this.evaluationContext, Integer.class);
        }
        if (includes.length == 0 && excludes.length == 0) {
            SimpleRetryPolicy simple = new SimpleRetryPolicy();
            simple.setMaxAttempts(maxAttempts);
            return simple;
        }
        Map<Class<? extends Throwable>, Boolean> policyMap = new HashMap<Class<? extends Throwable>, Boolean>();
        for (Class<? extends Throwable> type : includes) {
            policyMap.put(type, true);
        }
        for (Class<? extends Throwable> type : excludes) {
            policyMap.put(type, false);
        }

        return new SimpleRetryPolicy(maxAttempts, policyMap, true);
    }

    private BackOffPolicy getBackoffPolicy(Backoff backoff) {
        long min = backoff.delay() == 0 ? backoff.value() : backoff.delay();

        long max = backoff.maxDelay();

        double multiplier = backoff.multiplier();

        if (multiplier > 0) {
            ExponentialBackOffPolicy policy = new ExponentialBackOffPolicy();
            if (backoff.random()) {
                policy = new ExponentialRandomBackOffPolicy();
            }
            policy.setInitialInterval(min);
            policy.setMultiplier(multiplier);
            policy.setMaxInterval(max > min ? max : ExponentialBackOffPolicy.DEFAULT_MAX_INTERVAL);
            if (this.sleeper != null) {
                policy.setSleeper(this.sleeper);
            }
            return policy;
        }
        if (max > min) {
            UniformRandomBackOffPolicy policy = new UniformRandomBackOffPolicy();
            policy.setMinBackOffPeriod(min);
            policy.setMaxBackOffPeriod(max);
            if (this.sleeper != null) {
                policy.setSleeper(this.sleeper);
            }
            return policy;
        }
        FixedBackOffPolicy policy = new FixedBackOffPolicy();
        policy.setBackOffPeriod(min);
        if (this.sleeper != null) {
            policy.setSleeper(this.sleeper);
        }
        return policy;
    }

    /**
     * Resolve the specified value if possible.
     *
     * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#resolveEmbeddedValue
     */
    private String resolve(String value) {
        if (this.beanFactory != null && this.beanFactory instanceof ConfigurableBeanFactory) {
            return ((ConfigurableBeanFactory) this.beanFactory).resolveEmbeddedValue(value);
        }
        return value;
    }

    public void setTransactionRepository(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }
}
