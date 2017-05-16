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

package org.springframework.scheduling.config;

import java.util.concurrent.RejectedExecutionHandler;

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

	private String poolSize;

	private Integer queueCapacity;

	private RejectedExecutionHandler rejectedExecutionHandler;

	private Integer keepAliveSeconds;

	private String beanName;

	private ThreadPoolTaskExecutor target;


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
		this.target = new ThreadPoolTaskExecutor();
		determinePoolSizeRange();
		if (this.queueCapacity != null) {
			this.target.setQueueCapacity(this.queueCapacity);
		}
		if (this.keepAliveSeconds != null) {
			this.target.setKeepAliveSeconds(this.keepAliveSeconds);
		}
		if (this.rejectedExecutionHandler != null) {
			this.target.setRejectedExecutionHandler(this.rejectedExecutionHandler);
		}
		if (this.beanName != null) {
			this.target.setThreadNamePrefix(this.beanName + "-");
		}
		this.target.afterPropertiesSet();
	}

	private void determinePoolSizeRange() {
		if (StringUtils.hasText(this.poolSize)) {
			try {
				int corePoolSize;
				int maxPoolSize;
				int separatorIndex = this.poolSize.indexOf('-');
				if (separatorIndex != -1) {
					corePoolSize = Integer.valueOf(this.poolSize.substring(0, separatorIndex));
					maxPoolSize = Integer.valueOf(this.poolSize.substring(separatorIndex + 1, this.poolSize.length()));
					if (corePoolSize > maxPoolSize) {
						throw new IllegalArgumentException(
								"Lower bound of pool-size range must not exceed the upper bound");
					}
					if (this.queueCapacity == null) {
						// No queue-capacity provided, so unbounded
						if (corePoolSize == 0) {
							// Actually set 'corePoolSize' to the upper bound of the range
							// but allow core threads to timeout...
							this.target.setAllowCoreThreadTimeOut(true);
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
					Integer value = Integer.valueOf(this.poolSize);
					corePoolSize = value;
					maxPoolSize = value;
				}
				this.target.setCorePoolSize(corePoolSize);
				this.target.setMaxPoolSize(maxPoolSize);
			}
			catch (NumberFormatException ex) {
				throw new IllegalArgumentException("Invalid pool-size value [" + this.poolSize + "]: only single " +
						"maximum integer (e.g. \"5\") and minimum-maximum range (e.g. \"3-5\") are supported", ex);
			}
		}
	}


	@Override
	public TaskExecutor getObject() {
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
		this.target.destroy();
	}

}
