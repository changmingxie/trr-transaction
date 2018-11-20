package org.mengyun.compensable.transaction.retry.backoff;


import org.mengyun.compensable.transaction.retry.RetryContext;

/**
 * Created by changming.xie on 2/2/16.
 */

public interface BackOffPolicy {

    BackOffContext start(RetryContext context);

    void backOff(BackOffContext backOffContext);

}