package org.springframework.aop;

import org.aopalliance.intercept.MethodInvocation;

/**
 * Performs pre processing before asynchronous execution.
 * @author huqichao
 * @date 2018-10-15 17:35
 */
public interface AsyncExecutionPreProcessor {

    /**
     * Performs pre processing before asynchronous execution.
     * @param invocation the method to intercept and make asynchronous
     */
    void preProcessBeforeAsyncExecution(MethodInvocation invocation);
}
