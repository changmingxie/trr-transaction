package org.mengyun.compensable.transaction.retry.annotation;

import org.mengyun.compensable.transaction.retry.interceptor.MethodInvocationRecoverer;

import java.lang.reflect.Method;

/**
 * Created by changming.xie on 8/24/17.
 */
public class SimpleMethodInvocationRecoverer implements MethodInvocationRecoverer {

    private Object target;

    private Method method;

    public SimpleMethodInvocationRecoverer(Object target, Method method) {
        this.target = target;
        this.method = method;
    }

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public Method getMethod() {
        return method;
    }
}
