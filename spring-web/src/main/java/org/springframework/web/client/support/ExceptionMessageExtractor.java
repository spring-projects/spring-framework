package org.springframework.web.client.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

public class ExceptionMessageExtractor {

	private static final int MAX_BODY_BYTES_LENGTH = 400;

	private static final int MAX_BODY_CHARS_LENGTH = 200;

	@NotNull
	public static String getExceptionMessage(HttpStatus statusCode, String statusText, @Nullable byte[] responseBody,
			@Nullable Charset responseCharset, @Nullable URI url, @Nullable HttpMethod method) {
		if (url == null || method == null) {
			return getSimpleExceptionMessage(statusCode, statusText);
		}

		return getDetailedExceptionMessage(statusCode, statusText, responseBody, responseCharset, url, method);
	}

	@NotNull
	private static String getDetailedExceptionMessage(HttpStatus statusCode, String statusText, @Nullable byte[] responseBody, @Nullable Charset responseCharset, @Nullable URI url, @Nullable HttpMethod method) {
		StringBuilder result = new StringBuilder();

		result.append(getSimpleExceptionMessage(statusCode, statusText))
				.append(" after ")
				.append(method)
				.append(" ")
				.append(url)
				.append(" : ");

		if (responseBody == null || responseBody.length == 0) {
			result.append("[no body]");
		}
		else {
			result
					.append("[")
					.append(getResponseBody(responseBody, responseCharset))
					.append("]");
		}

		return result.toString();
	}

	@NotNull
	private static String getSimpleExceptionMessage(HttpStatus statusCode, String statusText) {
		return statusCode.value() + " " + statusText;
	}

	private static String getResponseBody(byte[] responseBody, @Nullable Charset responseCharset) {
		Charset charset = getCharsetOrDefault(responseCharset);
		if (responseBody.length < MAX_BODY_BYTES_LENGTH) {
			return getCompleteResponseBody(responseBody, charset);
		}
		return getResponseBodyPreview(responseBody, charset);
	}

	@NotNull
	private static String getCompleteResponseBody(byte[] responseBody, Charset responseCharset) {
		return new String(responseBody, responseCharset);
	}

	private static String getResponseBodyPreview(byte[] responseBody, Charset responseCharset) {
		try {
			String bodyPreview = readBodyAsString(responseBody, responseCharset);
			return bodyPreview + "... (" + responseBody.length + " bytes)";
		}
		catch (IOException e) {
			// should never happen
			throw new IllegalStateException(e);
		}
	}

	@NotNull
	private static String readBodyAsString(byte[] responseBody, Charset responseCharset) throws IOException {
		Reader reader = new InputStreamReader(new ByteArrayInputStream(responseBody), responseCharset);
		CharBuffer result = CharBuffer.allocate(MAX_BODY_CHARS_LENGTH);

		reader.read(result);
		reader.close();
		result.flip();

		return result.toString();
	}

	private static Charset getCharsetOrDefault(@Nullable Charset responseCharset) {
		if (responseCharset == null) {
			return StandardCharsets.ISO_8859_1;
		}
		return responseCharset;
	}

}
