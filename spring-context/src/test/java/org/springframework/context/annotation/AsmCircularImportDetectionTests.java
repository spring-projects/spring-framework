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

package org.springframework.context.annotation;

import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;

/**
 * Unit test proving that ASM-based {@link ConfigurationClassParser} correctly detects
 * circular use of the {@link Import @Import} annotation.
 *
 * @author Chris Beams
 */
class AsmCircularImportDetectionTests extends AbstractCircularImportDetectionTests {

	@Override
	protected ConfigurationClassParser newParser() {
		return new ConfigurationClassParser(
				new CachingMetadataReaderFactory(),
				new FailFastProblemReporter(),
				new StandardEnvironment(),
				new DefaultResourceLoader(),
				new AnnotationBeanNameGenerator(),
				new DefaultListableBeanFactory());
	}

	@Override
	protected String loadAsConfigurationSource(Class<?> clazz) {
		return clazz.getName();
	}

}
