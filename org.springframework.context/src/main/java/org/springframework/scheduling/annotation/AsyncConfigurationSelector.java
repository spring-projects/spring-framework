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

package org.springframework.scheduling.annotation;

import java.util.Map;

import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.context.config.AdviceMode;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

/**
 * Select which implementation of {@link AbstractAsyncConfiguration}
 * should be used based on the value of {@link EnableAsync#mode} on the
 * importing @{@link Configuration} class.
 *
 * @author Chris Beams
 * @since 3.1
 * @see EnableAsync
 * @see AbstractAsyncConfiguration
 * @see ProxyAsyncConfiguration
 * @see AnnotationConfigUtils#ASYNC_EXECUTION_ASPECT_CONFIGURATION_CLASS_NAME
 */
public class AsyncConfigurationSelector implements ImportSelector {

	public String[] selectImports(AnnotationMetadata importingClassMetadata) {
		Map<String, Object> enableAsync =
			importingClassMetadata.getAnnotationAttributes(EnableAsync.class.getName());
		Assert.notNull(enableAsync,
				"@EnableAsync is not present on importing class " +
				importingClassMetadata.getClassName());

		switch ((AdviceMode) enableAsync.get("mode")) {
			case PROXY:
				return new String[] {ProxyAsyncConfiguration.class.getName()};
			case ASPECTJ:
				return new String[] {AnnotationConfigUtils.ASYNC_EXECUTION_ASPECT_CONFIGURATION_CLASS_NAME};
			default:
				throw new IllegalArgumentException("Unknown AdviceMode " + enableAsync.get("mode"));
		}
	}

}
