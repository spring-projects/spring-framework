/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client;

import java.io.IOException;
import java.io.OutputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.util.StreamUtils;

/**
 * Base implementation of {@link ClientHttpRequest} that buffers output
 * in a byte array before sending it over the wire.
 *
 * @author Arjen Poutsma
 * @since 3.0.6
 */
public abstract class AbstractBufferingClientHttpRequest extends AbstractClientHttpRequest {

	private final FastByteArrayOutputStream bufferedOutput = new FastByteArrayOutputStream(1024);


	@Override
	protected final OutputStream getBodyInternal(HttpHeaders headers) {
		return this.bufferedOutput;
	}

	@Override
	protected final ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
		byte[] bytes = this.bufferedOutput.toByteArrayUnsafe();
		if (headers.getContentLength() < 0) {
			headers.setContentLength(bytes.length);
		}
		ClientHttpResponse result = executeInternal(headers, bytes);
		this.bufferedOutput.reset();
		return result;
	}

	/**
	 * Abstract template method that writes the given headers and content to the HTTP request.
	 * @param headers the HTTP headers
	 * @param bufferedOutput the body content
	 * @return the response object for the executed request
	 */
	protected abstract ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput)
			throws IOException;

	/**
	 * Execute with the given request and body.
	 * @param request the request to execute with
	 * @param bufferedOutput the body to write
	 * @param bufferResponse whether to buffer the response
	 * @return the resulting response
	 * @throws IOException in case of I/O errors from execution
	 * @since 7.0
	 */
	final ClientHttpResponse executeWithRequestAndBody(
			ClientHttpRequest request, byte[] bufferedOutput, boolean bufferResponse) throws IOException {

		if (bufferedOutput.length > 0) {
			long contentLength = request.getHeaders().getContentLength();
			if (contentLength > -1 && contentLength != bufferedOutput.length) {
				request.getHeaders().setContentLength(bufferedOutput.length);
			}
			if (request instanceof StreamingHttpOutputMessage streamingOutputMessage) {
				streamingOutputMessage.setBody(bufferedOutput);
			}
			else {
				StreamUtils.copy(bufferedOutput, request.getBody());
			}
		}

		ClientHttpResponse response = request.execute();
		return (bufferResponse ? new BufferingClientHttpResponseWrapper(response) : response);
	}

}
