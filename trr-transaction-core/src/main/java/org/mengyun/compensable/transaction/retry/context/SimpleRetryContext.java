package org.mengyun.compensable.transaction.retry.context;

import org.mengyun.compensable.transaction.retry.RetryContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by changming.xie on 2/2/16.
 */
public class SimpleRetryContext implements RetryContext {

    private volatile int count;

    private volatile Throwable lastException;

    private List<RetryContext> children = new ArrayList<RetryContext>();

    private RetryContext parent;

    public SimpleRetryContext() {

    }

    @Override
    public int getRetryCount() {
        return count;
    }

    @Override
    public Throwable getLastThrowable() {
        return lastException;
    }

    @Override
    public void registerThrowable(Throwable throwable) {
        this.lastException = throwable;
        if (throwable != null)
            count++;
    }

    @Override
    public void setNeedRecovery(boolean needRecovery) {

    }
    
}
