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

package org.springframework.core.type.classreading;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Test;

import org.springframework.core.OverridingClassLoader;
import org.springframework.core.type.AbstractAnnotationMetadataTests;
import org.springframework.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link SimpleAnnotationMetadata} and
 * {@link SimpleAnnotationMetadataReadingVisitor}.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 */
class SimpleAnnotationMetadataTests extends AbstractAnnotationMetadataTests {

	@Override
	protected AnnotationMetadata get(Class<?> source) {
		try {
			return new SimpleMetadataReaderFactory(source.getClassLoader())
					.getMetadataReader(source.getName()).getAnnotationMetadata();
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Test
	void getClassAttributeWhenUnknownClass() throws IOException {
		var classLoader = new FilteringClassLoader(getClass().getClassLoader());
		var mergedAnnotation = new SimpleMetadataReaderFactory(classLoader)
				.getMetadataReader(WithClassMissingFromClasspath.class.getName())
				.getAnnotationMetadata()
				.getAnnotations()
				.get(ClassAttributes.class);
		assertThat(mergedAnnotation.getStringArray("types")).contains("javax.annotation.meta.When");
		assertThatExceptionOfType(TypeNotPresentException.class)
				.isThrownBy(() -> mergedAnnotation.getClassArray("types"))
				.withMessageContaining("javax.annotation.meta.When");
	}


	private static class FilteringClassLoader extends OverridingClassLoader {

		FilteringClassLoader(ClassLoader parent) {
			super(parent);
		}

		@Override
		protected boolean isEligibleForOverriding(String className) {
			return className.startsWith("javax.annotation.");
		}

		@Override
		protected Class<?> loadClassForOverriding(String name) throws ClassNotFoundException {
			throw new ClassNotFoundException(name);
		}
	}


	@ClassAttributes(types = {javax.annotation.meta.When.class})
	@javax.annotation.Nonnull(when = javax.annotation.meta.When.MAYBE)
	static class WithClassMissingFromClasspath {
	}


	@Retention(RetentionPolicy.RUNTIME)
	@interface ClassAttributes {

		Class<?>[] types();
	}

}
