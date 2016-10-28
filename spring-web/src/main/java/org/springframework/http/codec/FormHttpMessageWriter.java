/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.codec;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * Implementation of an {@link HttpMessageWriter} to write HTML form data, i.e.
 * response body with media type {@code "application/x-www-form-urlencoded"}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class FormHttpMessageWriter implements HttpMessageWriter<MultiValueMap<String, String>> {

	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private static final ResolvableType MULTIVALUE_TYPE =
			ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class);


	private Charset defaultCharset = DEFAULT_CHARSET;


	/**
	 * Set the default character set to use for writing form data when the response
	 * Content-Type header does not explicitly specify it.
	 * <p>By default this is set to "UTF-8".
	 */
	public void setDefaultCharset(Charset charset) {
		Assert.notNull(charset, "'charset' must not be null");
		this.defaultCharset = charset;
	}

	/**
	 * Return the configured default charset.
	 */
	public Charset getDefaultCharset() {
		return this.defaultCharset;
	}


	@Override
	public boolean canWrite(ResolvableType elementType, MediaType mediaType) {
		return MULTIVALUE_TYPE.isAssignableFrom(elementType) &&
				(mediaType == null || MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(mediaType));
	}

	@Override
	public Mono<Void> write(Publisher<? extends MultiValueMap<String, String>> inputStream,
			ResolvableType elementType, MediaType mediaType, ReactiveHttpOutputMessage outputMessage,
			Map<String, Object> hints) {

		MediaType contentType = outputMessage.getHeaders().getContentType();
		if (contentType == null) {
			contentType = MediaType.APPLICATION_FORM_URLENCODED;
			outputMessage.getHeaders().setContentType(contentType);
		}

		Charset charset = getMediaTypeCharset(contentType);

		return Flux
				.from(inputStream)
				.single()
				.map(form -> generateForm(form, charset))
				.then(value -> {
					ByteBuffer byteBuffer = charset.encode(value);
					DataBuffer buffer = outputMessage.bufferFactory().wrap(byteBuffer);
					outputMessage.getHeaders().setContentLength(byteBuffer.remaining());
					return outputMessage.writeWith(Mono.just(buffer));
				});

	}

	private Charset getMediaTypeCharset(MediaType mediaType) {
		if (mediaType != null && mediaType.getCharset() != null) {
			return mediaType.getCharset();
		}
		else {
			return getDefaultCharset();
		}
	}

	private String generateForm(MultiValueMap<String, String> form, Charset charset) {
		StringBuilder builder = new StringBuilder();
		try {
			for (Iterator<String> names = form.keySet().iterator(); names.hasNext();) {
				String name = names.next();
				for (Iterator<String> values = form.get(name).iterator(); values.hasNext();) {
					String value = values.next();
					builder.append(URLEncoder.encode(name, charset.name()));
					if (value != null) {
						builder.append('=');
						builder.append(URLEncoder.encode(value, charset.name()));
						if (values.hasNext()) {
							builder.append('&');
						}
					}
				}
				if (names.hasNext()) {
					builder.append('&');
				}
			}
		}
		catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
		return builder.toString();
	}

	@Override
	public List<MediaType> getWritableMediaTypes() {
		return Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED);
	}

}
