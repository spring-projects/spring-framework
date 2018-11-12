/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.List;
import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.log.LogFormatUtils;
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
public class FormHttpMessageWriter extends LoggingCodecSupport
		implements HttpMessageWriter<MultiValueMap<String, String>> {

	/**
	 * The default charset used by the writer.
	 */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private static final MediaType DEFAULT_FORM_DATA_MEDIA_TYPE =
			new MediaType(MediaType.APPLICATION_FORM_URLENCODED, DEFAULT_CHARSET);

	private static final List<MediaType> MEDIA_TYPES =
			Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED);

	private static final ResolvableType MULTIVALUE_TYPE =
			ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class);


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
		if (!MultiValueMap.class.isAssignableFrom(elementType.toClass())) {
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

		mediaType = getMediaType(mediaType);
		message.getHeaders().setContentType(mediaType);

		Charset charset = mediaType.getCharset();
		Assert.notNull(charset, "No charset"); // should never occur

		return Mono.from(inputStream).flatMap(form -> {
			logFormData(form, hints);
			String value = serializeForm(form, charset);
			ByteBuffer byteBuffer = charset.encode(value);
			DataBuffer buffer = message.bufferFactory().wrap(byteBuffer);
			message.getHeaders().setContentLength(byteBuffer.remaining());
			return message.writeWith(Mono.just(buffer));
		});
	}

	private MediaType getMediaType(@Nullable MediaType mediaType) {
		if (mediaType == null) {
			return DEFAULT_FORM_DATA_MEDIA_TYPE;
		}
		else if (mediaType.getCharset() == null) {
			return new MediaType(mediaType, getDefaultCharset());
		}
		else {
			return mediaType;
		}
	}

	private void logFormData(MultiValueMap<String, String> form, Map<String, Object> hints) {
		LogFormatUtils.traceDebug(logger, traceOn -> Hints.getLogPrefix(hints) + "Writing " +
				(isEnableLoggingRequestDetails() ?
						LogFormatUtils.formatValue(form, !traceOn) :
						"form fields " + form.keySet() + " (content masked)"));
	}

	protected String serializeForm(MultiValueMap<String, String> formData, Charset charset) {
		StringBuilder builder = new StringBuilder();
		formData.forEach((name, values) ->
				values.forEach(value -> {
					try {
						if (builder.length() != 0) {
							builder.append('&');
						}
						builder.append(URLEncoder.encode(name, charset.name()));
						if (value != null) {
							builder.append('=');
							builder.append(URLEncoder.encode(value, charset.name()));
						}
					}
					catch (UnsupportedEncodingException ex) {
						throw new IllegalStateException(ex);
					}
				}));
		return builder.toString();
	}

}
