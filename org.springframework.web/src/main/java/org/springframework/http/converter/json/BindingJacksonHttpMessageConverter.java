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

package org.springframework.http.converter.json;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.Assert;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter HttpMessageConverter} that can read
 * and write JSON using <a href="http://jackson.codehaus.org/">Jackson's</a> {@link ObjectMapper}.
 *
 * <p>This converter can be used to bind to typed beans, or untyped {@link java.util.HashMap HashMap} instances. 
 *
 * <p>By default, this converter supports {@code application/json}. This can be overridden by setting the {@link
 * #setSupportedMediaTypes(List) supportedMediaTypes} property, and overriding the {@link #getContentType(Object)}
 * method.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public class BindingJacksonHttpMessageConverter<T> extends AbstractHttpMessageConverter<T> {

	private ObjectMapper objectMapper = new ObjectMapper();

	private JsonFactory jsonFactory = new MappingJsonFactory();

	private JsonEncoding encoding = JsonEncoding.UTF8;

	/**
	 * Construct a new {@code BindingJacksonHttpMessageConverter},
	 */
	public BindingJacksonHttpMessageConverter() {
		super(new MediaType("application", "json"));
	}

	/**
	 * Sets the {@code ObjectMapper} for this converter. By default, a default {@link ObjectMapper#ObjectMapper()
	 * ObjectMapper} is used.
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "'objectMapper' must not be null");
		this.objectMapper = objectMapper;
	}

	/** Sets the {@code JsonFactory} for this converter. By default, a {@link MappingJsonFactory} is used. */
	public void setJsonFactory(JsonFactory jsonFactory) {
		Assert.notNull(jsonFactory, "'jsonFactory' must not be null");
		this.jsonFactory = jsonFactory;
	}

	/**
	 * Sets the {@code JsonEncoding} for this converter. By default, {@linkplain JsonEncoding#UTF8 UTF-8} is used.
	 */
	public void setEncoding(JsonEncoding encoding) {
		this.encoding = encoding;
	}

	public boolean supports(Class<? extends T> clazz) {
		return objectMapper.canSerialize(clazz);
	}

	@Override
	protected T readInternal(Class<T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		return objectMapper.readValue(inputMessage.getBody(), clazz);
	}

	@Override
	protected MediaType getContentType(T t) {
		Charset charset = Charset.forName(encoding.getJavaName());
		return new MediaType("application", "json", charset);
	}

	@Override
	protected void writeInternal(T t, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(outputMessage.getBody(), encoding);
		objectMapper.writeValue(jsonGenerator, t);
	}
}
