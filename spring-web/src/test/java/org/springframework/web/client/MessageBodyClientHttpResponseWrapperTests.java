package org.springframework.web.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Unit tests for {MessageBodyClientHttpResponseWrapper}.
 *
 * @author Yin-Jui Liao
 */
public class MessageBodyClientHttpResponseWrapperTests {


	@Test
	public void testMessageBodyNotExist() throws IOException {

		ClientHttpResponse response = new ClientHttpResponse() {
			@Override
			public HttpStatus getStatusCode() throws IOException {
				return HttpStatus.NO_CONTENT;
			}

			@Override
			public int getRawStatusCode() throws IOException {
				return 204;
			}

			@Override
			public String getStatusText() throws IOException {
				return null;
			}

			@Override
			public void close() {

			}

			@Override
			public InputStream getBody() throws IOException {
				return null;
			}

			@Override
			public HttpHeaders getHeaders() {
				return null;
			}
		};
		MessageBodyClientHttpResponseWrapper responseWrapper = new MessageBodyClientHttpResponseWrapper(response);
		assertThat(responseWrapper.hasEmptyMessageBody()).isTrue();
	}

	@Test
	public void testMessageBodyExist() throws IOException {

		ClientHttpResponse response = new ClientHttpResponse() {
			@Override
			public HttpStatus getStatusCode() throws IOException {
				return HttpStatus.ACCEPTED;
			}

			@Override
			public int getRawStatusCode() throws IOException {
				return 202;
			}

			@Override
			public String getStatusText() throws IOException {
				return null;
			}

			@Override
			public void close() {

			}

			@Override
			public InputStream getBody() throws IOException {
				String body = "Accepted request";
				InputStream stream = new ByteArrayInputStream(body.getBytes());
				return stream;
			}

			@Override
			public HttpHeaders getHeaders() {
				return null;
			}
		};
		MessageBodyClientHttpResponseWrapper responseWrapper = new MessageBodyClientHttpResponseWrapper(response);
		assertThat(responseWrapper.hasEmptyMessageBody()).isFalse();
	}
}
