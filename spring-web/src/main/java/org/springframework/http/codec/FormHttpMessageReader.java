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
 * Implementation of {@link HttpMessageReader} to read 'normal' HTML
 * forms with {@code "application/x-www-form-urlencoded"} media type.
 *
 * @author Sebastien Deleuze
 */
public class FormHttpMessageReader implements HttpMessageReader<MultiValueMap<String, String>> {

	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private static final ResolvableType formType = ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class);

	private Charset charset = DEFAULT_CHARSET;


	@Override
	public boolean canRead(ResolvableType elementType, MediaType mediaType) {
		return (mediaType == null || MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(mediaType)) &&
				formType.isAssignableFrom(elementType);
	}

	@Override
	public Flux<MultiValueMap<String, String>> read(ResolvableType elementType,
			ReactiveHttpInputMessage inputMessage, Map<String, Object> hints) {
		return readMono(elementType, inputMessage, hints).flux();
	}

	@Override
	public Mono<MultiValueMap<String, String>> readMono(ResolvableType elementType,
			ReactiveHttpInputMessage inputMessage, Map<String, Object> hints) {

		MediaType contentType = inputMessage.getHeaders().getContentType();
		Charset charset = (contentType.getCharset() != null ? contentType.getCharset() : this.charset);

		return inputMessage.getBody()
				.reduce(DataBuffer::write)
				.map(buffer -> {
					CharBuffer charBuffer = charset.decode(buffer.asByteBuffer());
					DataBufferUtils.release(buffer);
					String body = charBuffer.toString();
					String[] pairs = StringUtils.tokenizeToStringArray(body, "&");
					MultiValueMap<String, String> result = new LinkedMultiValueMap<>(pairs.length);
					try {
						for (String pair : pairs) {
							int idx = pair.indexOf('=');
							if (idx == -1) {
								result.add(URLDecoder.decode(pair, charset.name()), null);
							}
							else {
								String name = URLDecoder.decode(pair.substring(0, idx), charset.name());
								String value = URLDecoder.decode(pair.substring(idx + 1), charset.name());
								result.add(name, value);
							}
						}
					}
					catch (UnsupportedEncodingException ex) {
						throw new IllegalStateException(ex);
					}

					return result;
				});
	}

	@Override
	public List<MediaType> getReadableMediaTypes() {
		return Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED);
	}

	/**
	 * Set the default character set to use for reading form data when the request
	 * Content-Type header does not explicitly specify it.
	 * <p>By default this is set to "UTF-8".
	 */
	public void setCharset(Charset charset) {
		Assert.notNull(charset, "'charset' must not be null");
		this.charset = charset;
	}

}
