package org.mengyun.compensable.transaction.retry;

import org.mengyun.compensable.transaction.Invocation;

/**
 * Created by changming.xie on 2/2/16.
 */
public interface RecoveryCallback<T> {

    Invocation getInvocation();

    T recover(RetryContext context) throws Throwable;

}