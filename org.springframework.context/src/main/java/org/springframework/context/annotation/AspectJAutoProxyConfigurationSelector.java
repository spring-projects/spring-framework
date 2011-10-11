/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.context.annotation;

import java.util.Map;

import org.springframework.aop.config.AopConfigUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Registers an {@link org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator
 * AnnotationAwareAspectJAutoProxyCreator} against the current {@link BeanDefinitionRegistry}
 * as appropriate based on a given @{@link EnableAspectJAutoProxy} annotation.
 *
 * @author Chris Beams
 * @see EnableAspectJAutoProxy
 * @since 3.1
 */
public class AspectJAutoProxyConfigurationSelector implements ImportSelector {

	/**
	 * Register, escalate, and configure the AspectJ auto proxy creator. Always return
	 * an empty array, as no actual {@code @Configuration} classes are required.
	 */
	public String[] selectImports(ImportSelectorContext context) {
		BeanDefinitionRegistry registry = context.getBeanDefinitionRegistry();
		AnnotationMetadata importingClassMetadata = context.getImportingClassMetadata();

		Map<String, Object> enableAJAutoProxy =
			importingClassMetadata.getAnnotationAttributes(EnableAspectJAutoProxy.class.getName());

		AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry);

		if ((Boolean)enableAJAutoProxy.get("proxyTargetClass")) {
			AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
		}

		return new String[] { };
	}

}
