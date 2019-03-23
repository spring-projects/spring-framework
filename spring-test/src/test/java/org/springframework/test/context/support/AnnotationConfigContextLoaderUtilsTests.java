/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.test.context.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Test;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.*;
import static org.springframework.test.context.support.AnnotationConfigContextLoaderUtils.*;

/**
 * Unit tests for {@link AnnotationConfigContextLoaderUtils}.
 *
 * @author Sam Brannen
 * @since 4.1.5
 */
public class AnnotationConfigContextLoaderUtilsTests {

	@Test(expected = IllegalArgumentException.class)
	public void detectDefaultConfigurationClassesWithNullDeclaringClass() {
		detectDefaultConfigurationClasses(null);
	}

	@Test
	public void detectDefaultConfigurationClassesWithoutConfigurationClass() {
		Class<?>[] configClasses = detectDefaultConfigurationClasses(NoConfigTestCase.class);
		assertNotNull(configClasses);
		assertEquals(0, configClasses.length);
	}

	@Test
	public void detectDefaultConfigurationClassesWithExplicitConfigurationAnnotation() {
		Class<?>[] configClasses = detectDefaultConfigurationClasses(ExplicitConfigTestCase.class);
		assertNotNull(configClasses);
		assertArrayEquals(new Class<?>[] { ExplicitConfigTestCase.Config.class }, configClasses);
	}

	@Test
	public void detectDefaultConfigurationClassesWithConfigurationMetaAnnotation() {
		Class<?>[] configClasses = detectDefaultConfigurationClasses(MetaAnnotatedConfigTestCase.class);
		assertNotNull(configClasses);
		assertArrayEquals(new Class<?>[] { MetaAnnotatedConfigTestCase.Config.class }, configClasses);
	}


	private static class NoConfigTestCase {

	}

	private static class ExplicitConfigTestCase {

		@Configuration
		static class Config {
		}
	}

	@Configuration
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	private static @interface MetaConfig {
	}

	private static class MetaAnnotatedConfigTestCase {

		@MetaConfig
		static class Config {
		}
	}

}
