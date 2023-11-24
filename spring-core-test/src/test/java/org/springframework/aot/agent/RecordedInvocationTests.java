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

package org.springframework.aot.agent;


import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link RecordedInvocation}.
 *
 * @author Brian Clozel
 */
class RecordedInvocationTests {

	private RecordedInvocation staticInvocation;

	private RecordedInvocation instanceInvocation;

	@BeforeEach
	void setup() throws Exception {
		staticInvocation = RecordedInvocation.of(InstrumentedMethod.CLASS_FORNAME)
				.withArgument(String.class.getCanonicalName())
				.returnValue(String.class)
				.build();
		instanceInvocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETMETHOD)
				.onInstance(String.class)
				.withArguments("toString", new Class[0])
				.returnValue(String.class.getMethod("toString"))
				.build();
	}

	@Test
	void buildValidStaticInvocation() {
		assertThat(staticInvocation.getHintType()).isEqualTo(HintType.REFLECTION);
		assertThat(staticInvocation.getMethodReference()).isEqualTo(InstrumentedMethod.CLASS_FORNAME.methodReference());
		assertThat(staticInvocation.getArguments()).containsOnly(String.class.getCanonicalName());
		assertThat(staticInvocation.getArgumentTypes()).containsOnly(TypeReference.of(String.class));
		assertThat((Class<?>) staticInvocation.getReturnValue()).isEqualTo(String.class);
		assertThat(staticInvocation.isStatic()).isTrue();
	}

	@Test
	void staticInvocationShouldThrowWhenGetInstance() {
		assertThatThrownBy(staticInvocation::getInstance).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(staticInvocation::getInstanceTypeReference).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void staticInvocationToString() {
		assertThat(staticInvocation.toString()).contains("ReflectionHints", "java.lang.Class#forName", "[java.lang.String]");
	}

	@Test
	void buildValidInstanceInvocation() throws Exception {
		assertThat(instanceInvocation.getHintType()).isEqualTo(HintType.REFLECTION);
		assertThat(instanceInvocation.getMethodReference()).isEqualTo(InstrumentedMethod.CLASS_GETMETHOD.methodReference());
		assertThat(instanceInvocation.getArguments()).containsOnly("toString", new Class[0]);
		assertThat(instanceInvocation.getArgumentTypes()).containsOnly(TypeReference.of(String.class), TypeReference.of(Class[].class));
		Method toString = String.class.getMethod("toString");
		assertThat((Method) instanceInvocation.getReturnValue()).isEqualTo(toString);
		assertThat(instanceInvocation.isStatic()).isFalse();
		assertThat((Class<?>) instanceInvocation.getInstance()).isEqualTo(String.class);
		assertThat(instanceInvocation.getInstanceTypeReference()).isEqualTo(TypeReference.of(Class.class));
	}

	@Test
	void instanceInvocationToString() {
		assertThat(instanceInvocation.toString()).contains("ReflectionHints", "", "java.lang.Class#getMethod",
				"java.lang.String", "[toString, [Ljava.lang.Class;");
	}

}
