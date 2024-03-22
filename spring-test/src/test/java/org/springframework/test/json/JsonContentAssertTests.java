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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.assertj.core.api.AssertProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.DefaultComparator;
import org.skyscreamer.jsonassert.comparator.JSONComparator;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link JsonContentAssert}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
@TestInstance(Lifecycle.PER_CLASS)
class JsonContentAssertTests {

	private static final String SOURCE = loadJson("source.json");

	private static final String LENIENT_SAME = loadJson("lenient-same.json");

	private static final String DIFFERENT = loadJson("different.json");

	private static final JSONComparator COMPARATOR = new DefaultComparator(JSONCompareMode.LENIENT);

	@Test
	void isEqualToWhenStringIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isEqualTo(SOURCE);
	}

	@Test
	void isEqualToWhenNullActualShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertThat(forJson(null)).isEqualTo(SOURCE));
	}

	@Test
	void isEqualToWhenExpectedIsNotAStringShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualTo(SOURCE.getBytes()));
	}

	@Test
	void isEqualToWhenExpectedIsNullShouldFail() {
		CharSequence actual = null;
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualTo(actual, JSONCompareMode.LENIENT));
	}

	@Test
	void isEqualToWhenStringIsMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isEqualTo(LENIENT_SAME, JSONCompareMode.LENIENT);
	}

	@Test
	void isEqualToWhenStringIsNotMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualTo(DIFFERENT, JSONCompareMode.LENIENT));
	}

	@Test
	void isEqualToWhenResourcePathIsMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isEqualTo("lenient-same.json", JSONCompareMode.LENIENT);
	}

	@Test
	void isEqualToWhenResourcePathIsNotMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualTo("different.json", JSONCompareMode.LENIENT));
	}

	Stream<Arguments> source() {
		return Stream.of(
				Arguments.of(new ClassPathResource("source.json", JsonContentAssertTests.class)),
				Arguments.of(new ByteArrayResource(SOURCE.getBytes())),
				Arguments.of(new FileSystemResource(createFile(SOURCE))),
				Arguments.of(new InputStreamResource(createInputStream(SOURCE))));
	}

	Stream<Arguments> lenientSame() {
		return Stream.of(
				Arguments.of(new ClassPathResource("lenient-same.json", JsonContentAssertTests.class)),
				Arguments.of(new ByteArrayResource(LENIENT_SAME.getBytes())),
				Arguments.of(new FileSystemResource(createFile(LENIENT_SAME))),
				Arguments.of(new InputStreamResource(createInputStream(LENIENT_SAME))));
	}

	Stream<Arguments> different() {
		return Stream.of(
				Arguments.of(new ClassPathResource("different.json", JsonContentAssertTests.class)),
				Arguments.of(new ByteArrayResource(DIFFERENT.getBytes())),
				Arguments.of(new FileSystemResource(createFile(DIFFERENT))),
				Arguments.of(new InputStreamResource(createInputStream(DIFFERENT))));
	}

	@ParameterizedTest
	@MethodSource("lenientSame")
	void isEqualToWhenResourceIsMatchingAndLenientSameShouldPass(Resource expected) {
		assertThat(forJson(SOURCE)).isEqualTo(expected, JSONCompareMode.LENIENT);
	}

	@ParameterizedTest
	@MethodSource("different")
	void isEqualToWhenResourceIsNotMatchingAndLenientShouldFail(Resource expected) {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isEqualTo(expected, JSONCompareMode.LENIENT));
	}


	@Test
	void isEqualToWhenStringIsMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isEqualTo(LENIENT_SAME, COMPARATOR);
	}

	@Test
	void isEqualToWhenStringIsNotMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualTo(DIFFERENT, COMPARATOR));
	}

	@Test
	void isEqualToWhenResourcePathIsMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isEqualTo("lenient-same.json", COMPARATOR);
	}

	@Test
	void isEqualToWhenResourcePathIsNotMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualTo("different.json", COMPARATOR));
	}

	@ParameterizedTest
	@MethodSource("lenientSame")
	void isEqualToWhenResourceIsMatchingAndComparatorShouldPass(Resource expected) {
		assertThat(forJson(SOURCE)).isEqualTo(expected, COMPARATOR);
	}

	@ParameterizedTest
	@MethodSource("different")
	void isEqualToWhenResourceIsNotMatchingAndComparatorShouldFail(Resource expected) {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isEqualTo(expected, COMPARATOR));
	}

	@Test
	void isLenientlyEqualToWhenStringIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isLenientlyEqualTo(LENIENT_SAME);
	}

	@Test
	void isLenientlyEqualToWhenNullActualShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(null)).isLenientlyEqualTo(SOURCE));
	}

	@Test
	void isLenientlyEqualToWhenStringIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isLenientlyEqualTo(DIFFERENT));
	}

	@Test
	void isLenientlyEqualToWhenExpectedDoesNotExistShouldFail() {
		assertThatIllegalStateException()
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isLenientlyEqualTo("does-not-exist.json"))
				.withMessage("Unable to load JSON from class path resource [org/springframework/test/json/does-not-exist.json]");
	}

	@Test
	void isLenientlyEqualToWhenResourcePathIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isLenientlyEqualTo("lenient-same.json");
	}

	@Test
	void isLenientlyEqualToWhenResourcePathIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isLenientlyEqualTo("different.json"));
	}

	@ParameterizedTest
	@MethodSource("lenientSame")
	void isLenientlyEqualToWhenResourceIsMatchingShouldPass(Resource expected) {
		assertThat(forJson(SOURCE)).isLenientlyEqualTo(expected);
	}

	@ParameterizedTest
	@MethodSource("different")
	void isLenientlyEqualToWhenResourceIsNotMatchingShouldFail(Resource expected) {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isLenientlyEqualTo(expected));
	}

	@Test
	void isStrictlyEqualToWhenStringIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isStrictlyEqualTo(SOURCE);
	}

	@Test
	void isStrictlyEqualToWhenStringIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isStrictlyEqualTo(LENIENT_SAME));
	}

	@Test
	void isStrictlyEqualToWhenResourcePathIsMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isStrictlyEqualTo("source.json");
	}

	@Test
	void isStrictlyEqualToWhenResourcePathIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isStrictlyEqualTo("lenient-same.json"));
	}

	@ParameterizedTest
	@MethodSource("source")
	void isStrictlyEqualToWhenResourceIsMatchingShouldPass(Resource expected) {
		assertThat(forJson(SOURCE)).isStrictlyEqualTo(expected);
	}

	@ParameterizedTest
	@MethodSource("lenientSame")
	void isStrictlyEqualToWhenResourceIsNotMatchingShouldFail(Resource expected) {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isStrictlyEqualTo(expected));
	}


	@Test
	void isNotEqualToWhenStringIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualTo(SOURCE));
	}

	@Test
	void isNotEqualToWhenNullActualShouldPass() {
		assertThat(forJson(null)).isNotEqualTo(SOURCE);
	}

	@Test
	void isNotEqualToWhenStringIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualTo(DIFFERENT);
	}

	@Test
	void isNotEqualToAsObjectWhenExpectedIsNotAStringShouldNotFail() {
		assertThat(forJson(SOURCE)).isNotEqualTo(SOURCE.getBytes());
	}

	@Test
	void isNotEqualToWhenStringIsMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualTo(LENIENT_SAME, JSONCompareMode.LENIENT));
	}

	@Test
	void isNotEqualToWhenStringIsNotMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualTo(DIFFERENT, JSONCompareMode.LENIENT);
	}

	@Test
	void isNotEqualToWhenResourcePathIsMatchingAndLenientShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isNotEqualTo("lenient-same.json", JSONCompareMode.LENIENT));
	}

	@Test
	void isNotEqualToWhenResourcePathIsNotMatchingAndLenientShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualTo("different.json", JSONCompareMode.LENIENT);
	}

	@ParameterizedTest
	@MethodSource("lenientSame")
	void isNotEqualToWhenResourceIsMatchingAndLenientShouldFail(Resource expected) {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertThat(forJson(SOURCE))
				.isNotEqualTo(expected, JSONCompareMode.LENIENT));
	}

	@ParameterizedTest
	@MethodSource("different")
	void isNotEqualToWhenResourceIsNotMatchingAndLenientShouldPass(Resource expected) {
		assertThat(forJson(SOURCE)).isNotEqualTo(expected, JSONCompareMode.LENIENT);
	}

	@Test
	void isNotEqualToWhenStringIsMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualTo(LENIENT_SAME, COMPARATOR));
	}

	@Test
	void isNotEqualToWhenStringIsNotMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualTo(DIFFERENT, COMPARATOR);
	}

	@Test
	void isNotEqualToWhenResourcePathIsMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualTo("lenient-same.json", COMPARATOR));
	}

	@Test
	void isNotEqualToWhenResourcePathIsNotMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualTo("different.json", COMPARATOR);
	}

	@ParameterizedTest
	@MethodSource("lenientSame")
	void isNotEqualToWhenResourceIsMatchingAndComparatorShouldFail(Resource expected) {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forJson(SOURCE)).isNotEqualTo(expected, COMPARATOR));
	}

	@ParameterizedTest
	@MethodSource("different")
	void isNotEqualToWhenResourceIsNotMatchingAndComparatorShouldPass(Resource expected) {
		assertThat(forJson(SOURCE)).isNotEqualTo(expected, COMPARATOR);
	}

	@Test
	void isNotEqualToWhenResourceIsMatchingAndComparatorShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotEqualTo(createResource(LENIENT_SAME), COMPARATOR));
	}

	@Test
	void isNotEqualToWhenResourceIsNotMatchingAndComparatorShouldPass() {
		assertThat(forJson(SOURCE)).isNotEqualTo(createResource(DIFFERENT), COMPARATOR);
	}

	@Test
	void isNotLenientlyEqualToWhenNullActualShouldPass() {
		assertThat(forJson(null)).isNotLenientlyEqualTo(SOURCE);
	}

	@Test
	void isNotLenientlyEqualToWhenStringIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotLenientlyEqualTo(DIFFERENT);
	}

	@Test
	void isNotLenientlyEqualToWhenResourcePathIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotLenientlyEqualTo("lenient-same.json"));
	}

	@Test
	void isNotLenientlyEqualToWhenResourcePathIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotLenientlyEqualTo("different.json");
	}

	@ParameterizedTest
	@MethodSource("lenientSame")
	void isNotLenientlyEqualToWhenResourceIsMatchingShouldFail(Resource expected) {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotLenientlyEqualTo(expected));
	}

	@ParameterizedTest
	@MethodSource("different")
	void isNotLenientlyEqualToWhenResourceIsNotMatchingShouldPass(Resource expected) {
		assertThat(forJson(SOURCE)).isNotLenientlyEqualTo(expected);
	}

	@Test
	void isNotStrictlyEqualToWhenStringIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotStrictlyEqualTo(SOURCE));
	}

	@Test
	void isNotStrictlyEqualToWhenStringIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualTo(LENIENT_SAME);
	}

	@Test
	void isNotStrictlyEqualToWhenResourcePathIsMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotStrictlyEqualTo("source.json"));
	}

	@Test
	void isNotStrictlyEqualToWhenResourcePathIsNotMatchingShouldPass() {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualTo("lenient-same.json");
	}

	@ParameterizedTest
	@MethodSource("source")
	void isNotStrictlyEqualToWhenResourceIsMatchingShouldFail(Resource expected) {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forJson(SOURCE)).isNotStrictlyEqualTo(expected));
	}

	@ParameterizedTest
	@MethodSource("lenientSame")
	void isNotStrictlyEqualToWhenResourceIsNotMatchingShouldPass(Resource expected) {
		assertThat(forJson(SOURCE)).isNotStrictlyEqualTo(expected);
	}

	@Test
	void isNullWhenActualIsNullShouldPass() {
		assertThat(forJson(null)).isNull();
	}

	private Path createFile(String content) {
		try {
			Path temp = Files.createTempFile("file", ".json");
			Files.writeString(temp, content);
			return temp;
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private InputStream createInputStream(String content) {
		return new ByteArrayInputStream(content.getBytes());
	}

	private Resource createResource(String content) {
		return new ByteArrayResource(content.getBytes());
	}

	private static String loadJson(String path) {
		try {
			ClassPathResource resource = new ClassPathResource(path, JsonContentAssertTests.class);
			return new String(FileCopyUtils.copyToByteArray(resource.getInputStream()));
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}

	}

	private AssertProvider<JsonContentAssert> forJson(@Nullable String json) {
		return () -> new JsonContentAssert(json, JsonContentAssertTests.class);
	}

}
