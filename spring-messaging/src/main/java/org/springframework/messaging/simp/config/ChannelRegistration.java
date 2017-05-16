/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.messaging.simp.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * A registration class for customizing the configuration for a
 * {@link org.springframework.messaging.MessageChannel}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ChannelRegistration {

	private TaskExecutorRegistration registration;

	private final List<ChannelInterceptor> interceptors = new ArrayList<>();


	/**
	 * Configure the thread pool backing this message channel.
	 */
	public TaskExecutorRegistration taskExecutor() {
		if (this.registration == null) {
			this.registration = new TaskExecutorRegistration();
		}
		return this.registration;
	}

	/**
	 * Configure the thread pool backing this message channel using a custom
	 * ThreadPoolTaskExecutor.
	 */
	public TaskExecutorRegistration taskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		if (this.registration == null) {
			this.registration = new TaskExecutorRegistration(taskExecutor);
		}
		return this.registration;
	}

	/**
	 * Configure interceptors for the message channel.
	 */
	public ChannelRegistration setInterceptors(ChannelInterceptor... interceptors) {
		if (interceptors != null) {
			this.interceptors.addAll(Arrays.asList(interceptors));
		}
		return this;
	}


	protected boolean hasTaskExecutor() {
		return (this.registration != null);
	}

	protected TaskExecutorRegistration getTaskExecRegistration() {
		return this.registration;
	}

	protected TaskExecutorRegistration getOrCreateTaskExecRegistration() {
		return taskExecutor();
	}

	protected boolean hasInterceptors() {
		return !this.interceptors.isEmpty();
	}

	protected List<ChannelInterceptor> getInterceptors() {
		return this.interceptors;
	}
}
