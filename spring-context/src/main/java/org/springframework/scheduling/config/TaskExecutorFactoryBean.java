/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.JdkVersion;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;

/**
 * FactoryBean for creating ThreadPoolTaskExecutor instances, choosing
 * between the standard concurrent and the backport-concurrent variant.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 3.0
 */
public class TaskExecutorFactoryBean implements
		FactoryBean<TaskExecutor>, BeanNameAware, InitializingBean, DisposableBean {

	private String poolSize;

	private Integer queueCapacity;

	private Object rejectedExecutionHandler;

	private Integer keepAliveSeconds;

	private String beanName;

	private TaskExecutor target;


	public void setPoolSize(String poolSize) {
		this.poolSize = poolSize;
	}

	public void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

	public void setRejectedExecutionHandler(Object rejectedExecutionHandler) {
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
	public void afterPropertiesSet() throws Exception {
		Class<?> executorClass = (shouldUseBackport() ?
				getClass().getClassLoader().loadClass("org.springframework.scheduling.backportconcurrent.ThreadPoolTaskExecutor") :
				ThreadPoolTaskExecutor.class);
		BeanWrapper bw = new BeanWrapperImpl(executorClass);
		determinePoolSizeRange(bw);
		if (this.queueCapacity != null) {
			bw.setPropertyValue("queueCapacity", this.queueCapacity);
		}
		if (this.keepAliveSeconds != null) {
			bw.setPropertyValue("keepAliveSeconds", this.keepAliveSeconds);
		}
		if (this.rejectedExecutionHandler != null) {
			bw.setPropertyValue("rejectedExecutionHandler", this.rejectedExecutionHandler);
		}
		if (this.beanName != null) {
			bw.setPropertyValue("threadNamePrefix", this.beanName + "-");
		}
		this.target = (TaskExecutor) bw.getWrappedInstance();
		if (this.target instanceof InitializingBean) {
			((InitializingBean) this.target).afterPropertiesSet();
		}
	}

	private boolean shouldUseBackport() {
		return (StringUtils.hasText(this.poolSize) && this.poolSize.startsWith("0") &&
				JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_16);
	}

	private void determinePoolSizeRange(BeanWrapper bw) {
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
						// no queue-capacity provided, so unbounded
						if (corePoolSize == 0) {
							// actually set 'corePoolSize' to the upper bound of the range
							// but allow core threads to timeout
							bw.setPropertyValue("allowCoreThreadTimeOut", true);
							corePoolSize = maxPoolSize;
						}
						else {
							// non-zero lower bound implies a core-max size range
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
				bw.setPropertyValue("corePoolSize", corePoolSize);
				bw.setPropertyValue("maxPoolSize", maxPoolSize);
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
		if (this.target != null) {
			return this.target.getClass();
		}
		return (!shouldUseBackport() ? ThreadPoolTaskExecutor.class : TaskExecutor.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	@Override
	public void destroy() throws Exception {
		if (this.target instanceof DisposableBean) {
			((DisposableBean) this.target).destroy();
		}
	}

}
