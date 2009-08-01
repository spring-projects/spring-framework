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

package org.springframework.scheduling.concurrent;

import java.util.concurrent.Future;

import org.springframework.scheduling.support.DelegatingErrorHandlingRunnable;
import org.springframework.scheduling.support.ErrorHandler;

/**
 * @author Mark Fisher
 * @since 3.0
 */
abstract class TaskUtils {

	/**
	 * Decorates the task for error handling. If the provided
	 * {@link ErrorHandler} is not null, it will be used. Otherwise,
	 * repeating tasks will have errors suppressed by default whereas
	 * one-shot tasks will have errors propagated by default since those
	 * errors may be expected through the returned {@link Future}. In both
	 * cases, the errors will be logged.
	 */
	static DelegatingErrorHandlingRunnable errorHandlingTask(
			Runnable task, ErrorHandler errorHandler, boolean isRepeatingTask) {

		if (task instanceof DelegatingErrorHandlingRunnable) {
			return (DelegatingErrorHandlingRunnable) task;
		}
		ErrorHandler eh = errorHandler != null ? errorHandler : getDefaultErrorHandler(isRepeatingTask);
		return new DelegatingErrorHandlingRunnable(task, eh);
	}

	static ErrorHandler getDefaultErrorHandler(boolean isRepeatingTask) {
		return (isRepeatingTask ? ErrorHandler.LOG_AND_SUPPRESS : ErrorHandler.LOG_AND_PROPAGATE);
	}

}
