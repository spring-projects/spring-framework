/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.web.servlet.hypermedia;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.junit.Test;
import org.springframework.core.AnnotationAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link AnnotationMappingDiscoverer}.
 * 
 * @author Oliver Gierke
 */
public class AnnotationMappingDiscovererUnitTests {

	AnnotationMappingDiscoverer discoverer = new AnnotationMappingDiscoverer(
			RequestMapping.class);

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullAnnotationType() {
		new AnnotationMappingDiscoverer((Class<? extends Annotation>) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullAnnotationAttribute() {
		new AnnotationMappingDiscoverer((AnnotationAttribute) null);
	}

	@Test
	public void discoversTypeLevelMapping() {
		assertThat(discoverer.getMapping(MyController.class), is("/type"));
	}

	@Test
	public void discoversMethodLevelMapping() throws Exception {
		Method method = MyController.class.getMethod("method");
		assertThat(discoverer.getMapping(method), is("/type/method"));
	}

	@Test
	public void returnsNullForNonExistentTypeLevelMapping() {
		assertThat(discoverer.getMapping(ControllerWithoutTypeLevelMapping.class),
				is(nullValue()));
	}

	@Test
	public void resolvesMethodLevelMappingWithoutTypeLevelMapping() throws Exception {

		Method method = ControllerWithoutTypeLevelMapping.class.getMethod("method");
		assertThat(discoverer.getMapping(method), is("/method"));
	}

	@Test
	public void resolvesMethodLevelMappingWithSlashRootMapping() throws Exception {

		Method method = SlashRootMapping.class.getMethod("method");
		assertThat(discoverer.getMapping(method), is("/method"));
	}

	/**
	 * @see #46
	 */
	@Test
	public void treatsMissingMethodMappingAsEmptyMapping() throws Exception {

		Method method = MyController.class.getMethod("noMethodMapping");
		assertThat(discoverer.getMapping(method), is("/type"));
	}

	@RequestMapping("/type")
	interface MyController {

		@RequestMapping("/method")
		void method();

		@RequestMapping
		void noMethodMapping();
	}

	interface ControllerWithoutTypeLevelMapping {

		@RequestMapping("/method")
		void method();
	}

	@RequestMapping("/")
	interface SlashRootMapping {

		@RequestMapping("/method")
		void method();
	}
}
