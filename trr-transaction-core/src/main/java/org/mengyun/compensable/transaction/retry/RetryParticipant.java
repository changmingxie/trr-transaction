package org.mengyun.compensable.transaction.retry;

import org.mengyun.compensable.transaction.Invocation;
import org.mengyun.compensable.transaction.MethodInvocation;
import org.mengyun.compensable.transaction.Participant;
import org.mengyun.compensable.transaction.TransactionXid;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by changmingxie on 10/27/15.
 */
public class RetryParticipant implements Participant {

    private static final long serialVersionUID = 4127729421281425247L;

    private TransactionXid xid;

    private Invocation confirmInvocationContext;

    private Invocation cancelInvocationContext;

    private List<RetryParticipant> children = new ArrayList<RetryParticipant>();

    private RetryParticipant parent;

    private boolean needRecovery;

    public RetryParticipant() {

    }

    public RetryParticipant(TransactionXid xid, MethodInvocation confirmInvocationContext, MethodInvocation cancelInvocationContext) {
        this.xid = xid;
        this.confirmInvocationContext = confirmInvocationContext;
        this.cancelInvocationContext = cancelInvocationContext;
    }

    public RetryParticipant(Invocation confirmInvocationContext, Invocation cancelInvocationContext) {
        this.xid = new TransactionXid();
        this.confirmInvocationContext = confirmInvocationContext;
        this.cancelInvocationContext = cancelInvocationContext;
    }

    public void rollback() throws Throwable {
        if (cancelInvocationContext != null) {
            this.cancelInvocationContext.proceed();
        }
    }

    public void commit() throws Throwable {
        if (confirmInvocationContext != null && needRecovery) {
            this.confirmInvocationContext.proceed();
        }

        for (RetryParticipant child : children) {
            child.commit();
        }
    }

    @Override
    public void addChild(Participant participant) {

        RetryParticipant child = (RetryParticipant) participant;

        this.children.add(child);
        child.setParent(this);
    }

    public TransactionXid getXid() {
        return xid;
    }

    public void setXid(TransactionXid xid) {
        this.xid = xid;
    }

    public void setConfirmInvocationContext(Invocation invocation) {
        this.confirmInvocationContext = invocation;
    }

    public Invocation getConfirmInvocationContext() {
        return this.confirmInvocationContext;
    }

    public void setCancelInvocationContext(Invocation invocation) {
        this.cancelInvocationContext = invocation;
    }

    @Override
    public RetryParticipant getParent() {
        return parent;
    }

    public void setParent(RetryParticipant parent) {
        this.parent = parent;
    }

    public void setNeedRecovery(boolean needRecovery) {
        this.needRecovery = needRecovery;
    }

    public boolean isNeedRecovery() {

        if (needRecovery) {
            return true;
        }

        for (RetryParticipant child : this.children) {
            if (child.isNeedRecovery()) {
                return true;
            }
        }

        return false;
    }

    public List<RetryParticipant> getChildren() {
        return this.children;
    }

    public void setChildren(List<RetryParticipant> children) {
        this.children = children;
    }
}
