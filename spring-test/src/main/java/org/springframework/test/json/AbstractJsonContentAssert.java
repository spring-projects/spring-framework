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
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.function.Consumer;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.AssertProvider;
import org.assertj.core.error.BasicErrorMessageFactory;
import org.assertj.core.internal.Failures;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.lang.Nullable;
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
		extends AbstractStringAssert<SELF> {

	private static final Failures failures = Failures.instance();


	@Nullable
	private final GenericHttpMessageConverter<Object> jsonMessageConverter;

	@Nullable
	private Class<?> resourceLoadClass;

	@Nullable
	private Charset charset;

	private JsonLoader jsonLoader;

	/**
	 * Create an assert for the given JSON document.
	 * <p>Path can be converted to a value object using the given
	 * {@linkplain GenericHttpMessageConverter JSON message converter}.
	 * @param json the JSON document to assert
	 * @param jsonMessageConverter the converter to use
	 * @param selfType the implementation type of this assert
	 */
	protected AbstractJsonContentAssert(@Nullable String json,
			@Nullable GenericHttpMessageConverter<Object> jsonMessageConverter, Class<?> selfType) {
		super(json, selfType);
		this.jsonMessageConverter = jsonMessageConverter;
		this.jsonLoader = new JsonLoader(null, null);
		as("JSON content");
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
		return new JsonPathValueAssert(value, path, this.jsonMessageConverter);
	}

	/**
	 * Verify that the given JSON {@code path} is present with a JSON value
	 * satisfying the given {@code valueRequirements}.
	 * @param path the {@link JsonPath} expression
	 * @param valueRequirements a {@link Consumer} of the assertion object
	 */
	public SELF hasPathSatisfying(String path, Consumer<AssertProvider<JsonPathValueAssert>> valueRequirements) {
		Object value = new JsonPathValue(path).assertHasPath();
		JsonPathValueAssert valueAssert = new JsonPathValueAssert(value, path, this.jsonMessageConverter);
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


	private JsonComparison compare(@Nullable CharSequence expectedJson, JsonCompareMode compareMode) {
		return compare(expectedJson, JsonAssert.comparator(compareMode));
	}

	private JsonComparison compare(@Nullable CharSequence expectedJson, JsonComparator comparator) {
		return comparator.compare((expectedJson != null) ? expectedJson.toString() : null, this.actual);
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

		private final JsonPath jsonPath;

		private final String json;

		JsonPathValue(String path) {
			Assert.hasText(path, "'path' must not be null or empty");
			isNotNull();
			this.path = path;
			this.jsonPath = JsonPath.compile(this.path);
			this.json = AbstractJsonContentAssert.this.actual;
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

}
