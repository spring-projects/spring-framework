package org.springframework.aop;

/**
 * 在异步执行之前，执行一些异步提前处理
 * @author huqichao
 * @date 2018-10-15 17:35
 */
public interface AsyncExecutionPreProcessor {

    void preProcessBeforeAsyncExecution();
}
