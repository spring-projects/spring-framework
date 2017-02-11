/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.test.web.reactive.server;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.test.util.AssertionErrors;

import static org.springframework.test.util.AssertionErrors.assertEquals;

/**
 * Assertions on an Object.
 *
 * @param <V> the type of Object to apply assertions to
 * @param <S> assertion method return type
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ObjectAssertions<V, S extends ObjectAssertions<V, S>> {

	private final ExchangeActions exchangeActions;

	private V value;

	private final Supplier<V> valueSupplier;

	private final String errorPrefix;


	protected ObjectAssertions(ExchangeActions exchangeActions, V value, String errorPrefix) {
		this(exchangeActions, () -> value, errorPrefix);
	}

	protected ObjectAssertions(ExchangeActions exchangeActions, Supplier<V> valueSupplier, String errorPrefix) {
		this.exchangeActions = exchangeActions;
		this.valueSupplier = valueSupplier;
		this.errorPrefix = errorPrefix;
	}


	/**
	 * Assert the actual value is equal to the given expected value.
	 */
	public <T extends S> T isEqualTo(V expected) {
		assertEquals(this.errorPrefix, expected, getValue());
		return self();
	}

	/**
	 * Assert the actual value is not equal to the expected value.
	 */
	public <T extends S> T isNotEqualTo(V expected) {
		assertEquals(this.errorPrefix, expected, getValue());
		return self();
	}

	/**
	 * Apply custom assertions on the Object value with the help of
	 * {@link AssertionErrors} or an assertion library such as AssertJ.
	 * <p>Consider using statically imported methods for creating the assertion
	 * consumer to improve readability of tests.
	 * @param assertionConsumer consumer that will apply assertions.
	 */
	public <T extends S> T andAssert(Consumer<V> assertionConsumer) {
		assertionConsumer.accept(getValue());
		return self();
	}

	/**
	 * Apply custom actions on the Object value.
	 * <p>Consider using statically imported methods for creating the assertion
	 * consumer to improve readability of tests.
	 * @param consumer consumer that will apply the custom action
	 */
	public <T extends S> T andDo(Consumer<V> consumer) {
		consumer.accept(getValue());
		return self();
	}

	/**
	 * Continue with more assertions or actions on the response.
	 */
	public ExchangeActions and() {
		return this.exchangeActions;
	}

	/**
	 * Return {@link ExchangeInfo} for direct access to request and response.
	 */
	public ExchangeInfo andReturn() {
		return this.exchangeActions.andReturn();
	}


	// Protected methods for sub-classes

	protected ExchangeActions getExchangeActions() {
		return this.exchangeActions;
	}

	protected String getErrorPrefix() {
		return this.errorPrefix;
	}

	protected V getValue() {
		if (this.value == null) {
			this.value = this.valueSupplier.get();
		}
		return this.value;
	}

	@SuppressWarnings("unchecked")
	protected <T extends S> T self() {
		return (T) this;
	}

}
