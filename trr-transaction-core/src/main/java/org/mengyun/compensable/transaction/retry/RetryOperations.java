package org.mengyun.compensable.transaction.retry;

/**
 * Created by changming.xie on 2/2/16.
 */
public interface RetryOperations {

    <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback) throws E;

    <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback, RecoveryCallback<T> recoveryCallback) throws E;

    <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback, RecoveryCallback<T> recoveryCallback, RecoveryCallback<T> rollbackCallback) throws E;
}
