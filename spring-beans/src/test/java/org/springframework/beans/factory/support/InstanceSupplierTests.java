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

package org.springframework.beans.factory.support;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.util.function.ThrowingBiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link InstanceSupplier}.
 *
 * @author Phillip Webb
 */
class InstanceSupplierTests {

	private final RegisteredBean registeredBean = RegisteredBean
			.of(new DefaultListableBeanFactory(), "test");

	@Test
	void getWithoutRegisteredBeanThrowsException() {
		InstanceSupplier<String> supplier = registeredBean -> "test";
		assertThatIllegalStateException().isThrownBy(() -> supplier.get())
				.withMessage("No RegisteredBean parameter provided");
	}

	@Test
	void getWithExceptionWithoutRegisteredBeanThrowsException() {
		InstanceSupplier<String> supplier = registeredBean -> "test";
		assertThatIllegalStateException().isThrownBy(() -> supplier.getWithException())
				.withMessage("No RegisteredBean parameter provided");
	}

	@Test
	void getReturnsResult() throws Exception {
		InstanceSupplier<String> supplier = registeredBean -> "test";
		assertThat(supplier.get(this.registeredBean)).isEqualTo("test");
	}

	@Test
	void getWithEmptyExplicitArgumentsReturnsResult() throws Exception {
		InstanceSupplier<String> supplier = registeredBean -> "test";
		assertThat(supplier.get(this.registeredBean, new Object[0])).isEqualTo("test");
	}

	@Test
	void getWithExplicitArgumentsWhenNotSupportedThrowsException() {
		InstanceSupplier<String> supplier = registeredBean -> "test";
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> supplier.get(this.registeredBean, "test"))
				.withMessageContaining("Retrieval with arguments not supported");
	}

	@Test
	void supportsExplicitArgumentsReturnsFalse() {
		InstanceSupplier<String> supplier = registeredBean -> "test";
		assertThat(supplier.supportsExplicitArguments()).isFalse();
	}

	@Test
	void andThenWhenFunctionIsNullThrowsException() {
		InstanceSupplier<String> supplier = registeredBean -> "test";
		ThrowingBiFunction<RegisteredBean, String, String> after = null;
		assertThatIllegalArgumentException().isThrownBy(() -> supplier.andThen(after))
				.withMessage("'after' function must not be null");
	}

	@Test
	void andThenAppliesFunctionToObtainResult() throws Exception {
		InstanceSupplier<String> supplier = registeredBean -> "bean";
		supplier = supplier.andThen(
				(registeredBean, string) -> registeredBean.getBeanName() + "-" + string);
		assertThat(supplier.get(this.registeredBean)).isEqualTo("test-bean");
	}

	@Test
	void andThenAppliesFunctionToObtainResultWithExplicitArguments() throws Exception {
		InstanceSupplier<String> supplier = new InstanceSupplier<>() {
			@Override
			public String get(RegisteredBean registeredBean) {
				return "bean";
			}
			@Override
			public String get(RegisteredBean registeredBean, Object... args) {
				return (String) args[0];
			}
			@Override
			public boolean supportsExplicitArguments(Object... args) {
				return true;
			}
		};
		supplier = supplier.andThen(
				(registeredBean, string) -> registeredBean.getBeanName() + "-" + string);
		assertThat(supplier.supportsExplicitArguments()).isTrue();
		assertThat(supplier.get(this.registeredBean, "bean")).isEqualTo("test-bean");
	}

	@Test
	void andThenWhenInstanceSupplierHasFactoryMethod() throws Exception {
		Method factoryMethod = getClass().getDeclaredMethod("andThenWhenInstanceSupplierHasFactoryMethod");
		InstanceSupplier<String> supplier = InstanceSupplier.using(factoryMethod, () -> "bean");
		supplier = supplier.andThen(
				(registeredBean, string) -> registeredBean.getBeanName() + "-" + string);
		assertThat(supplier.get(this.registeredBean)).isEqualTo("test-bean");
		assertThat(supplier.getFactoryMethod()).isSameAs(factoryMethod);
	}

	@Test
	void ofSupplierWhenInstanceSupplierReturnsSameInstance() {
		InstanceSupplier<String> supplier = registeredBean -> "test";
		assertThat(InstanceSupplier.of(supplier)).isSameAs(supplier);
	}

	@Test
	void usingSupplierAdaptsToInstanceSupplier() throws Exception {
		InstanceSupplier<String> instanceSupplier = InstanceSupplier.using(() -> "test");
		assertThat(instanceSupplier.get(this.registeredBean)).isEqualTo("test");
	}

	@Test
	void ofInstanceSupplierAdaptsToInstanceSupplier() throws Exception {
		InstanceSupplier<String> instanceSupplier = InstanceSupplier
				.of(registeredBean -> "test");
		assertThat(instanceSupplier.get(this.registeredBean)).isEqualTo("test");
	}

}
