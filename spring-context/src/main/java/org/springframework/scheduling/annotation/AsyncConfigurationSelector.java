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

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AdviceModeImportSelector;
import org.springframework.context.annotation.AnnotationConfigUtils;

/**
 * Selects which implementation of {@link AbstractAsyncConfiguration} should be used based
 * on the value of {@link EnableAsync#mode} on the importing {@code @Configuration} class.
 *
 * @author Chris Beams
 * @since 3.1
 * @see EnableAsync
 * @see ProxyAsyncConfiguration
 * @see AnnotationConfigUtils#ASYNC_EXECUTION_ASPECT_CONFIGURATION_CLASS_NAME
 */
public class AsyncConfigurationSelector extends AdviceModeImportSelector<EnableAsync> {

	/**
	 * {@inheritDoc}
	 * @return {@link ProxyAsyncConfiguration} or {@code AspectJAsyncConfiguration} for
	 * {@code PROXY} and {@code ASPECTJ} values of {@link EnableAsync#mode()}, respectively
	 */
	public String[] selectImports(AdviceMode adviceMode) {
		switch (adviceMode) {
			case PROXY:
				return new String[] { ProxyAsyncConfiguration.class.getName() };
			case ASPECTJ:
				return new String[] { AnnotationConfigUtils.ASYNC_EXECUTION_ASPECT_CONFIGURATION_CLASS_NAME };
			default:
				return null;
		}
	}

}
