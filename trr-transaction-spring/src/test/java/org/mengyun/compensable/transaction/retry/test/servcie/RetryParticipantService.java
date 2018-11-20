package org.mengyun.compensable.transaction.retry.test.servcie;

import org.mengyun.compensable.transaction.retry.annotation.Backoff;
import org.mengyun.compensable.transaction.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Created by changming.xie on 8/10/17.
 */
@Service
public class RetryParticipantService {

    public static boolean EXCEPTION_THROW = true;

    private int count = 0;

    @Retryable(maxAttempts = 3, backoff = @Backoff(0), recover = "service2Recover")
    public void service2() {

        System.out.println("service2 called..");

//        throw new IllegalArgumentException();
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(0), rollback = "rollbackService3")
    public void service3(String abc) {

        System.out.println("service3 called...");

        throw new RuntimeException();
    }

    public void service2Recover() {
        System.out.println("service2 recovered...");
        throw new IllegalArgumentException();
    }

    public void rollbackService3(String abc) {
        System.out.println("****rollback3 called****");
        throw new RuntimeException();
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(0))
    public void nestedServiceWithRetry() {

    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(0))
    public void nestedServiceWithRetryFailed() {
        System.out.println("nestedServiceWithRetryFailed called!");
        throw new RuntimeException();
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(0), recover = "recoverOfNestedServiceWithRetryFailedAndRecoverSucceed")
    public void nestedServiceWithRetryFailedAndRecoverSucceed() {
        throw new RuntimeException();
    }

    public void recoverOfNestedServiceWithRetryFailedAndRecoverSucceed() {

    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(0), recover = "recoverOfNestedServiceWithRetryFailedAndRecoverFailed")
    public void nestedServiceWithRetryFailedAndRecoverFailed() {
        throw new RuntimeException();
    }

    public void recoverOfNestedServiceWithRetryFailedAndRecoverFailed() {
        System.out.println("recoverOfNestedServiceWithRetryFailedAndRecoverFailed call begin");
        if (EXCEPTION_THROW) {
            System.out.println("recoverOfNestedServiceWithRetryFailedAndRecoverFailed call failed");
            throw new IllegalArgumentException();
        } else {
            System.out.println("recoverOfNestedServiceWithRetryFailedAndRecoverFailed call succeed");
        }
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(0), rollback = "recoverOfNestedServiceWithRetryFailedAndRollbackSucceed")
    public void nestedServiceWithRetryFailedAndRollbackSucceed() {
        System.out.println("nestedServiceWithRetryFailedAndRollbackSucceed");
        throw new IllegalArgumentException();
    }

    public void recoverOfNestedServiceWithRetryFailedAndRollbackSucceed() {
        System.out.println("recoverOfNestedServiceWithRetryFailedAndRollbackSucceed");
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(0), rollback = "recoverOfNestedServiceWithRetryFailedAndRollbackFailed")
    public void nestedServiceWithRetryFailedAndRollbackFailed() {
        System.out.println("nestedServiceWithRetryFailedAndRollbackFailed");
        throw new RuntimeException();
    }

    public void recoverOfNestedServiceWithRetryFailedAndRollbackFailed() {
        System.out.println("recoverOfNestedServiceWithRetryFailedAndRollbackFailed");

        if (EXCEPTION_THROW) {
            throw new IllegalArgumentException();
        } else {
            System.out.println("recoverOfNestedServiceWithRetryFailedAndRollbackFailed call succeed");
        }
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(0), rollback = "recoverOfNestedServiceWithRetrySucceedAndRollbackSucceed")
    public void nestedServiceWithRetrySucceedAndRollbackSucceed() {
        System.out.println("nestedServiceWithRetrySucceedAndRollbackSucceed");
    }

    public void recoverOfNestedServiceWithRetrySucceedAndRollbackSucceed() {
        System.out.println("recoverOfNestedServiceWithRetrySucceedAndRollbackSucceed called");
    }
}
