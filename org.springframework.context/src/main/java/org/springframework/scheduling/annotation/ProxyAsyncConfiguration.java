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

package org.springframework.scheduling.annotation;

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

/**
 * {@code @Configuration} class that registers the Spring infrastructure beans necessary
 * to enable proxy-based asynchronous method execution.
 *
 * @author Chris Beams
 * @since 3.1
 * @see EnableAsync
 * @see AsyncConfigurationSelector
 */
@Configuration
public class ProxyAsyncConfiguration extends AbstractAsyncConfiguration {

	@Override
	@Bean(name=AnnotationConfigUtils.ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public AsyncAnnotationBeanPostProcessor asyncAdvisor() {
		Assert.notNull(this.enableAsync, "@EnableAsync annotation metadata was not injected");

		AsyncAnnotationBeanPostProcessor bpp = new AsyncAnnotationBeanPostProcessor();

		Class<? extends Annotation> customAsyncAnnotation = enableAsync.getClass("annotation", Annotation.class);
		if (customAsyncAnnotation != AnnotationUtils.getDefaultValue(EnableAsync.class, "annotation")) {
			bpp.setAsyncAnnotationType(customAsyncAnnotation);
		}

		if (this.executor != null) {
			bpp.setExecutor(this.executor);
		}

		bpp.setProxyTargetClass(this.enableAsync.getBoolean("proxyTargetClass"));

		bpp.setOrder(this.enableAsync.getInt("order"));

		return bpp;
	}

}
