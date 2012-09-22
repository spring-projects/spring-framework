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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

/**
 * Implementation of
 * {@link org.springframework.http.converter.HttpMessageConverter} that can read
 * and write strings based on the conversion, suggested by conversion service.
 * <p>
 * By default, this converter supports all media types (
 * <code>&#42;&#47;&#42;</code>), and writes with a {@code Content-Type} of
 * {@code text/plain}. This can be overridden by setting the
 * {@link #setSupportedMediaTypes supportedMediaTypes} property. Example of
 * usage:
 * 
 * <pre>
 * &lt;bean class="org.epo.lifesciences.common.spring.ObjectToStringHttpMessageConverter">
 *   &lt;property name="conversionService">
 *     &lt;bean class="org.springframework.context.support.ConversionServiceFactoryBean" />
 *   &lt;/property>
 * &lt;/bean>
 * </pre>
 * 
 * @author Arjen Poutsma
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 * @since 3.0
 */
public class ObjectToStringHttpMessageConverter extends AbstractHttpMessageConverter<Object>
		implements InitializingBean {

	private ConversionService conversionService;

	private Charset defaultCharset = StringHttpMessageConverter.DEFAULT_CHARSET;

	private boolean writeAcceptCharset = true;

	/**
	 * This delegate knows how to convert the message body to/from string.
	 */
	private StringHttpMessageConverter stringHttpMessageConverter;

	/**
	 * Sets the conversion service to use.
	 */
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * Sets the default charset to use.
	 * <p>
	 * Default charset to use is ISO-8859-1 (as defined in
	 * {@link #DEFAULT_CHARSET}).
	 */
	public void setDefaultCharset(Charset defaultCharset) {
		this.defaultCharset = defaultCharset;
	}

	/**
	 * Indicates whether the {@code Accept-Charset} should be written to any
	 * outgoing request.
	 * <p>
	 * Default is {@code true}.
	 */
	public void setWriteAcceptCharset(boolean writeAcceptCharset) {
		this.writeAcceptCharset = writeAcceptCharset;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return conversionService.canConvert(clazz, String.class);
	}

	@Override
	protected Object readInternal(Class<? extends Object> clazz, HttpInputMessage inputMessage)
			throws IOException {
		String s = stringHttpMessageConverter.readInternal(String.class, inputMessage);

		return conversionService.convert(s, clazz);
	}

	@Override
	protected Long getContentLength(Object obj, MediaType contentType) {
		String s = conversionService.convert(obj, String.class);

		return stringHttpMessageConverter.getContentLength(s, contentType);
	}

	@Override
	protected void writeInternal(Object obj, HttpOutputMessage outputMessage) throws IOException {
		String s = conversionService.convert(obj, String.class);

		stringHttpMessageConverter.writeInternal(s, outputMessage);
	}

	/**
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() {
		Assert.notNull(conversionService, "The conversion service must be provided");

		stringHttpMessageConverter = new StringHttpMessageConverter(defaultCharset);

		stringHttpMessageConverter.setWriteAcceptCharset(writeAcceptCharset);

		setSupportedMediaTypes(stringHttpMessageConverter.getSupportedMediaTypes());
	}
}
