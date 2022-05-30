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

package org.springframework.aot.generate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link MethodGeneratorWithName}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class MethodGeneratorWithNameTests {

	private final GeneratedMethods generatedMethods = new GeneratedMethods();

	@Test
	void withNameWhenGeneratingGetMethod() {
		GeneratedMethod generateMethod = generatedMethods.withName("my", "bean")
				.generateMethod("get", "test");
		assertThat(generateMethod.getName()).hasToString("getMyBeanTest");
	}

	@Test
	void withNameWhenGeneratingSetMethod() {
		GeneratedMethod generateMethod = generatedMethods.withName("my", "bean")
				.generateMethod("set", "test");
		assertThat(generateMethod.getName()).hasToString("setMyBeanTest");
	}

	@Test
	void withNameWhenGeneratingIsMethod() {
		GeneratedMethod generateMethod = generatedMethods.withName("my", "bean")
				.generateMethod("is", "test");
		assertThat(generateMethod.getName()).hasToString("isMyBeanTest");
	}

	@Test
	void withNameWhenGeneratingOtherMethod() {
		GeneratedMethod generateMethod = generatedMethods.withName("my", "bean")
				.generateMethod("test");
		assertThat(generateMethod.getName()).hasToString("myBeanTest");
	}

}
