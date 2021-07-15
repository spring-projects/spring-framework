/*
 * Copyright 2002-2021 the original author or authors.
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

import java.io.BufferedInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.asm.ClassReader;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AbstractMethodMetadataTests;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link MethodMetadataReadingVisitor}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 */
@SuppressWarnings("deprecation")
class MethodMetadataReadingVisitorTests extends AbstractMethodMetadataTests {

	@Override
	protected AnnotationMetadata get(Class<?> source) {
		try {
			ClassLoader classLoader = source.getClassLoader();
			String className = source.getName();
			String resourcePath = ResourceLoader.CLASSPATH_URL_PREFIX
					+ ClassUtils.convertClassNameToResourcePath(className)
					+ ClassUtils.CLASS_FILE_SUFFIX;
			Resource resource = new DefaultResourceLoader().getResource(resourcePath);
			try (InputStream inputStream = new BufferedInputStream(
					resource.getInputStream())) {
				ClassReader classReader = new ClassReader(inputStream);
				AnnotationMetadataReadingVisitor metadata = new AnnotationMetadataReadingVisitor(
						classLoader);
				classReader.accept(metadata, ClassReader.SKIP_DEBUG);
				return metadata;
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Test
	@Disabled("equals() not implemented in deprecated MethodMetadataReadingVisitor")
	@Override
	public void verifyEquals() throws Exception {
	}

	@Test
	@Disabled("hashCode() not implemented in deprecated MethodMetadataReadingVisitor")
	@Override
	public void verifyHashCode() throws Exception {
	}

	@Test
	@Disabled("toString() not implemented in deprecated MethodMetadataReadingVisitor")
	@Override
	public void verifyToString() {
	}

	@Test
	@Override
	public void getAnnotationsReturnsDirectAnnotations() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(
				super::getAnnotationsReturnsDirectAnnotations);
	}

}
