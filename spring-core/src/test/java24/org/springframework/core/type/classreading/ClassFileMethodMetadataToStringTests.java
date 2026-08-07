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

import org.junit.jupiter.api.Test;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.MethodMetadata;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for parameter type rendering by the {@code java.lang.classfile} based
 * {@link ClassFileMethodMetadata}'s {@code toString()}.
 *
 * @author junhyeong9812
 */
class ClassFileMethodMetadataToStringTests {

	@Test
	void toStringRendersParameterTypeNames() throws Exception {
		ClassFileMetadataReaderFactory factory = new ClassFileMetadataReaderFactory(new DefaultResourceLoader());
		MetadataReader reader = factory.getMetadataReader(WithMethod.class.getName());
		MethodMetadata method = reader.getAnnotationMetadata().getDeclaredMethods().stream()
				.filter(candidate -> candidate.getMethodName().equals("sample"))
				.findFirst()
				.orElseThrow();
		// primitive and array parameter types must keep their canonical names,
		// not be prefixed with "." (".int", ".String[]")
		assertThat(method.toString()).endsWith(".sample(int,java.lang.String[])");
	}


	@SuppressWarnings("unused")
	static class WithMethod {

		public void sample(int number, String[] values) {
		}
	}

}
