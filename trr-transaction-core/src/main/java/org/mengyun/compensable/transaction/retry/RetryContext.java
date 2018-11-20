package org.mengyun.compensable.transaction.retry;

/**
 * Created by changming.xie on 2/2/16.
 */
public interface RetryContext {

    int getRetryCount();

    Throwable getLastThrowable();

    public void registerThrowable(Throwable throwable);

    void setNeedRecovery(boolean needRecovery);
}
