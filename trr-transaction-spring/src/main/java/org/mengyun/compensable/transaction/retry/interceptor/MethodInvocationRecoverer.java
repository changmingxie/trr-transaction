package org.mengyun.compensable.transaction.retry.interceptor;


import java.lang.reflect.Method;

public interface MethodInvocationRecoverer {

    Object getTarget();

    Method getMethod();
}
