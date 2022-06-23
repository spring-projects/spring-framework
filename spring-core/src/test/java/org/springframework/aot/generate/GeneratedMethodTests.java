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

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.javapoet.MethodSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link GeneratedMethod}.
 *
 * @author Phillip Webb
 */
class GeneratedMethodTests {

	private static final Consumer<MethodSpec.Builder> methodSpecCustomizer = method -> {};

	private static final String NAME = "spring";

	@Test
	void getNameReturnsName() {
		GeneratedMethod generatedMethod = new GeneratedMethod(NAME, methodSpecCustomizer);
		assertThat(generatedMethod.getName()).isSameAs(NAME);
	}

	@Test
	void generateMethodSpecReturnsMethodSpec() {
		GeneratedMethod generatedMethod = new GeneratedMethod(NAME, method -> method.addJavadoc("Test"));
		assertThat(generatedMethod.getMethodSpec().javadoc).asString().contains("Test");
	}

	@Test
	void generateMethodSpecWhenMethodNameIsChangedThrowsException() {
		assertThatIllegalStateException().isThrownBy(() ->
				new GeneratedMethod(NAME, method -> method.setName("badname")).getMethodSpec())
			.withMessage("'method' consumer must not change the generated method name");
	}

}
