/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.scheduling.support;

/**
 * A strategy for handling errors that occur during asynchronous
 * execution of tasks that have been submitted to a TaskScheduler.
 * 
 * @author Mark Fisher
 * @since 3.0.
 */
public interface ErrorHandler {

	/**
	 * An ErrorHandler strategy that will log the Exception but perform
	 * no further handling. This will suppress the error so that
	 * subsequent executions of the task will not be prevented.
	 */
	static final ErrorHandler LOG_AND_SUPPRESS = new LoggingErrorHandler();

	/**
	 * An ErrorHandler strategy that will log at error level and then
	 * re-throw the Exception. Note: this will typically prevent subsequent
	 * execution of a scheduled task.
	 */
	static final ErrorHandler LOG_AND_PROPAGATE = new PropagatingErrorHandler();


	void handleError(Throwable t);

}
