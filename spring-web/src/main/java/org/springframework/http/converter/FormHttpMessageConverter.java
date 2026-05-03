/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.converter;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * Implementation of {@link HttpMessageConverter} to read and
 * write URL encoded forms. For multipart support, see the
 * {@link org.springframework.http.converter.multipart.MultipartHttpMessageConverter}.
 *
 * <p>This converter can read and write the
 * {@code "application/x-www-form-urlencoded"} media type as
 * {@link MultiValueMap MultiValueMap&lt;String, String&gt;}.
 *
 * <h3>Examples</h3>
 *
 * <p>The following snippet shows how to submit an HTML form using the
 * {@code "application/x-www-form-urlencoded"} content type.
 *
 * <pre class="code">
 * RestClient restClient = RestClient.create();
 *
 * MultiValueMap&lt;String, String&gt; form = new LinkedMultiValueMap&lt;&gt;();
 * form.add("field 1", "value 1");
 * form.add("field 2", "value 2");
 * form.add("field 2", "value 3");
 * form.add("field 3", 4);
 *
 * ResponseEntity&lt;Void&gt; response = restClient.post()
 *   .uri("https://example.com/myForm")
 *   .contentType(MediaType.APPLICATION_FORM_URLENCODED)
 *   .body(form)
 *   .retrieve()
 *   .toBodilessEntity();</pre>
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Brian Clozel
 * @since 3.0
 * @see org.springframework.util.MultiValueMap
 */
public class FormHttpMessageConverter implements SmartHttpMessageConverter<Object> {

	/** The default charset used by the converter. */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private static final ResolvableType MULTIVALUE_TYPE =
			ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class);

	private static final ResolvableType MAP_TYPE =
			ResolvableType.forClassWithGenerics(Map.class, String.class, String.class);

	private Charset charset = DEFAULT_CHARSET;


	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return List.of(MediaType.APPLICATION_FORM_URLENCODED);
	}

	/**
	 * Set the default character set to use for reading and writing form data when
	 * the request or response {@code Content-Type} header does not explicitly
	 * specify it.
	 * <p>By default, this is set to "UTF-8".
	 */
	public void setCharset(@Nullable Charset charset) {
		if (charset != this.charset) {
			this.charset = (charset != null ? charset : DEFAULT_CHARSET);
		}
	}

	@Override
	public boolean canRead(ResolvableType type, @Nullable MediaType mediaType) {
		return canConvert(type, mediaType);
	}

	@Override
	public boolean canWrite(ResolvableType targetType, Class<?> valueClass, @Nullable MediaType mediaType) {
		return canConvert(targetType, mediaType);
	}

	private boolean canConvert(ResolvableType targetType, @Nullable MediaType mediaType) {
		if (!Map.class.isAssignableFrom(targetType.toClass())) {
			return false;
		}
		for (MediaType supportedMediaType : getSupportedMediaTypes()) {
			if (supportedMediaType.includes(mediaType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Object read(ResolvableType type, HttpInputMessage inputMessage, @Nullable Map<String, Object> hints)
			throws IOException, HttpMessageNotReadableException {

		MediaType contentType = inputMessage.getHeaders().getContentType();
		Charset charset = (contentType != null && contentType.getCharset() != null ?
				contentType.getCharset() : this.charset);
		String body = StreamUtils.copyToString(inputMessage.getBody(), charset);

		String[] pairs = StringUtils.tokenizeToStringArray(body, "&");
		MultiValueMap<String, String> result = new LinkedMultiValueMap<>(pairs.length);
		try {
			for (String pair : pairs) {
				int idx = pair.indexOf('=');
				if (idx == -1) {
					result.add(URLDecoder.decode(pair, charset), null);
				}
				else {
					String name = URLDecoder.decode(pair.substring(0, idx), charset);
					String value = URLDecoder.decode(pair.substring(idx + 1), charset);
					result.add(name, value);
				}
			}
		}
		catch (IllegalArgumentException ex) {
			throw new HttpMessageNotReadableException("Could not decode HTTP form payload", ex, inputMessage);
		}
		if (!MultiValueMap.class.isAssignableFrom(type.toClass())) {
			return result.asSingleValueMap();
		}
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void write(Object data, ResolvableType type, @Nullable MediaType contentType,
					HttpOutputMessage outputMessage, @Nullable Map<String, Object> hints) throws IOException, HttpMessageNotWritableException {

		Assert.isInstanceOf(Map.class, data, "data must be of type Map or MultiValueMap");

		contentType = getFormContentType(contentType);
		outputMessage.getHeaders().setContentType(contentType);
		Charset charset = (contentType.getCharset() != null ? contentType.getCharset() : this.charset);

		String serializedForm = "";
		if (data instanceof MultiValueMap<?,?> formData) {
			serializedForm = serializeForm((MultiValueMap<String, String>) formData, charset);
		}
		else if (data instanceof Map<?,?> formData) {
			serializedForm = serializeForm((Map<String, String>) formData, charset);
		}
		byte[] bytes = serializedForm.getBytes(charset);
		outputMessage.getHeaders().setContentLength(bytes.length);

		if (outputMessage instanceof StreamingHttpOutputMessage streamingOutputMessage) {
			streamingOutputMessage.setBody(bytes);
		}
		else {
			StreamUtils.copy(bytes, outputMessage.getBody());
		}
	}

	/**
	 * Return the content type used to write forms, either the given content type
	 * or otherwise {@code application/x-www-form-urlencoded}.
	 * @param contentType the content type passed to {@link #write}, or {@code null}
	 * @return the content type to use
	 * @since 5.2.2
	 */
	protected MediaType getFormContentType(@Nullable MediaType contentType) {
		if (contentType == null) {
			return MediaType.APPLICATION_FORM_URLENCODED;
		}
		// Some servers don't handle charset parameter and spec is unclear,
		// Add it only if it is not DEFAULT_CHARSET.
		if (contentType.getCharset() == null && this.charset != DEFAULT_CHARSET) {
			return new MediaType(contentType, this.charset);
		}
		return contentType;
	}

	protected String serializeForm(Map<String, String> formData, Charset charset) {
		StringBuilder builder = new StringBuilder();
		formData.forEach((name, value) -> {
			if (name == null) {
				Assert.isTrue(ObjectUtils.isEmpty(value), () -> "Null name in form data: " + formData);
				return;
			}
			serializeValue(builder, name, value, charset);
		});
		return builder.toString();
	}

	protected String serializeForm(MultiValueMap<String, String> formData, Charset charset) {
		StringBuilder builder = new StringBuilder();
		formData.forEach((name, values) -> {
				if (name == null) {
					Assert.isTrue(CollectionUtils.isEmpty(values), () -> "Null name in form data: " + formData);
					return;
				}
				values.forEach(value -> serializeValue(builder, name, value, charset));
		});
		return builder.toString();
	}

	private void serializeValue(StringBuilder builder, String name, String value, Charset charset) {
		if (builder.length() != 0) {
			builder.append('&');
		}
		builder.append(URLEncoder.encode(name, charset));
		if (value != null) {
			builder.append('=');
			builder.append(URLEncoder.encode(value, charset));
		}
	}

}
