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

import org.springframework.core.type.AbstractMethodMetadataTests;
import org.springframework.core.type.AnnotationMetadata;


/**
 * Tests for {@link SimpleMethodMetadata} and
 * {@link SimpleMethodMetadataReadingVisitor} on Java < 24,
 * and for the ClassFile API variant on Java >= 24.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 */
class DefaultMethodMetadataTests extends AbstractMethodMetadataTests {


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

}
