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

import org.assertj.core.api.AbstractAssert;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.JSONComparator;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.function.ThrowingBiFunction;

/**
 * AssertJ {@link org.assertj.core.api.Assert assertions} that can be applied
 * to a {@link CharSequence} representation of a json document, mostly to
 * compare the json document against a target, using {@linkplain JSONCompare
 * JSON Assert}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Diego Berrueta
 * @author Camille Vienot
 * @author Stephane Nicoll
 * @since 6.2
 */
public class JsonContentAssert extends AbstractAssert<JsonContentAssert, CharSequence> {

	private final JsonLoader loader;

	/**
	 * Create a new {@link JsonContentAssert} instance that will load resources
	 * relative to the given {@code resourceLoadClass}, using the given
	 * {@code charset}.
	 * @param json the actual JSON content
	 * @param resourceLoadClass the source class used to load resources
	 * @param charset the charset of the JSON resources
	 */
	public JsonContentAssert(@Nullable CharSequence json, @Nullable Class<?> resourceLoadClass,
			@Nullable Charset charset) {

		super(json, JsonContentAssert.class);
		this.loader = new JsonLoader(resourceLoadClass, charset);
	}

	/**
	 * Create a new {@link JsonContentAssert} instance that will load resources
	 * relative to the given {@code resourceLoadClass}, using {@code UTF-8}.
	 * @param json the actual JSON content
	 * @param resourceLoadClass the source class used to load resources
	 */
	public JsonContentAssert(@Nullable CharSequence json, @Nullable Class<?> resourceLoadClass) {
		this(json, resourceLoadClass, null);
	}


