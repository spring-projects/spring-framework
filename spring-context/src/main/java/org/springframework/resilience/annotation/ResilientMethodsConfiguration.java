/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
			if (this.enableResilientMethods.getBoolean("proxyTargetClass")) {
				proxySupport.setProxyTargetClass(true);
			}
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
