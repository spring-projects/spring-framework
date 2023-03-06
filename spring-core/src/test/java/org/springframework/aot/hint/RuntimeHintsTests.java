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

package org.springframework.aot.hint;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RuntimeHints}.
 *
 * @author Stephane Nicoll
 */
class RuntimeHintsTests {

	private final RuntimeHints hints = new RuntimeHints();


	@Test
	void reflectionHintWithClass() {
		this.hints.reflection().registerType(String.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
		assertThat(this.hints.reflection().typeHints()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType().getCanonicalName()).isEqualTo(String.class.getCanonicalName());
			assertThat(typeHint.fields()).isEmpty();
			assertThat(typeHint.constructors()).isEmpty();
			assertThat(typeHint.methods()).isEmpty();
			assertThat(typeHint.getMemberCategories()).containsOnly(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
		});
	}

	@Test
	void resourceHintWithClass() {
		this.hints.resources().registerType(String.class);
		assertThat(this.hints.resources().resourcePatternHints()).singleElement().satisfies(resourceHint -> {
			assertThat(resourceHint.getIncludes()).map(ResourcePatternHint::getPattern)
					.containsExactlyInAnyOrder("/", "java", "java/lang", "java/lang/String.class");
			assertThat(resourceHint.getExcludes()).isEmpty();
		});
	}

	@Test
	void javaSerializationHintWithClass() {
		this.hints.serialization().registerType(String.class);
		assertThat(this.hints.serialization().javaSerializationHints().map(JavaSerializationHint::getType))
				.containsExactly(TypeReference.of(String.class));
	}

	@Test
	void jdkProxyWithClass() {
		this.hints.proxies().registerJdkProxy(Function.class);
		assertThat(this.hints.proxies().jdkProxyHints()).singleElement().satisfies(jdkProxyHint ->
				assertThat(jdkProxyHint.getProxiedInterfaces()).containsExactly(TypeReference.of(Function.class)));
	}

}
