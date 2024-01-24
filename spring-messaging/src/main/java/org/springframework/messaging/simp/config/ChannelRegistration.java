/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.core.task.TaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * A registration class for customizing the configuration for a
 * {@link org.springframework.messaging.MessageChannel}.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Nicoll
 * @since 4.0
 */
public class ChannelRegistration {

	@Nullable
	private TaskExecutorRegistration registration;

	@Nullable
	private TaskExecutor executor;

	private final List<ChannelInterceptor> interceptors = new ArrayList<>();


	/**
	 * Configure the thread pool backing this message channel.
	 */
	public TaskExecutorRegistration taskExecutor() {
		return taskExecutor(null);
	}

	/**
	 * Configure the thread pool backing this message channel using a custom
	 * ThreadPoolTaskExecutor.
	 * @param taskExecutor the executor to use (or {@code null} for a default executor)
	 */
	public TaskExecutorRegistration taskExecutor(@Nullable ThreadPoolTaskExecutor taskExecutor) {
		if (this.registration == null) {
			this.registration = (taskExecutor != null ? new TaskExecutorRegistration(taskExecutor) :
					new TaskExecutorRegistration());
		}
		return this.registration;
	}

	/**
	 * Configure the given {@link TaskExecutor} for this message channel,
	 * taking precedence over a {@linkplain #taskExecutor() task executor
	 * registration} if any.
	 * @param taskExecutor the task executor to use
	 * @since 6.1.4
	 */
	public ChannelRegistration executor(TaskExecutor taskExecutor) {
		this.executor = taskExecutor;
		return this;
	}

	/**
	 * Configure the given interceptors for this message channel,
	 * adding them to the channel's current list of interceptors.
	 * @since 4.3.12
	 */
	public ChannelRegistration interceptors(ChannelInterceptor... interceptors) {
		this.interceptors.addAll(Arrays.asList(interceptors));
		return this;
	}


	protected boolean hasTaskExecutor() {
		return (this.registration != null || this.executor != null);
	}

	protected boolean hasInterceptors() {
		return !this.interceptors.isEmpty();
	}

	/**
	 * Return the {@link TaskExecutor} to use. If no task executor has been
	 * configured, the {@code fallback} supplier is used to provide a fallback
	 * instance.
	 * <p>
	 * If the {@link TaskExecutor} to use is suitable for further customizations,
	 * the {@code customizer} consumer is invoked.
	 * @param fallback a supplier of a fallback task executor in case none is configured
	 * @param customizer further customizations
	 * @return the task executor to use
	 */
	protected TaskExecutor getTaskExecutor(Supplier<TaskExecutor> fallback, Consumer<TaskExecutor> customizer) {
		if (this.executor != null) {
			return this.executor;
		}
		else if (this.registration != null) {
			ThreadPoolTaskExecutor registeredTaskExecutor = this.registration.getTaskExecutor();
			customizer.accept(registeredTaskExecutor);
			return registeredTaskExecutor;
		}
		else {
			TaskExecutor taskExecutor = fallback.get();
			customizer.accept(taskExecutor);
			return taskExecutor;
		}
	}

	protected List<ChannelInterceptor> getInterceptors() {
		return this.interceptors;
	}

}
