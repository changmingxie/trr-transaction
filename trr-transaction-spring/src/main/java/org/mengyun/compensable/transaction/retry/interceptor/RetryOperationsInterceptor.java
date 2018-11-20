
package org.mengyun.compensable.transaction.retry.interceptor;

import org.mengyun.compensable.transaction.Invocation;
import org.mengyun.compensable.transaction.retry.RecoveryCallback;
import org.mengyun.compensable.transaction.retry.RetryCallback;
import org.mengyun.compensable.transaction.retry.RetryContext;
import org.mengyun.compensable.transaction.retry.RetryOperations;
import org.mengyun.compensable.transaction.retry.support.RetryTemplate;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Arrays;

public class RetryOperationsInterceptor implements MethodInterceptor {

    private RetryOperations retryOperations = new RetryTemplate();

    private MethodInvocationRecoverer recoverer;

    private MethodInvocationRecoverer rollbacker;

    private String label;

    public void setLabel(String label) {
        this.label = label;
    }

    public void setRetryOperations(RetryOperations retryTemplate) {
        Assert.notNull(retryTemplate, "'retryOperations' cannot be null.");
        this.retryOperations = retryTemplate;
    }

    public void setRecoverer(MethodInvocationRecoverer recoverer) {
        this.recoverer = recoverer;
    }

    public void setRollbacker(MethodInvocationRecoverer rollbacker) {
        this.rollbacker = rollbacker;
    }

    public Object invoke(final MethodInvocation invocation) throws Throwable {

        String name;
        if (StringUtils.hasText(label)) {
            name = label;
        } else {
            name = invocation.getMethod().toGenericString();
        }
        final String label = name;

        RetryCallback<Object, Throwable> retryCallback = new RetryCallback<Object, Throwable>() {

            public Object doWithRetry(RetryContext context) throws Exception {

				/*
                 * If we don't copy the invocation carefully it won't keep a reference to
				 * the other interceptors in the chain. We don't have a choice here but to
				 * specialise to ReflectiveMethodInvocation (but how often would another
				 * implementation come along?).
				 */
                if (invocation instanceof ProxyMethodInvocation) {
                    try {
                        return ((ProxyMethodInvocation) invocation).invocableClone().proceed();
                    } catch (Exception e) {
                        throw e;
                    } catch (Error e) {
                        throw e;
                    } catch (Throwable e) {
                        throw new IllegalStateException(e);
                    }
                } else {
                    throw new IllegalStateException(
                            "MethodInvocation of the wrong type detected - this should not happen with Spring AOP, " +
                                    "so please raise an issue if you see this exception");
                }
            }
        };

        ItemRecovererCallback recoveryCallback = null;

        ItemRecovererCallback rollbackCallback = null;

        if (recoverer != null) {
            recoveryCallback = new ItemRecovererCallback(
                    invocation.getArguments(), recoverer);
        }

        if (rollbacker != null) {

            rollbackCallback = new ItemRecovererCallback(
                    invocation.getArguments(), rollbacker);
        }

        return this.retryOperations.execute(retryCallback, recoveryCallback, rollbackCallback);
    }


    /**
     * @author Dave Syer
     */
    private static final class ItemRecovererCallback implements RecoveryCallback<Object> {

        private static final long serialVersionUID = -981297505221621327L;

        private Invocation invocation = null;

        /**
         * @param args the item that failed.
         */
        private ItemRecovererCallback(Object[] args, MethodInvocationRecoverer recoverer) {

            this.invocation = new org.mengyun.compensable.transaction.MethodInvocation(recoverer.getTarget().getClass(),
                    recoverer.getMethod().getName(), recoverer.getMethod().getParameterTypes(), Arrays.asList(args).toArray());
        }

        @Override
        public Invocation getInvocation() {
            return this.invocation;
        }

        public Object recover(RetryContext context) throws Throwable {
            return this.invocation.proceed();
        }
    }

}
