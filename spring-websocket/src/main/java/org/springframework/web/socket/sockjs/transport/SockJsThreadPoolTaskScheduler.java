/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.sockjs.transport;

import org.springframework.lang.UsesJava7;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ClassUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

/**
 * An extension of ThreadPoolTaskScheduler optimized for managing a large number
 * of task, e.g. setting he pool size to the number of available processors and
 * setting the setRemoveOnCancelPolicy property of
 * {@link java.util.concurrent.ScheduledThreadPoolExecutor} available in JDK 1.7
 * or higher in order to avoid keeping cancelled tasks around.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
@SuppressWarnings("serial")
public class SockJsThreadPoolTaskScheduler extends ThreadPoolTaskScheduler {

	// Check for setRemoveOnCancelPolicy method - available on JDK 7 and higher
	private static boolean hasRemoveOnCancelPolicyMethod = ClassUtils.hasMethod(
			ScheduledThreadPoolExecutor.class, "setRemoveOnCancelPolicy", Boolean.class);


	public SockJsThreadPoolTaskScheduler() {
		setThreadNamePrefix("SockJS-");
		setPoolSize(Runtime.getRuntime().availableProcessors());
	}


	@Override
	protected ExecutorService initializeExecutor(ThreadFactory factory, RejectedExecutionHandler handler) {
		ExecutorService service = super.initializeExecutor(factory, handler);
		configureRemoveOnCancelPolicy((ScheduledThreadPoolExecutor) service);
		return service;
	}

	@UsesJava7 // guard setting removeOnCancelPolicy (safe with 1.6 due to hasRemoveOnCancelPolicyMethod check)
	private void configureRemoveOnCancelPolicy(ScheduledThreadPoolExecutor service) {
		if (hasRemoveOnCancelPolicyMethod) {
			service.setRemoveOnCancelPolicy(true);
		}
	}

}
