/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.scheduling.aspectj;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.scheduling.annotation.AbstractAsyncConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.config.TaskManagementConfigUtils;

/**
 * {@code @Configuration} class that registers the Spring infrastructure beans necessary
 * to enable AspectJ-based asynchronous method execution.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 3.1
 * @see EnableAsync
 * @see org.springframework.scheduling.annotation.AsyncConfigurationSelector
 * @see org.springframework.scheduling.annotation.ProxyAsyncConfiguration
 */
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class AspectJAsyncConfiguration extends AbstractAsyncConfiguration {

	@Bean(name = TaskManagementConfigUtils.ASYNC_EXECUTION_ASPECT_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public AnnotationAsyncExecutionAspect asyncAdvisor() {
		AnnotationAsyncExecutionAspect asyncAspect = AnnotationAsyncExecutionAspect.aspectOf();
		asyncAspect.configure(this.executor, this.exceptionHandler);
		return asyncAspect;
	}

}
