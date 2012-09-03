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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

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

	private Charset defaultCharset;

	private List<Charset> availableCharsets;

	private boolean writeAcceptCharset = true;

	static final Charset DEFAULT_CHARSET = Charset.forName("ISO-8859-1");

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
		String s = FileCopyUtils.copyToString(new InputStreamReader(inputMessage.getBody(),
				getContentTypeCharset(inputMessage.getHeaders().getContentType())));

		return conversionService.convert(s, clazz);
	}

	@Override
	protected Long getContentLength(Object obj, MediaType contentType) {
		String s = conversionService.convert(obj, String.class);

		return Long.valueOf(s.getBytes(getContentTypeCharset(contentType)).length);
	}

	@Override
	protected void writeInternal(Object obj, HttpOutputMessage outputMessage) throws IOException {
		if (writeAcceptCharset) {
			outputMessage.getHeaders().setAcceptCharset(getAcceptedCharsets());
		}

		String s = conversionService.convert(obj, String.class);

		FileCopyUtils.copy(s, new OutputStreamWriter(outputMessage.getBody(),
				getContentTypeCharset(outputMessage.getHeaders().getContentType())));
	}

	/**
	 * Return the list of supported {@link Charset}.
	 * <p>
	 * By default, returns {@link Charset#availableCharsets()}. Can be
	 * overridden in subclasses.
	 * 
	 * @return the list of accepted charsets
	 */
	protected List<Charset> getAcceptedCharsets() {
		return availableCharsets;
	}

	/**
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() {
		Assert.notNull(conversionService, "The conversion service must be provided");

		if (defaultCharset == null) {
			defaultCharset = DEFAULT_CHARSET;
		}

		availableCharsets = new ArrayList<Charset>(Charset.availableCharsets().values());

		setSupportedMediaTypes(Arrays.asList(new MediaType("text", "plain", defaultCharset),
				MediaType.ALL));
	}

	private Charset getContentTypeCharset(MediaType contentType) {
		if (contentType != null && contentType.getCharSet() != null) {
			return contentType.getCharSet();
		}

		return defaultCharset;
	}
}
