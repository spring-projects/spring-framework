/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.quartz;

import org.springframework.core.NestedRuntimeException;
import org.springframework.util.MethodInvoker;

/**
 * Unchecked exception that wraps an exception thrown from a target method.
 * Propagated to the Quartz scheduler from a Job that reflectively invokes
 * an arbitrary target method.
 *
 * @author Juergen Hoeller
 * @since 2.5.3
 * @see MethodInvokingJobDetailFactoryBean
 */
@SuppressWarnings("serial")
public class JobMethodInvocationFailedException extends NestedRuntimeException {

	/**
	 * Constructor for JobMethodInvocationFailedException.
	 * @param methodInvoker the MethodInvoker used for reflective invocation
	 * @param cause the root cause (as thrown from the target method)
	 */
	public JobMethodInvocationFailedException(MethodInvoker methodInvoker, Throwable cause) {
		super("Invocation of method '" + methodInvoker.getTargetMethod() +
				"' on target class [" + methodInvoker.getTargetClass() + "] failed", cause);
	}

}
