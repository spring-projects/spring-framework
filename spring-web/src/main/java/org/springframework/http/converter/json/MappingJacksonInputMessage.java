/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.converter.json;

import java.io.IOException;
import java.io.InputStream;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;

/**
 * {@link HttpInputMessage} that can eventually stores a Jackson view that will be used
 * to deserialize the message.
 *
 * @author Sebastien Deleuze
 * @since 4.2
 * @deprecated since 7.0 in favor of using {@link org.springframework.http.converter.SmartHttpMessageConverter} hints
 */
@Deprecated(since = "7.0", forRemoval = true)
public class MappingJacksonInputMessage implements HttpInputMessage {

	private final InputStream body;

	private final HttpHeaders headers;

	private @Nullable Class<?> deserializationView;


	public MappingJacksonInputMessage(InputStream body, HttpHeaders headers) {
		this.body = body;
		this.headers = headers;
	}

	public MappingJacksonInputMessage(InputStream body, HttpHeaders headers, Class<?> deserializationView) {
		this(body, headers);
		this.deserializationView = deserializationView;
	}


	@Override
	public InputStream getBody() throws IOException {
		return this.body;
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	public void setDeserializationView(@Nullable Class<?> deserializationView) {
		this.deserializationView = deserializationView;
	}

	public @Nullable Class<?> getDeserializationView() {
		return this.deserializationView;
	}

}
