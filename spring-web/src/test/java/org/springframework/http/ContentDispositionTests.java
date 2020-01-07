/*
 * Copyright 2002-2020 the original author or authors.
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

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.springframework.http.ContentDisposition.builder;

/**
 * Unit tests for {@link ContentDisposition}
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class ContentDispositionTests {

	private static DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;


	@Test
	public void parse() {
		assertEquals(builder("form-data").name("foo").filename("foo.txt").size(123L).build(),
				parse("form-data; name=\"foo\"; filename=\"foo.txt\"; size=123"));
	}

	@Test
	public void parseFilenameUnquoted() {
		assertEquals(builder("form-data").filename("unquoted").build(),
				parse("form-data; filename=unquoted"));
	}

	@Test  // SPR-16091
	public void parseFilenameWithSemicolon() {
		assertEquals(builder("attachment").filename("filename with ; semicolon.txt").build(),
				parse("attachment; filename=\"filename with ; semicolon.txt\""));
	}

	@Test
	public void parseEncodedFilename() {
		assertEquals(builder("form-data").name("name").filename("中文.txt", StandardCharsets.UTF_8).build(),
				parse("form-data; name=\"name\"; filename*=UTF-8''%E4%B8%AD%E6%96%87.txt"));
	}

	@Test // gh-24112
	public void parseEncodedFilenameWithPaddedCharset() {
		assertEquals(builder("attachment").filename("some-file.zip", StandardCharsets.UTF_8).build(),
				parse("attachment; filename*= UTF-8''some-file.zip"));
	}

	@Test
	public void parseEncodedFilenameWithoutCharset() {
		assertEquals(builder("form-data").name("name").filename("test.txt").build(),
				parse("form-data; name=\"name\"; filename*=test.txt"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void parseEncodedFilenameWithInvalidCharset() {
		parse("form-data; name=\"name\"; filename*=UTF-16''test.txt");
	}

	@Test
	public void parseEncodedFilenameWithInvalidName() {

		Consumer<String> tester = input -> {
			try {
				parse(input);
				fail();
			}
			catch (IllegalArgumentException ex) {
				// expected
			}
		};

		tester.accept("form-data; name=\"name\"; filename*=UTF-8''%A");
		tester.accept("form-data; name=\"name\"; filename*=UTF-8''%A.txt");
	}

	@Test // gh-23077
	public void parseWithEscapedQuote() {

		BiConsumer<String, String> tester = (description, filename) ->
				assertEquals(description,
						builder("form-data").name("file").filename(filename).size(123L).build(),
						parse("form-data; name=\"file\"; filename=\"" + filename + "\"; size=123"));

		tester.accept("Escaped quotes should be ignored",
				"\\\"The Twilight Zone\\\".txt");

		tester.accept("Escaped quotes preceded by escaped backslashes should be ignored",
				"\\\\\\\"The Twilight Zone\\\\\\\".txt");

		tester.accept("Escaped backslashes should not suppress quote",
				"The Twilight Zone \\\\");

		tester.accept("Escaped backslashes should not suppress quote",
				"The Twilight Zone \\\\\\\\");
	}

	@Test
	public void parseWithExtraSemicolons() {
		assertEquals(builder("form-data").name("foo").filename("foo.txt").size(123L).build(),
				parse("form-data; name=\"foo\";; ; filename=\"foo.txt\"; size=123"));
	}

	@Test
	public void parseDates() {
		assertEquals(
				builder("attachment")
						.creationDate(ZonedDateTime.parse("Mon, 12 Feb 2007 10:15:30 -0500", formatter))
						.modificationDate(ZonedDateTime.parse("Tue, 13 Feb 2007 10:15:30 -0500", formatter))
						.readDate(ZonedDateTime.parse("Wed, 14 Feb 2007 10:15:30 -0500", formatter)).build(),
				parse("attachment; creation-date=\"Mon, 12 Feb 2007 10:15:30 -0500\"; " +
						"modification-date=\"Tue, 13 Feb 2007 10:15:30 -0500\"; " +
						"read-date=\"Wed, 14 Feb 2007 10:15:30 -0500\""));
	}

	@Test
	public void parseIgnoresInvalidDates() {
		assertEquals(
				builder("attachment")
						.readDate(ZonedDateTime.parse("Wed, 14 Feb 2007 10:15:30 -0500", formatter))
						.build(),
				parse("attachment; creation-date=\"-1\"; " +
						"modification-date=\"-1\"; " +
						"read-date=\"Wed, 14 Feb 2007 10:15:30 -0500\""));
	}

	@Test(expected = IllegalArgumentException.class)
	public void parseEmpty() {
		parse("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void parseNoType() {
		parse(";");
	}

	@Test(expected = IllegalArgumentException.class)
	public void parseInvalidParameter() {
		parse("foo;bar");
	}

	private static ContentDisposition parse(String input) {
		return ContentDisposition.parse(input);
	}


	@Test
	public void format() {
		assertEquals("form-data; name=\"foo\"; filename=\"foo.txt\"; size=123",
				builder("form-data").name("foo").filename("foo.txt").size(123L).build().toString());
	}

	@Test
	public void formatWithEncodedFilename() {
		assertEquals("form-data; name=\"name\"; filename*=UTF-8''%E4%B8%AD%E6%96%87.txt",
				builder("form-data").name("name").filename("中文.txt", StandardCharsets.UTF_8).build().toString());
	}

	@Test
	public void formatWithEncodedFilenameUsingUsAscii() {
		assertEquals("form-data; name=\"name\"; filename=\"test.txt\"",
				builder("form-data")
						.name("name")
						.filename("test.txt", StandardCharsets.US_ASCII)
						.build()
						.toString());
	}

	@Test // gh-24220
	public void formatWithFilenameWithQuotes() {

		BiConsumer<String, String> tester = (input, output) -> {

			assertEquals("form-data; filename=\"" + output + "\"",
					builder("form-data").filename(input).build().toString());

			assertEquals("form-data; filename=\"" + output + "\"",
					builder("form-data").filename(input, StandardCharsets.US_ASCII).build().toString());
		};

		String filename = "\"foo.txt";
		tester.accept(filename, "\\" + filename);

		filename = "\\\"foo.txt";
		tester.accept(filename, filename);

		filename = "\\\\\"foo.txt";
		tester.accept(filename, "\\" + filename);

		filename = "\\\\\\\"foo.txt";
		tester.accept(filename, filename);

		filename = "\\\\\\\\\"foo.txt";
		tester.accept(filename, "\\" + filename);

		tester.accept("\"\"foo.txt", "\\\"\\\"foo.txt");
		tester.accept("\"\"\"foo.txt", "\\\"\\\"\\\"foo.txt");

		tester.accept("foo.txt\\", "foo.txt");
		tester.accept("foo.txt\\\\", "foo.txt\\\\");
		tester.accept("foo.txt\\\\\\", "foo.txt\\\\");
	}

	@Test(expected = IllegalArgumentException.class)
	public void formatWithEncodedFilenameUsingInvalidCharset() {
		builder("form-data").name("name").filename("test.txt", StandardCharsets.UTF_16).build().toString();
	}

}
