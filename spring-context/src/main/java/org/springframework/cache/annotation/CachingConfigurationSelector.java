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

package org.springframework.cache.annotation;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AdviceModeImportSelector;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.AutoProxyRegistrar;

/**
 * Selects which implementation of {@link AbstractCachingConfiguration} should be used
 * based on the value of {@link EnableCaching#mode} on the importing {@code @Configuration}
 * class.
 *
 * @author Chris Beams
 * @since 3.1
 * @see EnableCaching
 * @see ProxyCachingConfiguration
 * @see AnnotationConfigUtils#CACHE_ASPECT_CONFIGURATION_CLASS_NAME
 */
public class CachingConfigurationSelector extends AdviceModeImportSelector<EnableCaching> {

	/**
	 * {@inheritDoc}
	 * @return {@link ProxyCachingConfiguration} or {@code AspectJCacheConfiguration} for
	 * {@code PROXY} and {@code ASPECTJ} values of {@link EnableCaching#mode()}, respectively
	 */
	@Override
	public String[] selectImports(AdviceMode adviceMode) {
		switch (adviceMode) {
			case PROXY:
				return new String[] { AutoProxyRegistrar.class.getName(), ProxyCachingConfiguration.class.getName() };
			case ASPECTJ:
				return new String[] { AnnotationConfigUtils.CACHE_ASPECT_CONFIGURATION_CLASS_NAME };
			default:
				return null;
		}
	}

}
