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

package org.springframework.http.converter;

import java.io.IOException;
import java.nio.charset.Charset;

import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * An {@code HttpMessageConverter} that uses {@link StringHttpMessageConverter}
 * for reading and writing content and a {@link ConversionService} for converting
 * the String content to and from the target object type.
 *
 * <p>By default, this converter supports the media type {@code text/plain} only.
 * This can be overridden through the {@link #setSupportedMediaTypes supportedMediaTypes}
 * property.
 *
 * <p>A usage example:
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.http.converter.ObjectToStringHttpMessageConverter">
 *   &lt;constructor-arg>
 *     &lt;bean class="org.springframework.context.support.ConversionServiceFactoryBean"/>
 *   &lt;/constructor-arg>
 * &lt;/bean>
 * </pre>
 *
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class ObjectToStringHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

	private final ConversionService conversionService;

	private final StringHttpMessageConverter stringHttpMessageConverter;


	/**
	 * A constructor accepting a {@code ConversionService} to use to convert the
	 * (String) message body to/from the target class type. This constructor uses
	 * {@link StringHttpMessageConverter#DEFAULT_CHARSET} as the default charset.
	 * @param conversionService the conversion service
	 */
	public ObjectToStringHttpMessageConverter(ConversionService conversionService) {
		this(conversionService, StringHttpMessageConverter.DEFAULT_CHARSET);
	}

	/**
	 * A constructor accepting a {@code ConversionService} as well as a default charset.
	 * @param conversionService the conversion service
	 * @param defaultCharset the default charset
	 */
	public ObjectToStringHttpMessageConverter(ConversionService conversionService, Charset defaultCharset) {
		super(defaultCharset, MediaType.TEXT_PLAIN);

		Assert.notNull(conversionService, "ConversionService is required");
		this.conversionService = conversionService;
		this.stringHttpMessageConverter = new StringHttpMessageConverter(defaultCharset);
	}


	/**
	 * Indicates whether the {@code Accept-Charset} should be written to any outgoing request.
	 * <p>Default is {@code true}.
	 */
	public void setWriteAcceptCharset(boolean writeAcceptCharset) {
		this.stringHttpMessageConverter.setWriteAcceptCharset(writeAcceptCharset);
	}


	@Override
	public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
		return canRead(mediaType) && this.conversionService.canConvert(String.class, clazz);
	}

	@Override
	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
		return canWrite(mediaType) && this.conversionService.canConvert(clazz, String.class);
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		// should not be called, since we override canRead/Write
		throw new UnsupportedOperationException();
	}

	@Override
	protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		String value = this.stringHttpMessageConverter.readInternal(String.class, inputMessage);
		Object result = this.conversionService.convert(value, clazz);
		if (result == null) {
			throw new HttpMessageNotReadableException(
					"Unexpected null conversion result for '" + value + "' to " + clazz);
		}
		return result;
	}

	@Override
	protected void writeInternal(Object obj, HttpOutputMessage outputMessage) throws IOException {
		String value = this.conversionService.convert(obj, String.class);
		if (value != null) {
			this.stringHttpMessageConverter.writeInternal(value, outputMessage);
		}
	}

	@Override
	protected Long getContentLength(Object obj, @Nullable MediaType contentType) {
		String value = this.conversionService.convert(obj, String.class);
		if (value == null) {
			return 0L;
		}
		return this.stringHttpMessageConverter.getContentLength(value, contentType);
	}

}
