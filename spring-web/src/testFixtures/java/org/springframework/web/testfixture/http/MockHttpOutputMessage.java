/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.testfixture.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.util.StreamUtils;

/**
 * Mock implementation of {@link HttpOutputMessage}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class MockHttpOutputMessage implements HttpOutputMessage {

	private final HttpHeaders headers = new HttpHeaders();

	private final ByteArrayOutputStream body = new ByteArrayOutputStream(1024);


	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public OutputStream getBody() throws IOException {
		return this.body;
	}

	/**
	 * Return the body content as a byte array.
	 */
	public byte[] getBodyAsBytes() {
		return this.body.toByteArray();
	}

	/**
	 * Return the body content interpreted as a UTF-8 string.
	 */
	public String getBodyAsString() {
		return getBodyAsString(StandardCharsets.UTF_8);
	}

	/**
	 * Return the body content interpreted as a string using the supplied character set.
	 * @param charset the charset to use to turn the body content into a String
	 */
	public String getBodyAsString(Charset charset) {
		return StreamUtils.copyToString(this.body, charset);
	}

}
