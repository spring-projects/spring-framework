/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aot.generator;

import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.core.ResolvableType;
import org.springframework.javapoet.support.CodeSnippet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ResolvableTypeGenerator}.
 *
 * @author Stephane Nicoll
 */
class ResolvableTypeGeneratorTests {

	@Test
	void generateTypeForResolvableTypeWithGenericParameter() {
		assertThat(generateTypeFor(
				ResolvableType.forClassWithGenerics(Function.class,
						ResolvableType.forClassWithGenerics(Supplier.class, String.class),
						ResolvableType.forClassWithGenerics(Supplier.class, Integer.class))))
				.isEqualTo("ResolvableType.forClassWithGenerics(Function.class, "
						+ "ResolvableType.forClassWithGenerics(Supplier.class, String.class), "
						+ "ResolvableType.forClassWithGenerics(Supplier.class, Integer.class))");
	}

	@Test
	void generateTypeForResolvableTypeWithMixedParameter() {
		assertThat(generateTypeFor(
				ResolvableType.forClassWithGenerics(Function.class,
						ResolvableType.forClassWithGenerics(Supplier.class, String.class),
						ResolvableType.forClass(Integer.class))))
				.isEqualTo("ResolvableType.forClassWithGenerics(Function.class, "
						+ "ResolvableType.forClassWithGenerics(Supplier.class, String.class), "
						+ "ResolvableType.forClass(Integer.class))");
	}

	private String generateTypeFor(ResolvableType type) {
		return CodeSnippet.process(new ResolvableTypeGenerator().generateTypeFor(type));
	}

}
