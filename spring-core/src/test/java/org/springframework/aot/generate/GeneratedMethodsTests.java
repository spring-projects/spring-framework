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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.MethodSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link GeneratedMethods}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class GeneratedMethodsTests {

	private static final ClassName TEST_CLASS_NAME = ClassName.get("com.example", "Test");

	private static final Consumer<MethodSpec.Builder> methodSpecCustomizer = method -> {};

	private final GeneratedMethods methods = new GeneratedMethods(TEST_CLASS_NAME, MethodName::toString);

	@Test
	void createWhenClassNameIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() ->
						new GeneratedMethods(null, MethodName::toString))
				.withMessage("'className' must not be null");
	}

	@Test
	void createWhenMethodNameGeneratorIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() ->
						new GeneratedMethods(TEST_CLASS_NAME, null))
				.withMessage("'methodNameGenerator' must not be null");
	}

	@Test
	void createWithExistingGeneratorUsesGenerator() {
		Function<MethodName, String> generator = name -> "__" + name.toString();
		GeneratedMethods methods = new GeneratedMethods(TEST_CLASS_NAME, generator);
		assertThat(methods.add("test", methodSpecCustomizer).getName()).hasToString("__test");
	}

	@Test
	void addWithStringNameWhenSuggestedMethodIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() ->
						this.methods.add((String) null, methodSpecCustomizer))
				.withMessage("'suggestedName' must not be null");
	}

	@Test
	void addWithStringNameWhenMethodIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() ->
						this.methods.add("test", null))
				.withMessage("'method' must not be null");
	}

	@Test
	void addAddsMethod() {
		this.methods.add("springBeans", methodSpecCustomizer);
		this.methods.add("springContext", methodSpecCustomizer);
		assertThat(this.methods.stream().map(GeneratedMethod::getName).map(Object::toString))
				.containsExactly("springBeans", "springContext");
	}

	@Test
	void withPrefixWhenGeneratingGetMethodUsesPrefix() {
		GeneratedMethod generateMethod = this.methods.withPrefix("myBean")
				.add("getTest", methodSpecCustomizer);
		assertThat(generateMethod.getName()).hasToString("getMyBeanTest");
	}

	@Test
	void withPrefixWhenGeneratingSetMethodUsesPrefix() {
		GeneratedMethod generateMethod = this.methods.withPrefix("myBean")
				.add("setTest", methodSpecCustomizer);
		assertThat(generateMethod.getName()).hasToString("setMyBeanTest");
	}

	@Test
	void withPrefixWhenGeneratingIsMethodUsesPrefix() {
		GeneratedMethod generateMethod = this.methods.withPrefix("myBean")
				.add("isTest", methodSpecCustomizer);
		assertThat(generateMethod.getName()).hasToString("isMyBeanTest");
	}

	@Test
	void withPrefixWhenGeneratingOtherMethodUsesPrefix() {
		GeneratedMethod generateMethod = this.methods.withPrefix("myBean")
				.add("test", methodSpecCustomizer);
		assertThat(generateMethod.getName()).hasToString("myBeanTest");
	}

	@Test
	void doWithMethodSpecsAcceptsMethodSpecs() {
		this.methods.add("springBeans", methodSpecCustomizer);
		this.methods.add("springContext", methodSpecCustomizer);
		List<String> names = new ArrayList<>();
		this.methods.doWithMethodSpecs(methodSpec -> names.add(methodSpec.name));
		assertThat(names).containsExactly("springBeans", "springContext");
	}

}
