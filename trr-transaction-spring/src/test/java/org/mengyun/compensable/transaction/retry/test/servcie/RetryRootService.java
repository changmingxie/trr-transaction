package org.mengyun.compensable.transaction.retry.test.servcie;


import org.mengyun.compensable.transaction.retry.annotation.Backoff;
import org.mengyun.compensable.transaction.retry.annotation.Retryable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by changming.xie on 8/10/17.
 */
@Service
public class RetryRootService {


    @Autowired
    RetryParticipantService retryParticipantService;

    @Retryable(maxAttempts = 2)
    public void service() {

        System.out.println("begin root");

        retryParticipantService.service2();

        retryParticipantService.service3("abc");
    }


    @Retryable(maxAttempts = 3, backoff = @Backoff(0))
    public void serviceWithRetry() {

        System.out.println("serviceWithRetry called");
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(0), recover = "serviceWithRetry")
    public void serviceWithRetryAndRecover() {

    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(0), recover = "recoverOfServiceWithRetryFailedAndRecoverSucceed")
    public void serviceWithRetryFailedAndRecoverSucceed() {

        throw new RuntimeException();
    }

    public void recoverOfServiceWithRetryFailedAndRecoverSucceed() {

    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(0), recover = "recoverOfServiceWithRetryFailedAndRecoverFailed")
    public void serviceWithRetryFailedAndRecoverFailed() {
        throw new RuntimeException();
    }

    public void recoverOfServiceWithRetryFailedAndRecoverFailed() {
        throw new RuntimeException();
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(0), rollback = "rollbackOfServiceWithRetrySucceedAndRollback")
    public void serviceWithRetrySucceedAndRollback() {

    }

    public void rollbackOfServiceWithRetrySucceedAndRollback() {
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(0), rollback = "rollbackOfServiceWithRetryFailedAndRollbackSucceed")
    public void serviceWithRetryFailedAndRollbackSucceed() {
        throw new IllegalArgumentException();
    }

    public void rollbackOfServiceWithRetryFailedAndRollbackSucceed() {

    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(0), rollback = "rollbackOfServiceWithRetryFailedAndRollbackFailed")
    public void serviceWithRetryFailedAndRollbackFailed() {
        throw new IllegalArgumentException();
    }

    public void rollbackOfServiceWithRetryFailedAndRollbackFailed() {
        throw new RuntimeException();
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(0))
    public void serviceWithNestedRetry() {

        retryParticipantService.nestedServiceWithRetry();
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(0))
    public void serviceWithNestedRetryFailed() {

        retryParticipantService.nestedServiceWithRetryFailed();
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(0))
    public void serviceWithNestedRetryFailedAndRecoverSucceed() {
        retryParticipantService.nestedServiceWithRetryFailedAndRecoverSucceed();
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(0))
    public void serviceWithNestedRetryFailedAndRecoverFailed() {
        retryParticipantService.nestedServiceWithRetryFailedAndRecoverFailed();
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(0))
    public void serviceWithNestedRetryFailedAndRollbackSucceed() {
        System.out.println("serviceWithNestedRetryFailedAndRollbackSucceed");
        retryParticipantService.nestedServiceWithRetryFailedAndRollbackSucceed();
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(0))
    public void serviceWithNestedRetryFailedAndRollbackFailed() {
        retryParticipantService.nestedServiceWithRetryFailedAndRollbackFailed();
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(0), recover = "recoverOfServiceWithRecoverAndNestedRetryFailed")
    public void serviceWithRecoverAndNestedRetryFailed() {
        retryParticipantService.nestedServiceWithRetryFailed();
    }

    public void recoverOfServiceWithRecoverAndNestedRetryFailed() {

    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(0), recover = "recoverOfServiceWithRecoverFailedAndNestedRetryFailed")
    public void serviceWithRecoverFailedAndNestedRetryFailed() {
        retryParticipantService.nestedServiceWithRetryFailed();
    }

    public void recoverOfServiceWithRecoverFailedAndNestedRetryFailed() {
            throw new IllegalArgumentException();
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(0), rollback = "rollbackOfServiceWithRollbackSucceedAndNestedRetryFailed")
    public void serviceWithRollbackSucceedAndNestedRetryFailed() {
        retryParticipantService.nestedServiceWithRetryFailed();
    }

    public void rollbackOfServiceWithRollbackSucceedAndNestedRetryFailed() {
    }

    @Retryable(maxAttempts = 1, backoff = @Backoff(0))
    public void serviceWithMultipleNestedRetryAndLastRetryFailed() {

        System.out.println("serviceWithMultipleNestedRetryAndLastRetryFailed");

        retryParticipantService.nestedServiceWithRetrySucceedAndRollbackSucceed();

        retryParticipantService.nestedServiceWithRetrySucceedAndRollbackSucceed();

        retryParticipantService.nestedServiceWithRetryFailedAndRollbackSucceed();

    }

    @Retryable(maxAttempts = 1, backoff = @Backoff(0))
    public void serviceWithMultipleNestedRetryAndLastRetryFailedAndRollbackFailed() {

        System.out.println("serviceWithMultipleNestedRetryAndLastRetryFailed");

        retryParticipantService.nestedServiceWithRetrySucceedAndRollbackSucceed();

        retryParticipantService.nestedServiceWithRetrySucceedAndRollbackSucceed();

        retryParticipantService.nestedServiceWithRetryFailedAndRollbackFailed();

    }

    @Retryable(maxAttempts = 1, backoff = @Backoff(0))
    public void serviceWithMultipleNestedRetryAndAllRetryFailedAndRecoverFailedAndJobRecoverSucceed() {

        System.out.println("serviceWithMultipleNestedRetryAndAllRetryFailedAndRecoverFailedAndJobRecoverSucceed");

        retryParticipantService.nestedServiceWithRetryFailedAndRecoverFailed();

        retryParticipantService.nestedServiceWithRetryFailedAndRecoverFailed();

        retryParticipantService.nestedServiceWithRetryFailedAndRecoverFailed();

    }
}
