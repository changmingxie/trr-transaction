package org.mengyun.compensable.transaction.retry.policy;

import org.mengyun.compensable.transaction.retry.RetryContext;

/**
 * Created by changming.xie on 2/2/16.
 */
public interface RetryPolicy {

    boolean canRetry(RetryContext context);

    void registerThrowable(RetryContext context, Throwable e);

    RetryContext open();
}
