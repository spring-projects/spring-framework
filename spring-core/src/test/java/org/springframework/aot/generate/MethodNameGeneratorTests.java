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

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MethodNameGenerator}.
 *
 * @author Phillip Webb
 */
class MethodNameGeneratorTests {

	private final MethodNameGenerator generator = new MethodNameGenerator();

	@Test
	void createWithReservedNamesReservesNames() {
		MethodNameGenerator generator = new MethodNameGenerator("testName");
		assertThat(generator.generateMethodName("test", "name")).hasToString("testName1");
	}

	@Test
	void generateMethodNameGeneratesName() {
		String generated = this.generator.generateMethodName("register", "myBean",
				"bean");
		assertThat(generated).isEqualTo("registerMyBeanBean");
	}

	@Test
	void generateMethodNameWhenHasNonLettersGeneratesName() {
		String generated = this.generator.generateMethodName("register", "myBean123",
				"bean");
		assertThat(generated).isEqualTo("registerMyBeanBean");
	}

	@Test
	void generateMethodNameWhenHasDotsGeneratesCamelCaseName() {
		String generated = this.generator.generateMethodName("register",
				"org.springframework.example.bean");
		assertThat(generated).isEqualTo("registerOrgSpringframeworkExampleBean");
	}

	@Test
	void generateMethodNameWhenMultipleCallsGeneratesSequencedName() {
		String generated1 = this.generator.generateMethodName("register", "myBean123",
				"bean");
		String generated2 = this.generator.generateMethodName("register", "myBean!",
				"bean");
		String generated3 = this.generator.generateMethodName("register", "myBean%%",
				"bean");
		assertThat(generated1).isEqualTo("registerMyBeanBean");
		assertThat(generated2).isEqualTo("registerMyBeanBean1");
		assertThat(generated3).isEqualTo("registerMyBeanBean2");
	}

	@Test
	void generateMethodNameWhenAllEmptyPartsGeneratesSetName() {
		String generated = this.generator.generateMethodName("123");
		assertThat(generated).isEqualTo("$$aot");
	}

	@Test
	void joinReturnsJoinedName() {
		assertThat(MethodNameGenerator.join("get", "bean", "factory"))
				.isEqualTo("getBeanFactory");
		assertThat(MethodNameGenerator.join("get", null, "factory"))
				.isEqualTo("getFactory");
		assertThat(MethodNameGenerator.join(null, null)).isEqualTo("");
		assertThat(MethodNameGenerator.join("", null)).isEqualTo("");
		assertThat(MethodNameGenerator.join("get", InputStream.class))
				.isEqualTo("getInputStream");

	}

}
