/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
