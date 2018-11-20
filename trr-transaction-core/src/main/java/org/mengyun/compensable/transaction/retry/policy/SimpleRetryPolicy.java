package org.mengyun.compensable.transaction.retry.policy;

import org.mengyun.compensable.transaction.retry.classify.BinaryExceptionClassifier;
import org.mengyun.compensable.transaction.retry.context.SimpleRetryContext;
import org.mengyun.compensable.transaction.retry.RetryContext;

import java.util.Collections;
import java.util.Map;

/**
 * Created by changming.xie on 2/2/16.
 */

public class SimpleRetryPolicy implements RetryPolicy {


    public final static int DEFAULT_MAX_ATTEMPTS = 3;

    private volatile int maxAttempts;

    private BinaryExceptionClassifier retryableClassifier = new BinaryExceptionClassifier(false);


    public SimpleRetryPolicy() {
        this(DEFAULT_MAX_ATTEMPTS, Collections
                .<Class<? extends Throwable>, Boolean>singletonMap(Exception.class, true));
    }


    public SimpleRetryPolicy(int maxAttempts, Map<Class<? extends Throwable>, Boolean> retryableExceptions) {
        this(maxAttempts, retryableExceptions, false);
    }


    public SimpleRetryPolicy(int maxAttempts, Map<Class<? extends Throwable>, Boolean> retryableExceptions,
                             boolean traverseCauses) {
        super();
        this.maxAttempts = maxAttempts;
        this.retryableClassifier = new BinaryExceptionClassifier(retryableExceptions);
        this.retryableClassifier.setTraverseCauses(traverseCauses);
    }


    public void setMaxAttempts(int retryAttempts) {
        this.maxAttempts = retryAttempts;
    }


    public boolean canRetry(RetryContext context) {
        Throwable t = context.getLastThrowable();
        return (t == null || retryForException(t)) && context.getRetryCount() < maxAttempts;
    }

    public void registerThrowable(RetryContext context, Throwable throwable) {
        SimpleRetryContext simpleContext = ((SimpleRetryContext) context);
        simpleContext.registerThrowable(throwable);
    }

    public RetryContext open() {

        RetryContext retryContext = new SimpleRetryContext();
        
        return retryContext;
    }

    private boolean retryForException(Throwable ex) {
        return retryableClassifier.classify(ex);
    }
}
