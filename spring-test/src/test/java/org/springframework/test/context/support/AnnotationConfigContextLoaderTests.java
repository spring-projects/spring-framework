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

package org.springframework.test.context.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Unit tests for {@link AnnotationConfigContextLoader}.
 *
 * @author Sam Brannen
 * @since 3.1
 */
public class AnnotationConfigContextLoaderTests {

	private final AnnotationConfigContextLoader contextLoader = new AnnotationConfigContextLoader();


	@Test
	public void detectDefaultConfigurationClassesForAnnotatedInnerClass() {
		Class<?>[] configClasses = contextLoader.detectDefaultConfigurationClasses(ContextConfigurationInnerClassTestCase.class);
		assertNotNull(configClasses);
		assertEquals("annotated static ContextConfiguration should be considered.", 1, configClasses.length);

		configClasses = contextLoader.detectDefaultConfigurationClasses(AnnotatedFooConfigInnerClassTestCase.class);
		assertNotNull(configClasses);
		assertEquals("annotated static FooConfig should be considered.", 1, configClasses.length);
	}

	@Test
	public void detectDefaultConfigurationClassesForMultipleAnnotatedInnerClasses() {
		Class<?>[] configClasses = contextLoader.detectDefaultConfigurationClasses(MultipleStaticConfigurationClassesTestCase.class);
		assertNotNull(configClasses);
		assertEquals("multiple annotated static classes should be considered.", 2, configClasses.length);
	}

	@Test
	public void detectDefaultConfigurationClassesForNonAnnotatedInnerClass() {
		Class<?>[] configClasses = contextLoader.detectDefaultConfigurationClasses(PlainVanillaFooConfigInnerClassTestCase.class);
		assertNotNull(configClasses);
		assertEquals("non-annotated static FooConfig should NOT be considered.", 0, configClasses.length);
	}

	@Test
	public void detectDefaultConfigurationClassesForFinalAnnotatedInnerClass() {
		Class<?>[] configClasses = contextLoader.detectDefaultConfigurationClasses(FinalConfigInnerClassTestCase.class);
		assertNotNull(configClasses);
		assertEquals("final annotated static Config should NOT be considered.", 0, configClasses.length);
	}

	@Test
	public void detectDefaultConfigurationClassesForPrivateAnnotatedInnerClass() {
		Class<?>[] configClasses = contextLoader.detectDefaultConfigurationClasses(PrivateConfigInnerClassTestCase.class);
		assertNotNull(configClasses);
		assertEquals("private annotated inner classes should NOT be considered.", 0, configClasses.length);
	}

	@Test
	public void detectDefaultConfigurationClassesForNonStaticAnnotatedInnerClass() {
		Class<?>[] configClasses = contextLoader.detectDefaultConfigurationClasses(NonStaticConfigInnerClassesTestCase.class);
		assertNotNull(configClasses);
		assertEquals("non-static annotated inner classes should NOT be considered.", 0, configClasses.length);
	}

}
