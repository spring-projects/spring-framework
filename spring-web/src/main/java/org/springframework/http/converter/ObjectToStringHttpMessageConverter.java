/*
 * Copyright 2002-2012 the original author or authors.
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
import org.springframework.util.Assert;

/**
 * An {@code HttpMessageConverter} that uses {@link StringHttpMessageConverter}
 * for reading and writing content and a {@link ConversionService} for converting
 * the String content to and from the target object type.
 * <p>
 * By default, this converter supports the media type {@code text/plain} only.
 * This can be overridden by setting the
 * {@link #setSupportedMediaTypes supportedMediaTypes} property.
 * Example of usage:
 *
 * <pre>
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

	public static final Charset DEFAULT_CHARSET = Charset.forName("ISO-8859-1");

	private ConversionService conversionService;

	private StringHttpMessageConverter stringHttpMessageConverter;


	/**
	 * A constructor accepting a {@code ConversionService} to use to convert the
	 * (String) message body to/from the target class type.
	 * @param conversionService the conversion service
	 */
	public ObjectToStringHttpMessageConverter(ConversionService conversionService) {
		this(conversionService, DEFAULT_CHARSET);
	}

	public ObjectToStringHttpMessageConverter(ConversionService conversionService, Charset defaultCharset) {
		super(new MediaType("text", "plain", defaultCharset));

		Assert.notNull(conversionService, "conversionService is required");
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
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		return this.conversionService.canConvert(String.class, clazz) && canRead(mediaType);
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return this.conversionService.canConvert(clazz, String.class) && canWrite(mediaType);
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		// should not be called, since we override canRead/Write
		throw new UnsupportedOperationException();
	}

	@Override
	protected Object readInternal(Class<? extends Object> clazz, HttpInputMessage inputMessage) throws IOException {
		String value = this.stringHttpMessageConverter.readInternal(String.class, inputMessage);
		return this.conversionService.convert(value, clazz);
	}

	@Override
	protected void writeInternal(Object obj, HttpOutputMessage outputMessage) throws IOException {
		String s = this.conversionService.convert(obj, String.class);
		this.stringHttpMessageConverter.writeInternal(s, outputMessage);
	}

	@Override
	protected Long getContentLength(Object obj, MediaType contentType) {
		String value = this.conversionService.convert(obj, String.class);
		return this.stringHttpMessageConverter.getContentLength(value, contentType);
	}

}
