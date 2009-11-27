/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.http.converter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

/**
 * Implementation of {@link HttpMessageConverter} that can read and write form data.
 *
 * <p>By default, this converter reads and writes the media type ({@code application/x-www-form-urlencoded}). This can
 * be overridden by setting the {@link #setSupportedMediaTypes(java.util.List) supportedMediaTypes} property. Form data
 * is read from and written into a {@link MultiValueMap MultiValueMap&lt;String, String&gt;}.
 *
 * @author Arjen Poutsma
 * @see MultiValueMap
 * @since 3.0
 */
public class FormHttpMessageConverter extends AbstractHttpMessageConverter<MultiValueMap<String, String>> {

	public static final Charset DEFAULT_CHARSET = Charset.forName(WebUtils.DEFAULT_CHARACTER_ENCODING);

	/** Creates a new instance of the {@code FormHttpMessageConverter}. */
	public FormHttpMessageConverter() {
		super(new MediaType("application", "x-www-form-urlencoded"));
	}

	@Override
	public boolean supports(Class<? extends MultiValueMap<String, String>> clazz) {
		return MultiValueMap.class.isAssignableFrom(clazz);
	}

	@Override
	public MultiValueMap<String, String> readInternal(Class<MultiValueMap<String, String>> clazz,
			HttpInputMessage inputMessage) throws IOException {
		MediaType contentType = inputMessage.getHeaders().getContentType();
		Charset charset = contentType.getCharSet() != null ? contentType.getCharSet() : DEFAULT_CHARSET;
		String body = FileCopyUtils.copyToString(new InputStreamReader(inputMessage.getBody(), charset));

		String[] pairs = StringUtils.tokenizeToStringArray(body, "&");

		MultiValueMap<String, String> result = new LinkedMultiValueMap<String, String>(pairs.length);

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
		return result;
	}

	@Override
	protected void writeInternal(MultiValueMap<String, String> form, HttpOutputMessage outputMessage)
			throws IOException {
		MediaType contentType = getDefaultContentType(form);
		Charset charset = contentType.getCharSet() != null ? contentType.getCharSet() : DEFAULT_CHARSET;
		StringBuilder builder = new StringBuilder();
		for (Iterator<Map.Entry<String, List<String>>> entryIterator = form.entrySet().iterator();
				entryIterator.hasNext();) {
			Map.Entry<String, List<String>> entry = entryIterator.next();
			String name = entry.getKey();
			for (Iterator<String> valueIterator = entry.getValue().iterator(); valueIterator.hasNext();) {
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
			if (entryIterator.hasNext()) {
				builder.append('&');
			}
		}
		FileCopyUtils.copy(builder.toString(), new OutputStreamWriter(outputMessage.getBody(), charset));
	}
}
