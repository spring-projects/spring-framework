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

package org.springframework.test.json;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.AbstractMapAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.AssertFactory;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ObjectArrayAssert;
import org.assertj.core.error.BasicErrorMessageFactory;
import org.assertj.core.internal.Failures;

import org.springframework.core.ResolvableType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.test.http.HttpMessageContentConverter;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Base AssertJ {@linkplain org.assertj.core.api.Assert assertions} that can be
 * applied to a JSON value.
 *
 * <p>In JSON, values must be one of the following data types:
 * <ul>
 * <li>a {@linkplain #asString() string}</li>
 * <li>a {@linkplain #asNumber() number}</li>
 * <li>a {@linkplain #asBoolean() boolean}</li>
 * <li>an {@linkplain #asArray() array}</li>
 * <li>an {@linkplain #asMap() object} (JSON object)</li>
 * <li>{@linkplain #isNull() null}</li>
 * </ul>
 * This base class offers direct access for each of those types as well as
 * conversion methods based on an optional {@link GenericHttpMessageConverter}.
 *
 * @author Stephane Nicoll
 * @since 6.2
 * @param <SELF> the type of assertions
 */
public abstract class AbstractJsonValueAssert<SELF extends AbstractJsonValueAssert<SELF>>
		extends AbstractObjectAssert<SELF, Object> {

	private final Failures failures = Failures.instance();

	@Nullable
	private final HttpMessageContentConverter contentConverter;


	protected AbstractJsonValueAssert(@Nullable Object actual, Class<?> selfType,
			@Nullable HttpMessageContentConverter contentConverter) {

		super(actual, selfType);
		this.contentConverter = contentConverter;
	}


	/**
	 * Verify that the actual value is a non-{@code null} {@link String},
	 * and return a new {@linkplain AbstractStringAssert assertion} object that
	 * provides dedicated {@code String} assertions for it.
	 */
	@Override
	public AbstractStringAssert<?> asString() {
		return Assertions.assertThat(castTo(String.class, "a string"));
	}

	/**
	 * Verify that the actual value is a non-{@code null} {@link Number},
	 * usually an {@link Integer} or {@link Double}, and return a new
	 * {@linkplain AbstractObjectAssert assertion} object for it.
	 */
	public AbstractObjectAssert<?, Number> asNumber() {
		return Assertions.assertThat(castTo(Number.class, "a number"));
	}

	/**
	 * Verify that the actual value is a non-{@code null} {@link Boolean},
	 * and return a new {@linkplain AbstractBooleanAssert assertion} object
	 * that provides dedicated {@code Boolean} assertions for it.
	 */
	public AbstractBooleanAssert<?> asBoolean() {
		return Assertions.assertThat(castTo(Boolean.class, "a boolean"));
	}

	/**
	 * Verify that the actual value is a non-{@code null} array, and return a
	 * new {@linkplain ObjectArrayAssert assertion} object that provides dedicated
	 * array assertions for it.
	 */
	public ObjectArrayAssert<Object> asArray() {
		List<?> list = castTo(List.class, "an array");
		Object[] array = list.toArray(new Object[0]);
		return Assertions.assertThat(array);
	}

	/**
	 * Verify that the actual value is a non-{@code null} JSON object, and
	 * return a new {@linkplain AbstractMapAssert assertion} object that
	 * provides dedicated assertions on individual elements of the
	 * object.
	 * <p>The returned map assertion object uses attribute names as the keys,
	 * and the values can be any of the valid JSON values.
	 */
	@SuppressWarnings("unchecked")
	public AbstractMapAssert<?, Map<String, Object>, String, Object> asMap() {
		return Assertions.assertThat(castTo(Map.class, "a map"));
	}

	private <T> T castTo(Class<T> expectedType, String description) {
		if (this.actual == null) {
			throw valueProcessingFailed("To be %s%n".formatted(description));
		}
		if (!expectedType.isInstance(this.actual)) {
			throw valueProcessingFailed("To be %s%nBut was:%n  %s%n".formatted(description, this.actual.getClass().getName()));
		}
		return expectedType.cast(this.actual);
	}

	/**
	 * Verify that the actual value can be converted to an instance of the
	 * given {@code target}, and produce a new {@linkplain AbstractObjectAssert
	 * assertion} object narrowed to that type.
	 * @param target the {@linkplain Class type} to convert the actual value to
	 */
	public <T> AbstractObjectAssert<?, T> convertTo(Class<T> target) {
		isNotNull();
		T value = convertToTargetType(target);
		return Assertions.assertThat(value);
	}

	/**
	 * Verify that the actual value can be converted to an instance of the type
	 * defined by the given {@link AssertFactory} and return a new Assert narrowed
	 * to that type.
	 * <p>{@link InstanceOfAssertFactories} provides static factories for all the
	 * types supported by {@link Assertions#assertThat}. Additional factories can
	 * be created by implementing {@link AssertFactory}.
	 * <p>Example: <pre><code class="java">
	 * // Check that the json value is an array of 3 users
	 * assertThat(jsonValue).convertTo(InstanceOfAssertFactories.list(User.class))
	 *         hasSize(3); // ListAssert of User
	 * </code></pre>
	 * @param assertFactory the {@link AssertFactory} to use to produce a narrowed
	 * Assert for the type that it defines.
	 */
	public <ASSERT extends AbstractAssert<?, ?>> ASSERT convertTo(AssertFactory<?, ASSERT> assertFactory) {
		isNotNull();
		return assertFactory.createAssert(this::convertToTargetType);
	}

	/**
	 * Verify that the actual value is empty: either a {@code null} scalar value
	 * or an empty list or map.
	 * <p>Can also be used when the path uses a filter operator to validate that
	 * it did not match.
	 */
	public SELF isEmpty() {
		if (!ObjectUtils.isEmpty(this.actual)) {
			throw valueProcessingFailed("To be empty");
		}
		return this.myself;
	}

	/**
	 * Verify that the actual value is not empty: either a non-{@code null}
	 * scalar value or a non-empty list or map.
	 * <p>Can also be used when the path uses a filter operator to validate that
	 * it did match at least one element.
	 */
	public SELF isNotEmpty() {
		if (ObjectUtils.isEmpty(this.actual)) {
			throw valueProcessingFailed("To not be empty");
		}
		return this.myself;
	}

	private <T> T convertToTargetType(Type targetType) {
		if (this.contentConverter == null) {
			throw new IllegalStateException(
					"No JSON message converter available to convert %s".formatted(actualToString()));
		}
		try {
			return this.contentConverter.convertViaJson(this.actual, ResolvableType.forType(targetType));
		}
		catch (Exception ex) {
			throw valueProcessingFailed("To convert successfully to:%n  %s%nBut it failed:%n  %s%n"
					.formatted(targetType.getTypeName(), ex.getMessage()));
		}
	}

	protected String getExpectedErrorMessagePrefix() {
		return "Expected:";
	}

	private AssertionError valueProcessingFailed(String errorMessage) {
		throw this.failures.failure(this.info, new ValueProcessingFailed(
				getExpectedErrorMessagePrefix(), actualToString(), errorMessage));
	}

	private String actualToString() {
		return ObjectUtils.nullSafeToString(StringUtils.quoteIfString(this.actual));
	}


	private static final class ValueProcessingFailed extends BasicErrorMessageFactory {

		private ValueProcessingFailed(String prefix, String actualToString, String errorMessage) {
			super("%n%s%n  %s%n%s".formatted(prefix, actualToString, errorMessage));
		}
	}

}
