/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.scheduling.config;

import java.util.concurrent.RejectedExecutionHandler;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;

/**
 * {@link FactoryBean} for creating {@link ThreadPoolTaskExecutor} instances,
 * primarily used behind the XML task namespace.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 3.0
 */
public class TaskExecutorFactoryBean implements
		FactoryBean<TaskExecutor>, BeanNameAware, InitializingBean, DisposableBean {

	private @Nullable String poolSize;

	private @Nullable Integer queueCapacity;

	private @Nullable RejectedExecutionHandler rejectedExecutionHandler;

	private @Nullable Integer keepAliveSeconds;

	private @Nullable String beanName;

	private @Nullable ThreadPoolTaskExecutor target;


	public void setPoolSize(String poolSize) {
		this.poolSize = poolSize;
	}

	public void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

	public void setRejectedExecutionHandler(RejectedExecutionHandler rejectedExecutionHandler) {
		this.rejectedExecutionHandler = rejectedExecutionHandler;
	}

	public void setKeepAliveSeconds(int keepAliveSeconds) {
		this.keepAliveSeconds = keepAliveSeconds;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}


	@Override
	public void afterPropertiesSet() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		determinePoolSizeRange(executor);
		if (this.queueCapacity != null) {
			executor.setQueueCapacity(this.queueCapacity);
		}
		if (this.keepAliveSeconds != null) {
			executor.setKeepAliveSeconds(this.keepAliveSeconds);
		}
		if (this.rejectedExecutionHandler != null) {
			executor.setRejectedExecutionHandler(this.rejectedExecutionHandler);
		}
		if (this.beanName != null) {
			executor.setThreadNamePrefix(this.beanName + "-");
		}
		executor.afterPropertiesSet();
		this.target = executor;
	}

	private void determinePoolSizeRange(ThreadPoolTaskExecutor executor) {
		if (StringUtils.hasText(this.poolSize)) {
			try {
				int corePoolSize;
				int maxPoolSize;
				int separatorIndex = this.poolSize.indexOf('-');
				if (separatorIndex != -1) {
					corePoolSize = Integer.parseInt(this.poolSize, 0, separatorIndex, 10);
					maxPoolSize = Integer.parseInt(this.poolSize, separatorIndex + 1, this.poolSize.length(), 10);
					if (corePoolSize > maxPoolSize) {
						throw new IllegalArgumentException(
								"Lower bound of pool-size range must not exceed the upper bound");
					}
					if (this.queueCapacity == null) {
						// No queue-capacity provided, so unbounded
						if (corePoolSize == 0) {
							// Actually set 'corePoolSize' to the upper bound of the range
							// but allow core threads to timeout...
							executor.setAllowCoreThreadTimeOut(true);
							corePoolSize = maxPoolSize;
						}
						else {
							// Non-zero lower bound implies a core-max size range...
							throw new IllegalArgumentException(
									"A non-zero lower bound for the size range requires a queue-capacity value");
						}
					}
				}
				else {
					int value = Integer.parseInt(this.poolSize);
					corePoolSize = value;
					maxPoolSize = value;
				}
				executor.setCorePoolSize(corePoolSize);
				executor.setMaxPoolSize(maxPoolSize);
			}
			catch (NumberFormatException ex) {
				throw new IllegalArgumentException("Invalid pool-size value [" + this.poolSize + "]: only single " +
						"maximum integer (for example, \"5\") and minimum-maximum range (for example, \"3-5\") are supported", ex);
			}
		}
	}


	@Override
	public @Nullable TaskExecutor getObject() {
		return this.target;
	}

	@Override
	public Class<? extends TaskExecutor> getObjectType() {
		return (this.target != null ? this.target.getClass() : ThreadPoolTaskExecutor.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	@Override
	public void destroy() {
		if (this.target != null) {
			this.target.destroy();
		}
	}

}
