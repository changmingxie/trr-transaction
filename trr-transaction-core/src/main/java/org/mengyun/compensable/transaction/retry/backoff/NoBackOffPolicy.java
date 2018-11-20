package org.mengyun.compensable.transaction.retry.backoff;

import org.mengyun.compensable.transaction.retry.RetryContext;

/**
 * Created by changming.xie on 2/2/16.
 */
public class NoBackOffPolicy implements BackOffPolicy {


    @Override
    public BackOffContext start(RetryContext context) {
        return null;
    }

    @Override
    public void backOff(BackOffContext backOffContext) {

    }
}
