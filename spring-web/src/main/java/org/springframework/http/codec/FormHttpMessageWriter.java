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
 * Implementation of {@link HttpMessageWriter} to write 'normal' HTML
 * forms with {@code "application/x-www-form-urlencoded"} media type.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 * @see MultiValueMap
 */
public class FormHttpMessageWriter implements HttpMessageWriter<MultiValueMap<String, String>> {

	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private static final ResolvableType formType = ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class);

	private Charset charset = DEFAULT_CHARSET;


	@Override
	public boolean canWrite(ResolvableType elementType, MediaType mediaType) {
		return (mediaType == null || MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(mediaType)) &&
				formType.isAssignableFrom(elementType);
	}

	@Override
	public Mono<Void> write(Publisher<? extends MultiValueMap<String, String>> inputStream,
			ResolvableType elementType, MediaType mediaType, ReactiveHttpOutputMessage outputMessage,
			Map<String, Object> hints) {

		MediaType contentType = outputMessage.getHeaders().getContentType();
		Charset charset;
		if (contentType != null) {
			outputMessage.getHeaders().setContentType(contentType);
			charset = (contentType != null && contentType.getCharset() != null ? contentType.getCharset() : this.charset);
		}
		else {
			outputMessage.getHeaders().setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			charset = this.charset;
		}
		return Flux
				.from(inputStream)
				.single()
				.map(form -> generateForm(form))
				.then(value -> {
					ByteBuffer byteBuffer = charset.encode(value);
					DataBuffer buffer = outputMessage.bufferFactory().wrap(byteBuffer);
					outputMessage.getHeaders().setContentLength(byteBuffer.remaining());
					return outputMessage.writeWith(Mono.just(buffer));
				});

	}

	private String generateForm(MultiValueMap<String, String> form) {
		StringBuilder builder = new StringBuilder();
		try {
			for (Iterator<String> nameIterator = form.keySet().iterator(); nameIterator.hasNext();) {
				String name = nameIterator.next();
				for (Iterator<String> valueIterator = form.get(name).iterator(); valueIterator.hasNext();) {
					String value = valueIterator.next();
					builder.append(URLEncoder.encode(name, charset.name()));
					if (value != null) {
						builder.append('=');
						builder.append(URLEncoder.encode(value, charset.name()));
						if (valueIterator.hasNext()) {
							builder.append('&');
						}
					}
				}
				if (nameIterator.hasNext()) {
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

	/**
	 * Set the default character set to use for writing form data when the response
	 * Content-Type header does not explicitly specify it.
	 * <p>By default this is set to "UTF-8".
	 */
	public void setCharset(Charset charset) {
		Assert.notNull(charset, "'charset' must not be null");
		this.charset = charset;
	}

}
