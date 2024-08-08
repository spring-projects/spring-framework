/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.core.io.support;

import java.beans.PropertyEditor;

import org.junit.jupiter.api.Test;

import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;
import org.springframework.util.PlaceholderResolutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ResourceArrayPropertyEditor}.
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Yanming Zhou
 * @author Stephane Nicoll
 */
class ResourceArrayPropertyEditorTests {

	@Test
	void vanillaResource() {
		PropertyEditor editor = new ResourceArrayPropertyEditor();
		editor.setAsText("classpath:org/springframework/core/io/support/ResourceArrayPropertyEditor.class");
		Resource[] resources = (Resource[]) editor.getValue();
		assertThat(resources).isNotNull();
		assertThat(resources[0].exists()).isTrue();
	}

	@Test
	void patternResource() {
		// N.B. this will sometimes fail if you use classpath: instead of classpath*:.
		// The result depends on the classpath - if test-classes are segregated from classes
		// and they come first on the classpath (like in Maven) then it breaks, if classes
		// comes first (like in Spring Build) then it is OK.
		PropertyEditor editor = new ResourceArrayPropertyEditor();
		editor.setAsText("classpath*:org/springframework/core/io/support/Resource*Editor.class");
		Resource[] resources = (Resource[]) editor.getValue();
		assertThat(resources).isNotNull();
		assertThat(resources[0].exists()).isTrue();
	}

	@Test
	void systemPropertyReplacement() {
		PropertyEditor editor = new ResourceArrayPropertyEditor();
		System.setProperty("test.prop", "foo");
		try {
			editor.setAsText("${test.prop}");
			Resource[] resources = (Resource[]) editor.getValue();
			assertThat(resources[0].getFilename()).isEqualTo("foo");
		}
		finally {
			System.clearProperty("test.prop");
		}
	}

	@Test
	void strictSystemPropertyReplacementWithUnresolvablePlaceholder() {
		PropertyEditor editor = new ResourceArrayPropertyEditor(
				new PathMatchingResourcePatternResolver(), new StandardEnvironment(),
				false);
		System.setProperty("test.prop", "foo");
		try {
			assertThatExceptionOfType(PlaceholderResolutionException.class).isThrownBy(() ->
					editor.setAsText("${test.prop}-${bar}"));
		}
		finally {
			System.clearProperty("test.prop");
		}
	}

	@Test
	void commaDelimitedResourcesWithSingleResource() {
		PropertyEditor editor = new ResourceArrayPropertyEditor();
		editor.setAsText("classpath:org/springframework/core/io/support/ResourceArrayPropertyEditor.class,file:/test.txt");
		Resource[] resources = (Resource[]) editor.getValue();
		assertThat(resources).isNotNull();
		assertThat(resources[0]).isInstanceOfSatisfying(ClassPathResource.class,
				resource -> assertThat(resource.exists()).isTrue());
		assertThat(resources[1]).isInstanceOfSatisfying(FileUrlResource.class,
				resource -> assertThat(resource.getFilename()).isEqualTo("test.txt"));
	}

	@Test
	void commaDelimitedResourcesWithMultipleResources() {
		PropertyEditor editor = new ResourceArrayPropertyEditor();
		editor.setAsText("file:/test.txt, classpath:org/springframework/core/io/support/test-resources/*.txt");
		Resource[] resources = (Resource[]) editor.getValue();
		assertThat(resources).isNotNull();
		assertThat(resources[0]).isInstanceOfSatisfying(FileUrlResource.class,
				resource -> assertThat(resource.getFilename()).isEqualTo("test.txt"));
		assertThat(resources).anySatisfy(candidate ->
				assertThat(candidate.getFilename()).isEqualTo("resource1.txt"));
		assertThat(resources).anySatisfy(candidate ->
				assertThat(candidate.getFilename()).isEqualTo("resource2.txt"));
		assertThat(resources).hasSize(3);
	}

}