	/**
	 * Verify that the actual value is equal to the given JSON. The
	 * {@code expected} value can contain the JSON itself or, if it ends with
	 * {@code .json}, the name of a resource to be loaded from the classpath.
	 * @param expected the expected JSON or the name of a resource containing
	 * the expected JSON
	 * @param compareMode the compare mode used when checking
	 */
	public JsonContentAssert isEqualTo(@Nullable CharSequence expected, JSONCompareMode compareMode) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotFailed(compare(expectedJson, compareMode));
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
	public JsonContentAssert isEqualTo(Resource expected, JSONCompareMode compareMode) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotFailed(compare(expectedJson, compareMode));
	}

	/**
	 * Verify that the actual value is equal to the given JSON. The
	 * {@code expected} value can contain the JSON itself or, if it ends with
	 * {@code .json}, the name of a resource to be loaded from the classpath.
	 * @param expected the expected JSON or the name of a resource containing
	 * the expected JSON
	 * @param comparator the comparator used when checking
	 */
	public JsonContentAssert isEqualTo(@Nullable CharSequence expected, JSONComparator comparator) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotFailed(compare(expectedJson, comparator));
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
	public JsonContentAssert isEqualTo(Resource expected, JSONComparator comparator) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotFailed(compare(expectedJson, comparator));
	}

	/**
	 * Verify that the actual value is {@link JSONCompareMode#LENIENT leniently}
	 * equal to the given JSON. The {@code expected} value can contain the JSON
	 * itself or, if it ends with {@code .json}, the name of a resource to be
	 * loaded from the classpath.
	 * @param expected the expected JSON or the name of a resource containing
	 * the expected JSON
	 */
	public JsonContentAssert isLenientlyEqualTo(@Nullable CharSequence expected) {
		return isEqualTo(expected, JSONCompareMode.LENIENT);
	}

	/**
	 * Verify that the actual value is {@link JSONCompareMode#LENIENT leniently}
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
	public JsonContentAssert isLenientlyEqualTo(Resource expected) {
		return isEqualTo(expected, JSONCompareMode.LENIENT);
	}

	/**
	 * Verify that the actual value is {@link JSONCompareMode#STRICT strictly}
	 * equal to the given JSON. The {@code expected} value can contain the JSON
	 * itself or, if it ends with {@code .json}, the name of a resource to be
	 * loaded from the classpath.
	 * @param expected the expected JSON or the name of a resource containing
	 * the expected JSON
	 */
	public JsonContentAssert isStrictlyEqualTo(@Nullable CharSequence expected) {
		return isEqualTo(expected, JSONCompareMode.STRICT);
	}

	/**
	 * Verify that the actual value is {@link JSONCompareMode#STRICT strictly}
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
	public JsonContentAssert isStrictlyEqualTo(Resource expected) {
		return isEqualTo(expected, JSONCompareMode.STRICT);
	}

	/**
	 * Verify that the actual value is not equal to the given JSON. The
	 * {@code expected} value can contain the JSON itself or, if it ends with
	 * {@code .json}, the name of a resource to be loaded from the classpath.
	 * @param expected the expected JSON or the name of a resource containing
	 * the expected JSON
	 * @param compareMode the compare mode used when checking
	 */
	public JsonContentAssert isNotEqualTo(@Nullable CharSequence expected, JSONCompareMode compareMode) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotPassed(compare(expectedJson, compareMode));
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
	public JsonContentAssert isNotEqualTo(Resource expected, JSONCompareMode compareMode) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotPassed(compare(expectedJson, compareMode));
	}

	/**
	 * Verify that the actual value is not equal to the given JSON. The
	 * {@code expected} value can contain the JSON itself or, if it ends with
	 * {@code .json}, the name of a resource to be loaded from the classpath.
	 * @param expected the expected JSON or the name of a resource containing
	 * the expected JSON
	 * @param comparator the comparator used when checking
	 */
	public JsonContentAssert isNotEqualTo(@Nullable CharSequence expected, JSONComparator comparator) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotPassed(compare(expectedJson, comparator));
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
	public JsonContentAssert isNotEqualTo(Resource expected, JSONComparator comparator) {
		String expectedJson = this.loader.getJson(expected);
		return assertNotPassed(compare(expectedJson, comparator));
	}

	/**
	 * Verify that the actual value is not {@link JSONCompareMode#LENIENT
	 * leniently} equal to the given JSON. The {@code expected} value can
	 * contain the JSON itself or, if it ends with {@code .json}, the name of a
	 * resource to be loaded from the classpath.
	 * @param expected the expected JSON or the name of a resource containing
	 * the expected JSON
	 */
	public JsonContentAssert isNotLenientlyEqualTo(@Nullable CharSequence expected) {
		return isNotEqualTo(expected, JSONCompareMode.LENIENT);
	}

	/**
	 * Verify that the actual value is not {@link JSONCompareMode#LENIENT
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
	public JsonContentAssert isNotLenientlyEqualTo(Resource expected) {
		return isNotEqualTo(expected, JSONCompareMode.LENIENT);
	}

	/**
	 * Verify that the actual value is not {@link JSONCompareMode#STRICT
	 * strictly} equal to the given JSON. The {@code expected} value can
	 * contain the JSON itself or, if it ends with {@code .json}, the name of a
	 * resource to be loaded from the classpath.
	 * @param expected the expected JSON or the name of a resource containing
	 * the expected JSON
	 */
	public JsonContentAssert isNotStrictlyEqualTo(@Nullable CharSequence expected) {
		return isNotEqualTo(expected, JSONCompareMode.STRICT);
	}

	/**
	 * Verify that the actual value is not {@link JSONCompareMode#STRICT
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
	public JsonContentAssert isNotStrictlyEqualTo(Resource expected) {
		return isNotEqualTo(expected, JSONCompareMode.STRICT);
	}


	private JSONCompareResult compare(@Nullable CharSequence expectedJson, JSONCompareMode compareMode) {
		return compare(this.actual, expectedJson, (actualJsonString, expectedJsonString) ->
				JSONCompare.compareJSON(expectedJsonString, actualJsonString, compareMode));
	}

	private JSONCompareResult compare(@Nullable CharSequence expectedJson, JSONComparator comparator) {
		return compare(this.actual, expectedJson, (actualJsonString, expectedJsonString) ->
				JSONCompare.compareJSON(expectedJsonString, actualJsonString, comparator));
	}

	private JSONCompareResult compare(@Nullable CharSequence actualJson, @Nullable CharSequence expectedJson,
			ThrowingBiFunction<String, String, JSONCompareResult> comparator) {

		if (actualJson == null) {
			return compareForNull(expectedJson);
		}
		if (expectedJson == null) {
			return compareForNull(actualJson.toString());
		}
		try {
			return comparator.applyWithException(actualJson.toString(), expectedJson.toString());
		}
		catch (Exception ex) {
			if (ex instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			throw new IllegalStateException(ex);
		}
	}

	private JSONCompareResult compareForNull(@Nullable CharSequence expectedJson) {
		JSONCompareResult result = new JSONCompareResult();
		result.passed();
		if (expectedJson != null) {
			result.fail("Expected null JSON");
		}
		return result;
	}

	private JsonContentAssert assertNotFailed(JSONCompareResult result) {
		if (result.failed()) {
			failWithMessage("JSON Comparison failure: %s", result.getMessage());
		}
		return this;
	}

	private JsonContentAssert assertNotPassed(JSONCompareResult result) {
		if (result.passed()) {
			failWithMessage("JSON Comparison failure: %s", result.getMessage());
		}
		return this;
	}

}
