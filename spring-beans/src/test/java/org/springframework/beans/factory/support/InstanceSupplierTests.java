/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.support;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.util.function.ThrowingBiFunction;

import static org.assertj.core.api.Assertions.assertThat;
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
