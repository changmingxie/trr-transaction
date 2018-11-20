package org.mengyun.compensable.transaction.retry.test.ut;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mengyun.compensable.transaction.Transaction;
import org.mengyun.compensable.transaction.repository.TransactionRepository;
import org.mengyun.compensable.transaction.retry.test.servcie.RetryParticipantService;
import org.mengyun.compensable.transaction.retry.test.servcie.RetryRootService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by changming.xie on 8/10/17.
 */
public class RetryTest extends AbstractTestCase {

    @Autowired
    RetryRootService retryRootService;

    @Autowired
    TransactionRepository transactionRepository;

    @Before
    public void init() throws ParseException {
        RetryParticipantService.EXCEPTION_THROW = true;

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        List<Transaction> transactions = transactionRepository.findAllUnmodifiedSince(formatter.parse("2018-01-01"));

        for (Transaction transaction : transactions) {
            transactionRepository.delete(transaction);
        }
    }

    @After
    public void destory() throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        List<Transaction> transactions = transactionRepository.findAllUnmodifiedSince(formatter.parse("2018-01-01"));

        for (Transaction transaction : transactions) {
            transactionRepository.delete(transaction);
        }

        RetryParticipantService.EXCEPTION_THROW = true;
    }

    @Test
    public void given_simple_retryable_service_then_call_succeed_then_no_transaction_log() {

        retryRootService.serviceWithRetry();

        Transaction foundTransaction = findTransactionFromRepository();

        Assert.assertNull(foundTransaction);
    }


    @Test
    public void given_retryable_service_with_recover_then_call_succeed_then_no_transaction_log() {

        retryRootService.serviceWithRetryAndRecover();

        Transaction foundTransaction = findTransactionFromRepository();

        Assert.assertNull(foundTransaction);
    }

    @Test
    public void given_retryable_service_with_recover_then_call_failed_recover_succeed_then_no_transaction_log() {

        retryRootService.serviceWithRetryFailedAndRecoverSucceed();


        Transaction foundTransaction = findTransactionFromRepository();

        Assert.assertNull(foundTransaction);
    }

    @Test
    public void given_retryable_service_with_recover_then_call_failed_recover_failed_then_has_transaction_log_with_no_exception() {

        retryRootService.serviceWithRetryFailedAndRecoverFailed();

        Transaction foundTransaction = findTransactionFromRepository();

        Assert.assertNotNull(foundTransaction);
    }

    @Test
    public void given_retryable_service_with_rollback_then_call_succeed_then_no_transaction_log() {

        retryRootService.serviceWithRetrySucceedAndRollback();

        Transaction foundTransaction = findTransactionFromRepository();

        Assert.assertNull(foundTransaction);
    }

    @Test
    public void given_retryable_service_with_rollback_then_call_failed_and_rollback_succeed_then_no_transaction_log_and_throw_exception() {

        try {
            retryRootService.serviceWithRetryFailedAndRollbackSucceed();

        } catch (Throwable ex) {
            Assert.assertEquals(IllegalArgumentException.class, ex.getClass());
        }
        Transaction foundTransaction = findTransactionFromRepository();

        Assert.assertNull(foundTransaction);
    }

    @Test
    public void given_retryable_service_with_rollback_then_call_failed_and_rollback_failed_then_has_transaction_log_and_throw_rollback_exception() {

        try {
            retryRootService.serviceWithRetryFailedAndRollbackFailed();

        } catch (Throwable ex) {
            Assert.assertEquals(IllegalArgumentException.class, ex.getClass());
        }

        Transaction foundTransaction = findTransactionFromRepository();

        Assert.assertNotNull(foundTransaction);
    }

    @Test
    public void given_nested_retryable_service_then_call_succeed_then_no_transaction_log() {

        retryRootService.serviceWithNestedRetry();

        Transaction foundTransaction = findTransactionFromRepository();

        Assert.assertNull(foundTransaction);
    }

    @Test
    public void given_nested_retryable_service_then_nested_call_failed_then_no_transaction_log() {

        try {
            retryRootService.serviceWithNestedRetryFailed();
        } catch (Throwable ex) {
            Assert.assertEquals(RuntimeException.class, ex.getClass());
        }

        Transaction foundTransaction = findTransactionFromRepository();

        Assert.assertNull(foundTransaction);
    }

    @Test
    public void given_nested_retryable_service_then_nested_call_failed_but_recover_succeed_then_no_transaction_log() {

        retryRootService.serviceWithNestedRetryFailedAndRecoverSucceed();

        Transaction foundTransaction = findTransactionFromRepository();

        Assert.assertNull(foundTransaction);
    }

    @Test
    public void given_nested_retryable_service_then_nested_call_failed_and_recover_failed_then_has_transaction_log() {

        retryRootService.serviceWithNestedRetryFailedAndRecoverFailed();

        Transaction foundTransaction = findTransactionFromRepository();

        Assert.assertNotNull(foundTransaction);
    }

    @Test
    public void given_nested_retryable_service_then_nested_call_failed_and_rollback_succeed_then_no_transaction_log_with_try_exception() {

        try {
            retryRootService.serviceWithNestedRetryFailedAndRollbackSucceed();
        } catch (Throwable ex) {
            Assert.assertEquals(IllegalArgumentException.class, ex.getClass());
        }

        Transaction foundTransaction = findTransactionFromRepository();

        Assert.assertNull(foundTransaction);
    }


    @Test
    public void given_nested_retryable_service_then_nested_call_failed_and_rollback_failed_then_has_transaction_log_with_rollback_exception() {

        try {
            retryRootService.serviceWithNestedRetryFailedAndRollbackFailed();
        } catch (Throwable ex) {
            Assert.assertEquals(RuntimeException.class, ex.getClass());
        }

        Transaction foundTransaction = findTransactionFromRepository();

        Assert.assertNotNull(foundTransaction);
    }

    @Test
    public void given_nested_retryable_service_then_call_failed_and_recover_succeed_then_no_transaction_log() {

        retryRootService.serviceWithRecoverAndNestedRetryFailed();

        Transaction foundTransaction = findTransactionFromRepository();

        Assert.assertNull(foundTransaction);
    }

    @Test
    public void given_nested_retryable_service_then_call_failed_and_recover_failed_then_has_transaction_log_with_no_exception() {

        retryRootService.serviceWithRecoverFailedAndNestedRetryFailed();

        Transaction foundTransaction = findTransactionFromRepository();

        Assert.assertNotNull(foundTransaction);
    }

    @Test
    public void given_nested_retryable_service_then_call_failed_and_rollback_succeed_then_no_transaction_log_with_try_exception() {

        try {
            retryRootService.serviceWithRollbackSucceedAndNestedRetryFailed();
        } catch (Throwable ex) {
            Assert.assertEquals(RuntimeException.class, ex.getClass());
        }

        Transaction foundTransaction = findTransactionFromRepository();

        Assert.assertNull(foundTransaction);
    }

    @Test
    public void given_multiple_nested_retryable_service_then_last_nested_call_failed_and_other_nested_call_rollback_succeed_then_no_transaction_log_with_nested_try_exception() {

        try {
            retryRootService.serviceWithMultipleNestedRetryAndLastRetryFailed();
        } catch (Throwable ex) {
            Assert.assertEquals(IllegalArgumentException.class, ex.getClass());
        }

        Transaction foundTransaction = findTransactionFromRepository();

        Assert.assertNull(foundTransaction);
    }


    @Test
    public void given_multiple_nested_retryable_service_then_last_nested_call_failed_and_rollback_failed_then_has_transaction_log_with_nested_try_exception() throws IOException, InterruptedException {

        try {
            retryRootService.serviceWithMultipleNestedRetryAndLastRetryFailedAndRollbackFailed();
        } catch (Throwable ex) {
            Assert.assertEquals(RuntimeException.class, ex.getClass());
        }

        Transaction foundTransaction = findTransactionFromRepository();

        Assert.assertNotNull(foundTransaction);
    }

    @Test
    public void given_multiple_nested_retryable_service_when_last_nested_call_failed_and_rollback_failed_and_recovery_job_run_then_transaction_log_recovered_with_nested_try_exception() throws IOException, InterruptedException {

        try {
            retryRootService.serviceWithMultipleNestedRetryAndLastRetryFailedAndRollbackFailed();
        } catch (Throwable ex) {
            Assert.assertEquals(RuntimeException.class, ex.getClass());
        }

        Transaction foundTransaction = findTransactionFromRepository();

        Assert.assertNotNull(foundTransaction);

        Thread.sleep(1000 * 5l);

        RetryParticipantService.EXCEPTION_THROW = false;

        Thread.sleep(1000 * 5l);
    }

    @Test
    public void given_multiple_nested_retryable_service_when_all_nested_call_failed_and_recover_failed_and_recovery_job_run_then_transaction_log_recovered_with_no_exception() throws IOException, InterruptedException {

        retryRootService.serviceWithMultipleNestedRetryAndAllRetryFailedAndRecoverFailedAndJobRecoverSucceed();

        Transaction foundTransaction = findTransactionFromRepository();

        Assert.assertNotNull(foundTransaction);

        Thread.sleep(1000 * 5l);

        RetryParticipantService.EXCEPTION_THROW = false;

        Thread.sleep(1000 * 5l);

        foundTransaction = findTransactionFromRepository();

        Assert.assertNull(foundTransaction);
    }

    @Test
    public void testSerialize() {

        retryRootService.serviceWithNestedRetryFailedAndRecoverFailed();

        Transaction foundTransaction = findTransactionFromRepository();

        System.out.println(JSON.toJSONString(foundTransaction));
        System.out.println(JSON.toJSONString(JSON.parse(JSON.toJSONBytes(foundTransaction))));
    }


    private Transaction findTransactionFromRepository() {
        Transaction foundTransaction = null;


        List<Transaction> foundTransactions = transactionRepository.findAllUnmodifiedSince(DateUtils.addSeconds(new Date(), 1));

        if (foundTransactions != null && foundTransactions.size() > 0) {
            foundTransaction = foundTransactions.get(0);
        }
        return foundTransaction;
    }
}
