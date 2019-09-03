/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.http;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link ContentDisposition}
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class ContentDispositionTests {

	private static DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;


	@Test
	public void parse() {
		assertThat(parse("form-data; name=\"foo\"; filename=\"foo.txt\"; size=123"))
				.isEqualTo(ContentDisposition.builder("form-data")
						.name("foo")
						.filename("foo.txt")
						.size(123L)
						.build());
	}

	@Test
	public void parseFilenameUnquoted() {
		assertThat(parse("form-data; filename=unquoted"))
				.isEqualTo(ContentDisposition.builder("form-data")
						.filename("unquoted")
						.build());
	}

	@Test  // SPR-16091
	public void parseFilenameWithSemicolon() {
		assertThat(parse("attachment; filename=\"filename with ; semicolon.txt\""))
				.isEqualTo(ContentDisposition.builder("attachment")
						.filename("filename with ; semicolon.txt")
						.build());
	}

	@Test
	public void parseEncodedFilename() {
		assertThat(parse("form-data; name=\"name\"; filename*=UTF-8''%E4%B8%AD%E6%96%87.txt"))
				.isEqualTo(ContentDisposition.builder("form-data")
						.name("name")
						.filename("中文.txt", StandardCharsets.UTF_8)
						.build());
	}

	@Test // gh-23077
	public void parseWithEscapedQuote() {
		assertThat(parse("form-data; name=\"file\"; filename=\"\\\"The Twilight Zone\\\".txt\"; size=123"))
				.isEqualTo(ContentDisposition.builder("form-data")
						.name("file")
						.filename("\\\"The Twilight Zone\\\".txt")
						.size(123L)
						.build());
	}

	@Test
	public void parseWithExtraSemicolons() {
		assertThat(parse("form-data; name=\"foo\";; ; filename=\"foo.txt\"; size=123"))
				.isEqualTo(ContentDisposition.builder("form-data")
						.name("foo")
						.filename("foo.txt")
						.size(123L)
						.build());
	}

	@Test
	public void parseDates() {
		ZonedDateTime creationTime = ZonedDateTime.parse("Mon, 12 Feb 2007 10:15:30 -0500", formatter);
		ZonedDateTime modificationTime = ZonedDateTime.parse("Tue, 13 Feb 2007 10:15:30 -0500", formatter);
		ZonedDateTime readTime = ZonedDateTime.parse("Wed, 14 Feb 2007 10:15:30 -0500", formatter);

		assertThat(
				parse("attachment; " +
						"creation-date=\"" + creationTime.format(formatter) + "\"; " +
						"modification-date=\"" + modificationTime.format(formatter) + "\"; " +
						"read-date=\"" + readTime.format(formatter) + "\"")).isEqualTo(
				ContentDisposition.builder("attachment")
						.creationDate(creationTime)
						.modificationDate(modificationTime)
						.readDate(readTime)
						.build());
	}

	@Test
	public void parseIgnoresInvalidDates() {
		ZonedDateTime readTime = ZonedDateTime.parse("Wed, 14 Feb 2007 10:15:30 -0500", formatter);

		assertThat(
				parse("attachment; " +
						"creation-date=\"-1\"; " +
						"modification-date=\"-1\"; " +
						"read-date=\"" + readTime.format(formatter) + "\"")).isEqualTo(
				ContentDisposition.builder("attachment")
						.readDate(readTime)
						.build());
	}

	@Test
	public void parseEmpty() {
		assertThatIllegalArgumentException().isThrownBy(() -> parse(""));
	}

	@Test
	public void parseNoType() {
		assertThatIllegalArgumentException().isThrownBy(() -> parse(";"));
	}

	@Test
	public void parseInvalidParameter() {
		assertThatIllegalArgumentException().isThrownBy(() -> parse("foo;bar"));
	}

	private static ContentDisposition parse(String input) {
		return ContentDisposition.parse(input);
	}


	@Test
	public void headerValue() {
		assertThat(
				ContentDisposition.builder("form-data")
						.name("foo")
						.filename("foo.txt")
						.size(123L)
						.build().toString())
				.isEqualTo("form-data; name=\"foo\"; filename=\"foo.txt\"; size=123");
	}

	@Test
	public void headerValueWithEncodedFilename() {
		assertThat(
				ContentDisposition.builder("form-data")
						.name("name")
						.filename("中文.txt", StandardCharsets.UTF_8)
						.build().toString())
				.isEqualTo("form-data; name=\"name\"; filename*=UTF-8''%E4%B8%AD%E6%96%87.txt");
	}

	@Test  // SPR-14547
	public void encodeHeaderFieldParam() {
		Method encode = ReflectionUtils.findMethod(ContentDisposition.class,
				"encodeHeaderFieldParam", String.class, Charset.class);
		ReflectionUtils.makeAccessible(encode);

		String result = (String)ReflectionUtils.invokeMethod(encode, null, "test.txt",
				StandardCharsets.US_ASCII);
		assertThat(result).isEqualTo("test.txt");

		result = (String)ReflectionUtils.invokeMethod(encode, null, "中文.txt", StandardCharsets.UTF_8);
		assertThat(result).isEqualTo("UTF-8''%E4%B8%AD%E6%96%87.txt");
	}

	@Test
	public void encodeHeaderFieldParamInvalidCharset() {
		Method encode = ReflectionUtils.findMethod(ContentDisposition.class,
				"encodeHeaderFieldParam", String.class, Charset.class);
		ReflectionUtils.makeAccessible(encode);
		assertThatIllegalArgumentException().isThrownBy(() ->
				ReflectionUtils.invokeMethod(encode, null, "test", StandardCharsets.UTF_16));
	}

	@Test  // SPR-14408
	public void decodeHeaderFieldParam() {
		Method decode = ReflectionUtils.findMethod(ContentDisposition.class,
				"decodeHeaderFieldParam", String.class);
		ReflectionUtils.makeAccessible(decode);

		String result = (String)ReflectionUtils.invokeMethod(decode, null, "test.txt");
		assertThat(result).isEqualTo("test.txt");

		result = (String)ReflectionUtils.invokeMethod(decode, null, "UTF-8''%E4%B8%AD%E6%96%87.txt");
		assertThat(result).isEqualTo("中文.txt");
	}

	@Test
	public void decodeHeaderFieldParamInvalidCharset() {
		Method decode = ReflectionUtils.findMethod(ContentDisposition.class,
				"decodeHeaderFieldParam", String.class);
		ReflectionUtils.makeAccessible(decode);
		assertThatIllegalArgumentException().isThrownBy(() ->
				ReflectionUtils.invokeMethod(decode, null, "UTF-16''test"));
	}

	@Test
	public void decodeHeaderFieldParamShortInvalidEncodedFilename() {
		Method decode = ReflectionUtils.findMethod(ContentDisposition.class,
				"decodeHeaderFieldParam", String.class);
		ReflectionUtils.makeAccessible(decode);
		assertThatIllegalArgumentException().isThrownBy(() ->
				ReflectionUtils.invokeMethod(decode, null, "UTF-8''%A"));
	}

	@Test
	public void decodeHeaderFieldParamLongerInvalidEncodedFilename() {
		Method decode = ReflectionUtils.findMethod(ContentDisposition.class,
				"decodeHeaderFieldParam", String.class);
		ReflectionUtils.makeAccessible(decode);
		assertThatIllegalArgumentException().isThrownBy(() ->
				ReflectionUtils.invokeMethod(decode, null, "UTF-8''%A.txt"));
	}

}
