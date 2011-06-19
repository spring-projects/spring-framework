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
	public void generateDefaultConfigurationClassesForAnnotatedInnerClass() {
		Class<?>[] configClasses = contextLoader.generateDefaultConfigurationClasses(ContextConfigurationInnerClassTestCase.class);
		assertNotNull(configClasses);
		assertEquals("annotated static ContextConfiguration should be considered.", 1, configClasses.length);

		configClasses = contextLoader.generateDefaultConfigurationClasses(AnnotatedFooConfigInnerClassTestCase.class);
		assertNotNull(configClasses);
		assertEquals("annotated static FooConfig should be considered.", 1, configClasses.length);
	}

	@Test
	public void generateDefaultConfigurationClassesForMultipleAnnotatedInnerClasses() {
		Class<?>[] configClasses = contextLoader.generateDefaultConfigurationClasses(MultipleStaticConfigurationClassesTestCase.class);
		assertNotNull(configClasses);
		assertEquals("multiple annotated static classes should be considered.", 2, configClasses.length);
	}

	@Test
	public void generateDefaultConfigurationClassesForNonAnnotatedInnerClass() {
		Class<?>[] configClasses = contextLoader.generateDefaultConfigurationClasses(PlainVanillaFooConfigInnerClassTestCase.class);
		assertNotNull(configClasses);
		assertEquals("non-annotated static FooConfig should NOT be considered.", 0, configClasses.length);
	}

	@Test
	public void generateDefaultConfigurationClassesForFinalAnnotatedInnerClass() {
		Class<?>[] configClasses = contextLoader.generateDefaultConfigurationClasses(FinalConfigInnerClassTestCase.class);
		assertNotNull(configClasses);
		assertEquals("final annotated static Config should NOT be considered.", 0, configClasses.length);
	}

	@Test
	public void generateDefaultConfigurationClassesForPrivateAnnotatedInnerClass() {
		Class<?>[] configClasses = contextLoader.generateDefaultConfigurationClasses(PrivateConfigInnerClassTestCase.class);
		assertNotNull(configClasses);
		assertEquals("private annotated inner classes should NOT be considered.", 0, configClasses.length);
	}

	@Test
	public void generateDefaultConfigurationClassesForNonStaticAnnotatedInnerClass() {
		Class<?>[] configClasses = contextLoader.generateDefaultConfigurationClasses(NonStaticConfigInnerClassesTestCase.class);
		assertNotNull(configClasses);
		assertEquals("non-static annotated inner classes should NOT be considered.", 0, configClasses.length);
	}

}
