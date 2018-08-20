package org.springframework.web.client;

import java.net.URI;

import com.google.common.base.Strings;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.Assert.*;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.HttpStatus.*;

public class DefaultHttpErrorDetailsExtractorTests {

	private final DefaultHttpErrorDetailsExtractor extractor = new DefaultHttpErrorDetailsExtractor();

	@Test
	public void shouldGetSimpleExceptionMessage() {
		String actual = extractor.getErrorDetails(NOT_FOUND.value(), "Not Found", null, null, null, null);

		assertEquals("Should get a simple message", "404 Not Found", actual);
	}

	@Test
	public void shouldGetCompleteMessageWithoutBody() {
		String actual = extractor.getErrorDetails(NOT_FOUND.value(), "Not Found", null, null, URI.create("http://localhost:8080/my-endpoint"), GET);

		assertEquals("Should get a complete message without body", "404 Not Found after GET http://localhost:8080/my-endpoint : [no body]", actual);
	}

	@Test
	public void shouldGetCompleteMessageWithShortAsciiBodyNoCharset() {
		String responseBody = "my short response body";

		String actual = extractor.getErrorDetails(NOT_FOUND.value(), "Not Found", responseBody.getBytes(), null, URI.create("http://localhost:8080/my-endpoint"), GET);

		assertEquals("Should get a simple message", "404 Not Found after GET http://localhost:8080/my-endpoint : [my short response body]", actual);
	}

	@Test
	public void shouldGetCompleteMessageWithShortAsciiBodyUtfCharset() {
		String responseBody = "my short response body";

		String actual = extractor.getErrorDetails(NOT_FOUND.value(), "Not Found", responseBody.getBytes(), UTF_8, URI.create("http://localhost:8080/my-endpoint"), GET);

		assertEquals("Should get a simple message", "404 Not Found after GET http://localhost:8080/my-endpoint : [my short response body]", actual);
	}

	@Test
	public void shouldGetCompleteMessageWithShortUtfBodyUtfCharset() {
		String responseBody = "my short response body \u0105\u0119";

		String actual = extractor.getErrorDetails(NOT_FOUND.value(), "Not Found", responseBody.getBytes(), UTF_8, URI.create("http://localhost:8080/my-endpoint"), GET);

		assertEquals("Should get a simple message", "404 Not Found after GET http://localhost:8080/my-endpoint : [my short response body \u0105\u0119]", actual);
	}

	@Test
	public void shouldGetCompleteMessageWithShortUtfBodyNoCharset() {
		String responseBody = "my short response body \u0105\u0119";

		String actual = extractor.getErrorDetails(NOT_FOUND.value(), "Not Found", responseBody.getBytes(UTF_8), null, URI.create("http://localhost:8080/my-endpoint"), GET);

		assertEquals("Should get a simple message", "404 Not Found after GET http://localhost:8080/my-endpoint : [my short response body \u00c4\u0085\u00c4\u0099]", actual);
	}

	@Test
	public void shouldGetCompleteMessageWithLongAsciiBodyNoCharset() {
		String responseBody = Strings.repeat("asdfg", 100);
		String expectedMessage = "404 Not Found after GET http://localhost:8080/my-endpoint : [" + Strings.repeat("asdfg", 40) + "... (500 bytes)]";

		String actual = extractor.getErrorDetails(NOT_FOUND.value(), "Not Found", responseBody.getBytes(UTF_8), null, URI.create("http://localhost:8080/my-endpoint"), GET);

		assertEquals("Should get a simple message", expectedMessage, actual);
	}

	@Test
	public void shouldGetCompleteMessageWithLongUtfBodyNoCharset() {
		String responseBody = Strings.repeat("asd\u0105\u0119", 100);
		String expectedMessage = "404 Not Found after GET http://localhost:8080/my-endpoint : [" + Strings.repeat("asd\u00c4\u0085\u00c4\u0099", 28) + "asd\u00c4... (700 bytes)]";

		String actual = extractor.getErrorDetails(NOT_FOUND.value(), "Not Found", responseBody.getBytes(UTF_8), null, URI.create("http://localhost:8080/my-endpoint"), GET);

		assertEquals("Should get a simple message", expectedMessage, actual);
	}

	@Test
	public void shouldGetCompleteMessageWithLongUtfBodyUtfCharset() {
		String responseBody = Strings.repeat("asd\u0105\u0119", 100);
		String expectedMessage = "404 Not Found after GET http://localhost:8080/my-endpoint : [" + Strings.repeat("asd\u0105\u0119", 40) + "... (700 bytes)]";

		String actual = extractor.getErrorDetails(NOT_FOUND.value(), "Not Found", responseBody.getBytes(UTF_8), UTF_8, URI.create("http://localhost:8080/my-endpoint"), GET);

		assertEquals("Should get a simple message", expectedMessage, actual);
	}

}