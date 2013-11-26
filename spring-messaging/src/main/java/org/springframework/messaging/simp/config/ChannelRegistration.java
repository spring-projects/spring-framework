/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.messaging.simp.config;

import org.springframework.messaging.support.channel.ChannelInterceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A registration class for customizing the configuration for a
 * {@link org.springframework.messaging.MessageChannel}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ChannelRegistration {

	private TaskExecutorRegistration taskExecutorRegistration;

	private List<ChannelInterceptor> interceptors = new ArrayList<ChannelInterceptor>();


	/**
	 * Configure properties of the ThreadPoolTaskExecutor backing the message channel.
	 */
	public TaskExecutorRegistration taskExecutor() {
		this.taskExecutorRegistration = new TaskExecutorRegistration();
		return this.taskExecutorRegistration;
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
		return (this.taskExecutorRegistration != null);
	}

	protected TaskExecutorRegistration getTaskExecutorRegistration() {
		return this.taskExecutorRegistration;
	}

	protected boolean hasInterceptors() {
		return !this.interceptors.isEmpty();
	}

	protected List<ChannelInterceptor> getInterceptors() {
		return this.interceptors;
	}
}
