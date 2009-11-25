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

package org.springframework.scheduling.config;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.JdkVersion;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * FactoryBean for creating TaskExecutor instances.
 *
 * @author Mark Fisher
 * @since 3.0
 */
public class TaskExecutorFactoryBean implements FactoryBean<TaskExecutor>, BeanNameAware {

	private volatile TaskExecutor target;

	private volatile BeanWrapper beanWrapper;

	private volatile String poolSize;

	private volatile Integer queueCapacity;

	private volatile Object rejectedExecutionHandler;

	private volatile Integer keepAliveSeconds;

	private volatile String beanName;

	private final Object initializationMonitor = new Object();


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

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public Class<? extends TaskExecutor> getObjectType() {
		if (this.target != null) {
			return this.target.getClass();
		}
		return TaskExecutor.class;
	}

	public TaskExecutor getObject() throws Exception {
		if (this.target == null) {
			this.initializeExecutor();
		}
		return this.target;
	}

	public boolean isSingleton() {
		return true;
	}

	private void initializeExecutor() throws Exception {
		synchronized (this.initializationMonitor) {
			if (this.target != null) {
				return;
			}
			String executorClassName = (shouldUseBackport(this.poolSize))
					? "org.springframework.scheduling.backportconcurrent.ThreadPoolTaskExecutor"
					: "org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor";
			Class<?> executorClass = getClass().getClassLoader().loadClass(executorClassName);
			this.beanWrapper = new BeanWrapperImpl(executorClass);
			this.setValueIfNotNull("queueCapacity", this.queueCapacity);
			this.setValueIfNotNull("keepAliveSeconds", this.keepAliveSeconds);
			this.setValueIfNotNull("rejectedExecutionHandler", this.rejectedExecutionHandler);
			Integer[] range = this.determinePoolSizeRange();
			if (range != null) {
				this.setValueIfNotNull("corePoolSize", range[0]);
				this.setValueIfNotNull("maxPoolSize", range[1]);
			}
			this.target = (TaskExecutor) this.beanWrapper.getWrappedInstance();
			if (this.target instanceof InitializingBean) {
				((InitializingBean)this.target).afterPropertiesSet();
			}
		}
	}

	private void setValueIfNotNull(String name, Object value) {
		Assert.notNull(this.beanWrapper, "Property values cannot be set until the BeanWrapper has been created.");
		if (value != null) {
			this.beanWrapper.setPropertyValue(name, value);
		}
	}

	private Integer[] determinePoolSizeRange() {
		if (!StringUtils.hasText(this.poolSize)) {
			return null;
		}
		Integer[] range = null;
		try {
			int separatorIndex = poolSize.indexOf('-');
			if (separatorIndex != -1) {
				range = new Integer[2];
				range[0] = Integer.valueOf(poolSize.substring(0, separatorIndex));
				range[1] = Integer.valueOf(poolSize.substring(separatorIndex + 1, poolSize.length()));
				if (range[0] > range[1]) {
					throw new BeanCreationException(this.beanName,
							"Lower bound of pool-size range must not exceed the upper bound.");
				}
				if (this.queueCapacity == null) {
					// no queue-capacity provided, so unbounded
					if (range[0] == 0) {
						// actually set 'corePoolSize' to the upper bound of the range
						// but allow core threads to timeout
						this.setValueIfNotNull("allowCoreThreadTimeOut", true);
						range[0] = range[1];
					}
					else {
						// non-zero lower bound implies a core-max size range
						throw new BeanCreationException(this.beanName,
								"A non-zero lower bound for the size range requires a queue-capacity value.");
					}
				}
			}
			else {
				Integer value = Integer.valueOf(poolSize);
				range = new Integer[] {value, value};
			}
		}
		catch (NumberFormatException ex) {
			throw new BeanCreationException(this.beanName,
					"Invalid pool-size value [" + poolSize + "]: only single maximum integer " +
					"(e.g. \"5\") and minimum-maximum range (e.g. \"3-5\") are supported.", ex);
		}
		return range;
	}

	private boolean shouldUseBackport(String poolSize) {
		return (StringUtils.hasText(poolSize) && poolSize.startsWith("0") &&
				JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_16);
	}

}
