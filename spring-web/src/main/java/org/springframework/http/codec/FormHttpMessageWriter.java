/*
 * Copyright 2002-2017 the original author or authors.
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
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * {@link HttpMessageWriter} for writing a {@code MultiValueMap<String, String>}
 * as HTML form data, i.e. {@code "application/x-www-form-urlencoded"}, to the
 * body of a request.
 *
 * <p>Note that unless the media type is explicitly set to
 * {@link MediaType#APPLICATION_FORM_URLENCODED}, the {@link #canWrite} method
 * will need generic type information to confirm the target map has String values.
 * This is because a MultiValueMap with non-String values can be used to write
 * multipart requests.
 *
 * <p>To support both form data and multipart requests, consider using
 * {@link org.springframework.http.codec.multipart.MultipartHttpMessageWriter}
 * configured with this writer as the fallback for writing plain form data.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see org.springframework.http.codec.multipart.MultipartHttpMessageWriter
 */
public class FormHttpMessageWriter implements HttpMessageWriter<MultiValueMap<String, String>> {

	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private static final ResolvableType MULTIVALUE_TYPE =
			ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class);

	private static final List<MediaType> MEDIA_TYPES =
			Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED);


	private Charset defaultCharset = DEFAULT_CHARSET;


	/**
	 * Set the default character set to use for writing form data when the response
	 * Content-Type header does not explicitly specify it.
	 * <p>By default this is set to "UTF-8".
	 */
	public void setDefaultCharset(Charset charset) {
		Assert.notNull(charset, "Charset must not be null");
		this.defaultCharset = charset;
	}

	/**
	 * Return the configured default charset.
	 */
	public Charset getDefaultCharset() {
		return this.defaultCharset;
	}


	@Override
	public List<MediaType> getWritableMediaTypes() {
		return MEDIA_TYPES;
	}


	@Override
	public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
		Class<?> rawClass = elementType.getRawClass();
		if (rawClass == null || !MultiValueMap.class.isAssignableFrom(rawClass)) {
			return false;
		}
		if (MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(mediaType)) {
			// Optimistically, any MultiValueMap with or without generics
			return true;
		}
		if (mediaType == null) {
			// Only String-based MultiValueMap
			return MULTIVALUE_TYPE.isAssignableFrom(elementType);
		}
		return false;
	}

	@Override
	public Mono<Void> write(Publisher<? extends MultiValueMap<String, String>> inputStream,
			ResolvableType elementType, @Nullable MediaType mediaType, ReactiveHttpOutputMessage message,
			Map<String, Object> hints) {

		MediaType contentType = message.getHeaders().getContentType();
		if (contentType == null) {
			contentType = MediaType.APPLICATION_FORM_URLENCODED;
			message.getHeaders().setContentType(contentType);
		}

		Charset charset = getMediaTypeCharset(contentType);

		return Mono.from(inputStream).flatMap(form -> {
					String value = serializeForm(form, charset);
					ByteBuffer byteBuffer = charset.encode(value);
					DataBuffer buffer = message.bufferFactory().wrap(byteBuffer);
					message.getHeaders().setContentLength(byteBuffer.remaining());
					return message.writeWith(Mono.just(buffer));
				});

	}

	private Charset getMediaTypeCharset(@Nullable MediaType mediaType) {
		if (mediaType != null && mediaType.getCharset() != null) {
			return mediaType.getCharset();
		}
		else {
			return getDefaultCharset();
		}
	}

	private String serializeForm(MultiValueMap<String, String> form, Charset charset) {
		StringBuilder builder = new StringBuilder();
		try {
			for (Iterator<String> names = form.keySet().iterator(); names.hasNext();) {
				String name = names.next();
				for (Iterator<?> values = form.get(name).iterator(); values.hasNext();) {
					Object rawValue = values.next();
					builder.append(URLEncoder.encode(name, charset.name()));
					if (rawValue != null) {
						builder.append('=');
						Assert.isInstanceOf(String.class, rawValue,
								"FormHttpMessageWriter supports String values only. " +
										"Use MultipartHttpMessageWriter for multipart requests.");
						builder.append(URLEncoder.encode((String) rawValue, charset.name()));
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

}
