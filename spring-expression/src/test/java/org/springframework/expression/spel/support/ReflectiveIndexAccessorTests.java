/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.expression.spel.support;

import java.lang.reflect.Method;

import example.Color;
import example.FruitMap;
import org.junit.jupiter.api.Test;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.TypedValue;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ReflectiveIndexAccessor}.
 *
 * @author Sam Brannen
 * @since 6.2
 * @see org.springframework.expression.spel.SpelCompilationCoverageTests
 */
class ReflectiveIndexAccessorTests {

	@Test
	void nonexistentReadMethod() {
		Class<?> targetType = getClass();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ReflectiveIndexAccessor(targetType, int.class, "bogus"))
				.withMessage("Failed to find public read-method 'bogus(int)' in class '%s'.", targetType.getCanonicalName());
	}

	@Test
	void nonPublicReadMethod() {
		Class<?> targetType = PrivateReadMethod.class;
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ReflectiveIndexAccessor(targetType, int.class, "get"))
				.withMessage("Failed to find public read-method 'get(int)' in class '%s'.", targetType.getCanonicalName());
	}

	@Test
	void nonPublicWriteMethod() {
		Class<?> targetType = PrivateWriteMethod.class;
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ReflectiveIndexAccessor(targetType, int.class, "get", "set"))
				.withMessage("Failed to find public write-method 'set(int, java.lang.Object)' in class '%s'.",
						targetType.getCanonicalName());
	}

	@Test
	void nonPublicDeclaringClass() {
		Class<?> targetType = NonPublicTargetType.class;
		Method readMethod = ReflectionUtils.findMethod(targetType, "get", int.class);
		ReflectiveIndexAccessor accessor = new ReflectiveIndexAccessor(targetType, int.class, "get");

		assertThatIllegalStateException()
				.isThrownBy(() -> accessor.generateCode(mock(), mock(), mock()))
				.withMessage("Failed to find public declaring class for read-method: %s", readMethod);
	}

	@Test
	void publicReadAndWriteMethods() {
		FruitMap fruitMap = new FruitMap();
		EvaluationContext context = mock();
		ReflectiveIndexAccessor accessor =
				new ReflectiveIndexAccessor(FruitMap.class, Color.class, "getFruit", "setFruit");

		assertThat(accessor.getSpecificTargetClasses()).containsOnly(FruitMap.class);

		assertThat(accessor.canRead(context, this, Color.RED)).isFalse();
		assertThat(accessor.canRead(context, fruitMap, this)).isFalse();
		assertThat(accessor.canRead(context, fruitMap, Color.RED)).isTrue();
		assertThat(accessor.read(context, fruitMap, Color.RED)).extracting(TypedValue::getValue).isEqualTo("cherry");

		assertThat(accessor.canWrite(context, this, Color.RED)).isFalse();
		assertThat(accessor.canWrite(context, fruitMap, this)).isFalse();
		assertThat(accessor.canWrite(context, fruitMap, Color.RED)).isTrue();
		accessor.write(context, fruitMap, Color.RED, "strawberry");
		assertThat(fruitMap.getFruit(Color.RED)).isEqualTo("strawberry");
		assertThat(accessor.read(context, fruitMap, Color.RED)).extracting(TypedValue::getValue).isEqualTo("strawberry");

		assertThat(accessor.isCompilable()).isTrue();
		assertThat(accessor.getIndexedValueType()).isEqualTo(String.class);
		assertThatNoException().isThrownBy(() -> accessor.generateCode(mock(), mock(), mock()));
	}


	public static class PrivateReadMethod {
		Object get(int i) {
			return "foo";
		}
	}

	public static class PrivateWriteMethod {
		public Object get(int i) {
			return "foo";
		}

		void set(int i, String value) {
			// no-op
		}
	}

	static class NonPublicTargetType {
		public Object get(int i) {
			return "foo";
		}
	}

}
