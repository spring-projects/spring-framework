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

package org.springframework.resilience.annotation;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.framework.ProxyProcessorSupport;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * {@code @Configuration} class that registers the Spring infrastructure beans necessary
 * to enable proxy-based method invocations with retry and concurrency limit behavior.
 *
 * @author Juergen Hoeller
 * @since 7.0
 * @see EnableResilientMethods
 * @see RetryAnnotationBeanPostProcessor
 * @see ConcurrencyLimitBeanPostProcessor
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ResilientMethodsConfiguration implements ImportAware {

	private @Nullable AnnotationAttributes enableResilientMethods;


	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		this.enableResilientMethods = AnnotationAttributes.fromMap(
				importMetadata.getAnnotationAttributes(EnableResilientMethods.class.getName()));
	}

	private void configureProxySupport(ProxyProcessorSupport proxySupport) {
		if (this.enableResilientMethods != null) {
			proxySupport.setProxyTargetClass(this.enableResilientMethods.getBoolean("proxyTargetClass"));
			proxySupport.setOrder(this.enableResilientMethods.getNumber("order"));
		}
	}


	@Bean(name = "org.springframework.resilience.annotation.internalRetryAnnotationProcessor")
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public RetryAnnotationBeanPostProcessor retryAdvisor() {
		RetryAnnotationBeanPostProcessor bpp = new RetryAnnotationBeanPostProcessor();
		configureProxySupport(bpp);
		return bpp;
	}

	@Bean(name = "org.springframework.resilience.annotation.internalConcurrencyLimitProcessor")
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public ConcurrencyLimitBeanPostProcessor concurrencyLimitAdvisor() {
		ConcurrencyLimitBeanPostProcessor bpp = new ConcurrencyLimitBeanPostProcessor();
		configureProxySupport(bpp);
		return bpp;
	}

}
