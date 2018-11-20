package org.mengyun.compensable.transaction.retry.support;

import org.mengyun.compensable.transaction.Transaction;
import org.mengyun.compensable.transaction.TransactionManager;
import org.mengyun.compensable.transaction.TransactionType;
import org.mengyun.compensable.transaction.retry.*;
import org.mengyun.compensable.transaction.retry.backoff.BackOffContext;
import org.mengyun.compensable.transaction.retry.backoff.BackOffPolicy;
import org.mengyun.compensable.transaction.retry.backoff.NoBackOffPolicy;
import org.mengyun.compensable.transaction.retry.policy.RetryPolicy;
import org.mengyun.compensable.transaction.retry.policy.SimpleRetryPolicy;

import java.util.Collections;

/**
 * Created by changming.xie on 2/2/16.
 */
public class RetryTemplate implements RetryOperations {

    private volatile BackOffPolicy backOffPolicy = new NoBackOffPolicy();

    private volatile RetryPolicy retryPolicy =
            new SimpleRetryPolicy(3, Collections.<Class<? extends Throwable>, Boolean>singletonMap(Exception.class, true));

    private TransactionManager transactionManager;


    @Override
    public <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback) throws E {
        return doExecute(retryCallback, null, null);
    }

    @Override
    public <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback, RecoveryCallback<T> recoveryCallback) throws E {
        return doExecute(retryCallback, recoveryCallback, null);
    }

    public <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback, RecoveryCallback<T> recoveryCallback, RecoveryCallback<T> rollbackCallback) throws E {
        return doExecute(retryCallback, recoveryCallback, rollbackCallback);
    }

    public void setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public void setBackOffPolicy(BackOffPolicy backOffPolicy) {
        this.backOffPolicy = backOffPolicy;
    }

    protected <T, E extends Throwable> T doExecute(RetryCallback<T, E> retryCallback,
                                                   RecoveryCallback<T> recoveryCallback, RecoveryCallback<T> rollbackCallback) throws E {

        RetryPolicy retryPolicy = this.retryPolicy;
        BackOffPolicy backOffPolicy = this.backOffPolicy;

        RetryContext context = retryPolicy.open();

        Transaction transaction = null;

        T result = null;

        try {

            if (!transactionManager.isTransactionActive()) {
                transaction = new RetryTransaction(TransactionType.ROOT);
                transactionManager.begin(transaction);
            }

            RetryParticipant participant = new RetryParticipant(
                    recoveryCallback == null ? null : recoveryCallback.getInvocation(), rollbackCallback == null ? null : rollbackCallback.getInvocation());

            transactionManager.enlistParticipant(participant);

            BackOffContext backOffContext = backOffPolicy.start(context);

            Throwable lastException = null;

            while (canRetry(retryPolicy, context)) {

                backOffPolicy.backOff(backOffContext);

                try {
                    result = retryCallback.doWithRetry(context);
                    break;
                } catch (Throwable e) {

                    lastException = e;
                    registerThrowable(retryPolicy, context, e);
                }
            }


            if (lastException != null && recoveryCallback != null) {
                lastException = null;
                try {
                    result = recoveryCallback.recover(context);
                } catch (Throwable e) {
                    //recover failed. recovery job make sure  recover callback eventually called.
                    participant.setNeedRecovery(true);
                }
            }

            try {

                if (lastException == null && participant.getParent() == null) {
                    transactionManager.commit();
                }

                if (lastException != null && participant.getParent() == null) {
                    transactionManager.rollback();
                }

            } catch (Throwable e) {
                // ignore the exception, recovery job recover eventually.
            }

            if (lastException != null) {
                throw RetryTemplate.<E>wrapIfNecessary(lastException);
            }

            return result;

        } finally {
            transactionManager.cleanAfterCompletion(transaction);
        }
    }

    protected boolean canRetry(RetryPolicy retryPolicy, RetryContext context) {
        return retryPolicy.canRetry(context);
    }

    protected void registerThrowable(RetryPolicy retryPolicy, RetryContext context, Throwable e) {
        retryPolicy.registerThrowable(context, e);
    }

    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    private static <E extends Throwable> E wrapIfNecessary(Throwable throwable)
            throws RetryException {
        if (throwable instanceof Error) {
            throw (Error) throwable;
        } else if (throwable instanceof Exception) {
            @SuppressWarnings("unchecked")
            E rethrow = (E) throwable;
            return rethrow;
        } else {
            throw new RetryException("Exception in retry", throwable);
        }
    }
}
