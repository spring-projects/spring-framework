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

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Consumer;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.AssertFactory;
import org.assertj.core.api.AssertProvider;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.error.BasicErrorMessageFactory;
import org.assertj.core.internal.Failures;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.test.http.HttpMessageContentConverter;
import org.springframework.util.Assert;

/**
 * Base AssertJ {@linkplain org.assertj.core.api.Assert assertions} that can be
 * applied to a JSON document.
 *
 * <p>Supports evaluating {@linkplain JsonPath JSON path} expressions and
 * extracting a part of the document for further {@linkplain JsonPathValueAssert
 * assertions} on the value.
 *
 * <p>Also supports comparing the JSON document against a target, using a
 * {@linkplain JsonComparator JSON Comparator}. Resources that are loaded from
 * the classpath can be relative if a {@linkplain #withResourceLoadClass(Class)
 * class} is provided. By default, {@code UTF-8} is used to load resources,
 * but this can be overridden using {@link #withCharset(Charset)}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Diego Berrueta
 * @author Camille Vienot
 * @since 6.2
 * @param <SELF> the type of assertions
 */
public abstract class AbstractJsonContentAssert<SELF extends AbstractJsonContentAssert<SELF>>
		extends AbstractObjectAssert<SELF, JsonContent> {

	private static final Failures failures = Failures.instance();


	@Nullable
	private final HttpMessageContentConverter contentConverter;

	@Nullable
	private Class<?> resourceLoadClass;

	@Nullable
	private Charset charset;

	private JsonLoader jsonLoader;

	/**
	 * Create an assert for the given JSON document.
	 * @param actual the JSON document to assert
	 * @param selfType the implementation type of this assert
	 */
	protected AbstractJsonContentAssert(@Nullable JsonContent actual, Class<?> selfType) {
		super(actual, selfType);
		this.contentConverter = (actual != null ? actual.getContentConverter() : null);
		this.jsonLoader = new JsonLoader(null, null);
		as("JSON content");
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
	 * // Check that the JSON document is an array of 3 users
	 * assertThat(json).convertTo(InstanceOfAssertFactories.list(User.class))
	 *         hasSize(3); // ListAssert of User
	 * </code></pre>
	 * @param assertFactory the {@link AssertFactory} to use to produce a narrowed
	 * Assert for the type that it defines.
	 */
	public <ASSERT extends AbstractAssert<?, ?>> ASSERT convertTo(AssertFactory<?, ASSERT> assertFactory) {
		isNotNull();
		return assertFactory.createAssert(this::convertToTargetType);
	}

	private <T> T convertToTargetType(Type targetType) {
		String json = this.actual.getJson();
		if (this.contentConverter == null) {
			throw new IllegalStateException(
					"No JSON message converter available to convert %s".formatted(json));
		}
		try {
			return this.contentConverter.convert(fromJson(json), MediaType.APPLICATION_JSON,
					ResolvableType.forType(targetType));
		}
		catch (Exception ex) {
			throw failure(new ValueProcessingFailed(json,
					"To convert successfully to:%n  %s%nBut it failed:%n  %s%n".formatted(
							targetType.getTypeName(), ex.getMessage())));
		}
	}

	private HttpInputMessage fromJson(String json) {
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(json.getBytes(StandardCharsets.UTF_8));
		inputMessage.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		return inputMessage;
	}


	// JsonPath support

	/**
	 * Verify that the given JSON {@code path} is present, and extract the JSON
	 * value for further {@linkplain JsonPathValueAssert assertions}.
	 * @param path the {@link JsonPath} expression
	 * @see #hasPathSatisfying(String, Consumer)
	 */
	public JsonPathValueAssert extractingPath(String path) {
		Object value = new JsonPathValue(path).getValue();
		return new JsonPathValueAssert(value, path, this.contentConverter);
	}

	/**
	 * Verify that the given JSON {@code path} is present with a JSON value
	 * satisfying the given {@code valueRequirements}.
	 * @param path the {@link JsonPath} expression
	 * @param valueRequirements a {@link Consumer} of the assertion object
	 */
	public SELF hasPathSatisfying(String path, Consumer<AssertProvider<JsonPathValueAssert>> valueRequirements) {
		Object value = new JsonPathValue(path).assertHasPath();
		JsonPathValueAssert valueAssert = new JsonPathValueAssert(value, path, this.contentConverter);
		valueRequirements.accept(() -> valueAssert);
		return this.myself;
	}

	/**
	 * Verify that the given JSON {@code path} matches. For paths with an
	 * operator, this validates that the path expression is valid, but does not
	 * validate that it yield any results.
	 * @param path the {@link JsonPath} expression
	 */
	public SELF hasPath(String path) {
		new JsonPathValue(path).assertHasPath();
		return this.myself;
	}

	/**
	 * Verify that the given JSON {@code path} does not match.
	 * @param path the {@link JsonPath} expression
	 */
	public SELF doesNotHavePath(String path) {
		new JsonPathValue(path).assertDoesNotHavePath();
		return this.myself;
	}

	// JsonAssert support

	/**
	 * Verify that the actual value is {@linkplain JsonCompareMode#STRICT strictly}
	 * equal to the given JSON. The {@code expected} value can contain the JSON
	 * itself or, if it ends with {@code .json}, the name of a resource to be
	 * loaded from the classpath.
	 * @param expected the expected JSON or the name of a resource containing
	 * the expected JSON
	 * @see #isEqualTo(CharSequence, JsonCompareMode)
	 */
	public SELF isEqualTo(@Nullable CharSequence expected) {
		return isEqualTo(expected, JsonCompareMode.STRICT);
	}

	/**
	 * Verify that the actual value is equal to the given JSON. The
	 * {@code expected} value can contain the JSON itself or, if it ends with
	 * {@code .json}, the name of a resource to be loaded from the classpath.
	 * @param expected the expected JSON or the name of a resource containing
	 * the expected JSON
	 * @param compareMode the compare mode used when checking
	 */
	public SELF isEqualTo(@Nullable CharSequence expected, JsonCompareMode compareMode) {
		String expectedJson = this.jsonLoader.getJson(expected);
		return assertIsMatch(compare(expectedJson, compareMode));
	}

	/**
	 * Verify that the actual value is equal to the given JSON {@link Resource}.
	 * <p>The resource abstraction allows to provide several input types:
	 * <ul>
	 * <li>a {@code byte} array, using {@link ByteArrayResource}</li>
	 * <li>a {@code classpath} resource, using {@link ClassPathResource}</li>
	 * <li>a {@link File} or {@link Path}, using {@link FileSystemResource}</li>
	 * <li>an {@link InputStream}, using {@link InputStreamResource}</li>
	 * </ul>
	 * @param expected a resource containing the expected JSON
	 * @param compareMode the compare mode used when checking
	 */
	public SELF isEqualTo(Resource expected, JsonCompareMode compareMode) {
		String expectedJson = this.jsonLoader.getJson(expected);
		return assertIsMatch(compare(expectedJson, compareMode));
	}

	/**
	 * Verify that the actual value is equal to the given JSON. The
	 * {@code expected} value can contain the JSON itself or, if it ends with
	 * {@code .json}, the name of a resource to be loaded from the classpath.
	 * @param expected the expected JSON or the name of a resource containing
	 * the expected JSON
	 * @param comparator the comparator used when checking
	 */
	public SELF isEqualTo(@Nullable CharSequence expected, JsonComparator comparator) {
		String expectedJson = this.jsonLoader.getJson(expected);
		return assertIsMatch(compare(expectedJson, comparator));
	}

	/**
	 * Verify that the actual value is equal to the given JSON {@link Resource}.
	 * <p>The resource abstraction allows to provide several input types:
	 * <ul>
	 * <li>a {@code byte} array, using {@link ByteArrayResource}</li>
	 * <li>a {@code classpath} resource, using {@link ClassPathResource}</li>
	 * <li>a {@link File} or {@link Path}, using {@link FileSystemResource}</li>
	 * <li>an {@link InputStream}, using {@link InputStreamResource}</li>
	 * </ul>
	 * @param expected a resource containing the expected JSON
	 * @param comparator the comparator used when checking
	 */
	public SELF isEqualTo(Resource expected, JsonComparator comparator) {
		String expectedJson = this.jsonLoader.getJson(expected);
		return assertIsMatch(compare(expectedJson, comparator));
	}

	/**
	 * Verify that the actual value is {@link JsonCompareMode#LENIENT leniently}
	 * equal to the given JSON. The {@code expected} value can contain the JSON
	 * itself or, if it ends with {@code .json}, the name of a resource to be
	 * loaded from the classpath.
	 * @param expected the expected JSON or the name of a resource containing
	 * the expected JSON
	 */
	public SELF isLenientlyEqualTo(@Nullable CharSequence expected) {
		return isEqualTo(expected, JsonCompareMode.LENIENT);
	}

	/**
	 * Verify that the actual value is {@link JsonCompareMode#LENIENT leniently}
	 * equal to the given JSON {@link Resource}.
	 * <p>The resource abstraction allows to provide several input types:
	 * <ul>
	 * <li>a {@code byte} array, using {@link ByteArrayResource}</li>
	 * <li>a {@code classpath} resource, using {@link ClassPathResource}</li>
	 * <li>a {@link File} or {@link Path}, using {@link FileSystemResource}</li>
	 * <li>an {@link InputStream}, using {@link InputStreamResource}</li>
	 * </ul>
	 * @param expected a resource containing the expected JSON
	 */
	public SELF isLenientlyEqualTo(Resource expected) {
		return isEqualTo(expected, JsonCompareMode.LENIENT);
	}

	/**
	 * Verify that the actual value is {@link JsonCompareMode#STRICT strictly}
	 * equal to the given JSON. The {@code expected} value can contain the JSON
	 * itself or, if it ends with {@code .json}, the name of a resource to be
	 * loaded from the classpath.
	 * @param expected the expected JSON or the name of a resource containing
	 * the expected JSON
	 */
	public SELF isStrictlyEqualTo(@Nullable CharSequence expected) {
		return isEqualTo(expected, JsonCompareMode.STRICT);
	}

	/**
	 * Verify that the actual value is {@link JsonCompareMode#STRICT strictly}
	 * equal to the given JSON {@link Resource}.
	 * <p>The resource abstraction allows to provide several input types:
	 * <ul>
	 * <li>a {@code byte} array, using {@link ByteArrayResource}</li>
	 * <li>a {@code classpath} resource, using {@link ClassPathResource}</li>
	 * <li>a {@link File} or {@link Path}, using {@link FileSystemResource}</li>
	 * <li>an {@link InputStream}, using {@link InputStreamResource}</li>
	 * </ul>
	 * @param expected a resource containing the expected JSON
	 */
	public SELF isStrictlyEqualTo(Resource expected) {
		return isEqualTo(expected, JsonCompareMode.STRICT);
	}

	/**
	 * Verify that the actual value is {@linkplain JsonCompareMode#STRICT strictly}
	 * not equal to the given JSON. The {@code expected} value can contain the
	 * JSON itself or, if it ends with {@code .json}, the name of a resource to
	 * be loaded from the classpath.
	 * @param expected the expected JSON or the name of a resource containing
	 * the expected JSON
	 * @see #isNotEqualTo(CharSequence, JsonCompareMode)
	 */
	public SELF isNotEqualTo(@Nullable CharSequence expected) {
		return isNotEqualTo(expected, JsonCompareMode.STRICT);
	}

	/**
	 * Verify that the actual value is not equal to the given JSON. The
	 * {@code expected} value can contain the JSON itself or, if it ends with
	 * {@code .json}, the name of a resource to be loaded from the classpath.
	 * @param expected the expected JSON or the name of a resource containing
	 * the expected JSON
	 * @param compareMode the compare mode used when checking
	 */
	public SELF isNotEqualTo(@Nullable CharSequence expected, JsonCompareMode compareMode) {
		String expectedJson = this.jsonLoader.getJson(expected);
		return assertIsMismatch(compare(expectedJson, compareMode));
	}

	/**
	 * Verify that the actual value is not equal to the given JSON {@link Resource}.
	 * <p>The resource abstraction allows to provide several input types:
	 * <ul>
	 * <li>a {@code byte} array, using {@link ByteArrayResource}</li>
	 * <li>a {@code classpath} resource, using {@link ClassPathResource}</li>
	 * <li>a {@link File} or {@link Path}, using {@link FileSystemResource}</li>
	 * <li>an {@link InputStream}, using {@link InputStreamResource}</li>
	 * </ul>
	 * @param expected a resource containing the expected JSON
	 * @param compareMode the compare mode used when checking
	 */
	public SELF isNotEqualTo(Resource expected, JsonCompareMode compareMode) {
		String expectedJson = this.jsonLoader.getJson(expected);
		return assertIsMismatch(compare(expectedJson, compareMode));
	}

	/**
	 * Verify that the actual value is not equal to the given JSON. The
	 * {@code expected} value can contain the JSON itself or, if it ends with
	 * {@code .json}, the name of a resource to be loaded from the classpath.
	 * @param expected the expected JSON or the name of a resource containing
	 * the expected JSON
	 * @param comparator the comparator used when checking
	 */
	public SELF isNotEqualTo(@Nullable CharSequence expected, JsonComparator comparator) {
		String expectedJson = this.jsonLoader.getJson(expected);
		return assertIsMismatch(compare(expectedJson, comparator));
	}

	/**
	 * Verify that the actual value is not equal to the given JSON {@link Resource}.
	 * <p>The resource abstraction allows to provide several input types:
	 * <ul>
	 * <li>a {@code byte} array, using {@link ByteArrayResource}</li>
	 * <li>a {@code classpath} resource, using {@link ClassPathResource}</li>
	 * <li>a {@link File} or {@link Path}, using {@link FileSystemResource}</li>
	 * <li>an {@link InputStream}, using {@link InputStreamResource}</li>
	 * </ul>
	 * @param expected a resource containing the expected JSON
	 * @param comparator the comparator used when checking
	 */
	public SELF isNotEqualTo(Resource expected, JsonComparator comparator) {
		String expectedJson = this.jsonLoader.getJson(expected);
		return assertIsMismatch(compare(expectedJson, comparator));
	}

	/**
	 * Verify that the actual value is not {@link JsonCompareMode#LENIENT
	 * leniently} equal to the given JSON. The {@code expected} value can
	 * contain the JSON itself or, if it ends with {@code .json}, the name of a
	 * resource to be loaded from the classpath.
	 * @param expected the expected JSON or the name of a resource containing
	 * the expected JSON
	 */
	public SELF isNotLenientlyEqualTo(@Nullable CharSequence expected) {
		return isNotEqualTo(expected, JsonCompareMode.LENIENT);
	}

	/**
	 * Verify that the actual value is not {@link JsonCompareMode#LENIENT
	 * leniently} equal to the given JSON {@link Resource}.
	 * <p>The resource abstraction allows to provide several input types:
	 * <ul>
	 * <li>a {@code byte} array, using {@link ByteArrayResource}</li>
	 * <li>a {@code classpath} resource, using {@link ClassPathResource}</li>
	 * <li>a {@link File} or {@link Path}, using {@link FileSystemResource}</li>
	 * <li>an {@link InputStream}, using {@link InputStreamResource}</li>
	 * </ul>
	 * @param expected a resource containing the expected JSON
	 */
	public SELF isNotLenientlyEqualTo(Resource expected) {
		return isNotEqualTo(expected, JsonCompareMode.LENIENT);
	}

	/**
	 * Verify that the actual value is not {@link JsonCompareMode#STRICT
	 * strictly} equal to the given JSON. The {@code expected} value can
	 * contain the JSON itself or, if it ends with {@code .json}, the name of a
	 * resource to be loaded from the classpath.
	 * @param expected the expected JSON or the name of a resource containing
	 * the expected JSON
	 */
	public SELF isNotStrictlyEqualTo(@Nullable CharSequence expected) {
		return isNotEqualTo(expected, JsonCompareMode.STRICT);
	}

	/**
	 * Verify that the actual value is not {@link JsonCompareMode#STRICT
	 * strictly} equal to the given JSON {@link Resource}.
	 * <p>The resource abstraction allows to provide several input types:
	 * <ul>
	 * <li>a {@code byte} array, using {@link ByteArrayResource}</li>
	 * <li>a {@code classpath} resource, using {@link ClassPathResource}</li>
	 * <li>a {@link File} or {@link Path}, using {@link FileSystemResource}</li>
	 * <li>an {@link InputStream}, using {@link InputStreamResource}</li>
	 * </ul>
	 * @param expected a resource containing the expected JSON
	 */
	public SELF isNotStrictlyEqualTo(Resource expected) {
		return isNotEqualTo(expected, JsonCompareMode.STRICT);
	}

	/**
	 * Override the class used to load resources.
	 * <p>Resources can be loaded from an absolute location or relative to the
	 * specified class. For instance, specifying {@code com.example.MyClass} as
	 * the resource class allows you to use "my-file.json" to load
	 * {@code /com/example/my-file.json}.
	 * @param resourceLoadClass the class used to load resources, or {@code null}
	 * to only use absolute paths
	 */
	public SELF withResourceLoadClass(@Nullable Class<?> resourceLoadClass) {
		this.resourceLoadClass = resourceLoadClass;
		this.jsonLoader = new JsonLoader(resourceLoadClass, this.charset);
		return this.myself;
	}

	/**
	 * Override the {@link Charset} to use to load resources.
	 * <p>By default, resources are loaded using {@code UTF-8}.
	 * @param charset the charset to use, or {@code null} to use the default
	 */
	public SELF withCharset(@Nullable Charset charset) {
		this.charset = charset;
		this.jsonLoader = new JsonLoader(this.resourceLoadClass, charset);
		return this.myself;
	}

	@Nullable
	private String toJsonString() {
		return (this.actual != null ? this.actual.getJson() : null);
	}

	@SuppressWarnings("NullAway")
	private String toNonNullJsonString() {
		String jsonString = toJsonString();
		Assertions.assertThat(jsonString).as("JSON content").isNotNull();
		return jsonString;
	}

	private JsonComparison compare(@Nullable CharSequence expectedJson, JsonCompareMode compareMode) {
		return compare(expectedJson, JsonAssert.comparator(compareMode));
	}

	private JsonComparison compare(@Nullable CharSequence expectedJson, JsonComparator comparator) {
		return comparator.compare((expectedJson != null) ? expectedJson.toString() : null, toJsonString());
	}

	private SELF assertIsMatch(JsonComparison result) {
		return assertComparison(result, JsonComparison.Result.MATCH);
	}

	private SELF assertIsMismatch(JsonComparison result) {
		return assertComparison(result, JsonComparison.Result.MISMATCH);
	}

	private SELF assertComparison(JsonComparison jsonComparison, JsonComparison.Result requiredResult) {
		if (jsonComparison.getResult() != requiredResult) {
			failWithMessage("JSON comparison failure: %s", jsonComparison.getMessage());
		}
		return this.myself;
	}

	private AssertionError failure(BasicErrorMessageFactory errorMessageFactory) {
		throw failures.failure(this.info, errorMessageFactory);
	}


	/**
	 * A {@link JsonPath} value.
	 */
	private class JsonPathValue {

		private final String path;

		private final String json;

		private final JsonPath jsonPath;

		JsonPathValue(String path) {
			Assert.hasText(path, "'path' must not be null or empty");
			this.path = path;
			this.json = toNonNullJsonString();
			this.jsonPath = JsonPath.compile(this.path);
		}

		@Nullable
		Object assertHasPath() {
			return getValue();
		}

		void assertDoesNotHavePath() {
			try {
				read();
				throw failure(new JsonPathNotExpected(this.json, this.path));
			}
			catch (PathNotFoundException ignore) {
			}
		}

		@Nullable
		Object getValue() {
			try {
				return read();
			}
			catch (PathNotFoundException ex) {
				throw failure(new JsonPathNotFound(this.json, this.path));
			}
		}

		@Nullable
		private Object read() {
			return this.jsonPath.read(this.json);
		}


		static final class JsonPathNotFound extends BasicErrorMessageFactory {

			private JsonPathNotFound(String actual, String path) {
				super("%nExpecting:%n  %s%nTo match JSON path:%n  %s%n", actual, path);
			}
		}

		static final class JsonPathNotExpected extends BasicErrorMessageFactory {

			private JsonPathNotExpected(String actual, String path) {
				super("%nExpecting:%n  %s%nNot to match JSON path:%n  %s%n", actual, path);
			}
		}
	}

	private static final class ValueProcessingFailed extends BasicErrorMessageFactory {

		private ValueProcessingFailed(String actualToString, String errorMessage) {
			super("%nExpected:%n  %s%n%s".formatted(actualToString, errorMessage));
		}
	}

}
