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
import java.net.URLDecoder;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Implementation of an {@link HttpMessageReader} to read HTML form data, i.e.
 * request body with media type {@code "application/x-www-form-urlencoded"}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class FormHttpMessageReader implements HttpMessageReader<MultiValueMap<String, String>> {

	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private static final ResolvableType MULTIVALUE_TYPE =
			ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class);


	private Charset defaultCharset = DEFAULT_CHARSET;


	/**
	 * Set the default character set to use for reading form data when the
	 * request Content-Type header does not explicitly specify it.
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
	public boolean canRead(ResolvableType elementType, MediaType mediaType) {
		return MULTIVALUE_TYPE.isAssignableFrom(elementType) &&
				(mediaType == null || MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(mediaType));
	}

	@Override
	public Flux<MultiValueMap<String, String>> read(ResolvableType elementType,
			ReactiveHttpInputMessage inputMessage, Map<String, Object> hints) {

		return Flux.from(readMono(elementType, inputMessage, hints));
	}

	@Override
	public Mono<MultiValueMap<String, String>> readMono(ResolvableType elementType,
			ReactiveHttpInputMessage inputMessage, Map<String, Object> hints) {

		MediaType contentType = inputMessage.getHeaders().getContentType();
		Charset charset = getMediaTypeCharset(contentType);

		return inputMessage.getBody()
				.reduce(DataBuffer::write)
				.map(buffer -> {
					CharBuffer charBuffer = charset.decode(buffer.asByteBuffer());
					String body = charBuffer.toString();
					DataBufferUtils.release(buffer);
					return parseFormData(charset, body);
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

	private MultiValueMap<String, String> parseFormData(Charset charset, String body) {
		String[] pairs = StringUtils.tokenizeToStringArray(body, "&");
		MultiValueMap<String, String> result = new LinkedMultiValueMap<>(pairs.length);
		try {
			for (String pair : pairs) {
				int idx = pair.indexOf('=');
				if (idx == -1) {
					result.add(URLDecoder.decode(pair, charset.name()), null);
				}
				else {
					String name = URLDecoder.decode(pair.substring(0, idx),  charset.name());
					String value = URLDecoder.decode(pair.substring(idx + 1), charset.name());
					result.add(name, value);
				}
			}
		}
		catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
		return result;
	}

	@Override
	public List<MediaType> getReadableMediaTypes() {
		return Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED);
	}

}
