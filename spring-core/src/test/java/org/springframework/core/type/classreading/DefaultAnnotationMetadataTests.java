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

import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.Test;

import org.springframework.core.type.AbstractAnnotationMetadataTests;
import org.springframework.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link SimpleAnnotationMetadata} and
 * {@link SimpleAnnotationMetadataReadingVisitor} on Java < 24,
 * and for the ClassFile API variant on Java >= 24.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 */
class DefaultAnnotationMetadataTests extends AbstractAnnotationMetadataTests {

	@Override
	protected AnnotationMetadata get(Class<?> source) {
		try {
			return MetadataReaderFactory.create(source.getClassLoader())
					.getMetadataReader(source.getName()).getAnnotationMetadata();
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Test
	void getClassAttributeWhenUnknownClass() {
		var annotation = get(WithClassMissingFromClasspath.class).getAnnotations().get(ClassAttributes.class);
		assertThat(annotation.getStringArray("types")).contains("com.github.benmanes.caffeine.cache.Caffeine");
		assertThatIllegalArgumentException().isThrownBy(() -> annotation.getClassArray("types"));
	}

	@ClassAttributes(types = {Caffeine.class})
	public static class WithClassMissingFromClasspath {
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface ClassAttributes {
		Class<?>[] types();
	}

}
