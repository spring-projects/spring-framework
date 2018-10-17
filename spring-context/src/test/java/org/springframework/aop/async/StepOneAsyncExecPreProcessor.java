package org.springframework.aop.async;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.AsyncExecutionPreProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * @author huqichao
 * @date 2018-10-16 18:19
 */
@Order(2)
@Configuration
public class StepOneAsyncExecPreProcessor implements AsyncExecutionPreProcessor {
	/**
	 * Performs pre processing before asynchronous execution.
	 *
	 * @param invocation the method to intercept and make asynchronous
	 */
	@Override
	public void preProcessBeforeAsyncExecution(MethodInvocation invocation) {
		System.out.println("StepOneAsyncExecPreProcessor start");
	}
}
