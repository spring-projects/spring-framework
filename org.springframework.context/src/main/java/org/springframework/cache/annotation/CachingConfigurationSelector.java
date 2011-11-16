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

package org.springframework.cache.annotation;

import java.util.Map;

import org.springframework.aop.config.AopConfigUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportSelectorContext;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

/**
 * Selects which implementation of {@link AbstractCachingConfiguration} should be used
 * based on the value of {@link EnableCaching#mode} on the importing @{@link Configuration}
 * class.
 *
 * @author Chris Beams
 * @since 3.1
 * @see EnableCaching
 * @see AbstractCachingConfiguration
 * @see ProxyCachingConfiguration
 * @see org.springframework.cache.aspectj.AspectJCachingConfiguration
 */
public class CachingConfigurationSelector implements ImportSelector {

	/**
	 * {@inheritDoc}
	 * <p>This implementation selects {@link ProxyCachingConfiguration} if
	 * {@link EnableCaching#mode()} equals {@code PROXY}, and otherwise selects
	 * {@link org.springframework.cache.aspectj.AspectJCachingConfiguration AspectJCacheConfiguration}.
	 * <p>If {@code #mode()} equals {@code PROXY}, an auto-proxy creator bean definition
	 * will also be added to the enclosing {@link BeanDefinitionRegistry} and escalated
	 * if necessary through the usual {@link AopConfigUtils} family of methods.
	 */
	public String[] selectImports(ImportSelectorContext context) {
		AnnotationMetadata importingClassMetadata = context.getImportingClassMetadata();
		BeanDefinitionRegistry registry = context.getBeanDefinitionRegistry();

		Map<String, Object> enableCaching =
			importingClassMetadata.getAnnotationAttributes(EnableCaching.class.getName());
		Assert.notNull(enableCaching,
				"@EnableCaching is not present on importing class " +
				importingClassMetadata.getClassName());

		switch ((AdviceMode) enableCaching.get("mode")) {
			case PROXY:
				AopConfigUtils.registerAutoProxyCreatorIfNecessary(registry);
				if ((Boolean)enableCaching.get("proxyTargetClass")) {
					AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
				}
				return new String[] { ProxyCachingConfiguration.class.getName() };
			case ASPECTJ:
				return new String[] {"org.springframework.cache.aspectj.AspectJCachingConfiguration"};
			default:
				throw new IllegalArgumentException("Unknown AdviceMode " + enableCaching.get("mode"));
		}
	}

}
