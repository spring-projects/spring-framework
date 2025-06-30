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

package org.springframework.http.converter.xml;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.StreamUtils;

/**
 * Abstract base class for {@link org.springframework.http.converter.HttpMessageConverter HttpMessageConverters}
 * that convert from/to XML.
 *
 * <p>By default, subclasses of this converter support {@code text/xml}, {@code application/xml}, and {@code
 * application/*+xml}. This can be overridden by setting the {@link #setSupportedMediaTypes(java.util.List)
 * supportedMediaTypes} property.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @param <T> the converted object type
 */
public abstract class AbstractXmlHttpMessageConverter<T> extends AbstractHttpMessageConverter<T> {

	private final TransformerFactory transformerFactory = TransformerFactory.newInstance();


	/**
	 * Protected constructor that sets the {@link #setSupportedMediaTypes(java.util.List) supportedMediaTypes}
	 * to {@code text/xml} and {@code application/xml}, and {@code application/*+xml}.
	 */
	protected AbstractXmlHttpMessageConverter() {
		super(MediaType.APPLICATION_XML, MediaType.TEXT_XML, new MediaType("application", "*+xml"));
	}


	@Override
	public final T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		try {
			InputStream inputStream = StreamUtils.nonClosing(inputMessage.getBody());
			return readFromSource(clazz, inputMessage.getHeaders(), new StreamSource(inputStream));
		}
		catch (IOException | HttpMessageConversionException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new HttpMessageNotReadableException("Could not unmarshal to [" + clazz + "]: " + ex,
					ex, inputMessage);
		}
	}

	@Override
	protected final void writeInternal(T t, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		try {
			writeToResult(t, outputMessage.getHeaders(), new StreamResult(outputMessage.getBody()));
		}
		catch (IOException | HttpMessageConversionException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new HttpMessageNotWritableException("Could not marshal [" + t + "]: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Transforms the given {@code Source} to the {@code Result}.
	 * @param source the source to transform from
	 * @param result the result to transform to
	 * @throws TransformerException in case of transformation errors
	 */
	protected void transform(Source source, Result result) throws TransformerException {
		this.transformerFactory.newTransformer().transform(source, result);
	}


	/**
	 * Abstract template method called from {@link #read(Class, HttpInputMessage)}.
	 * @param clazz the type of object to return
	 * @param headers the HTTP input headers
	 * @param source the HTTP input body
	 * @return the converted object
	 * @throws Exception in case of I/O or conversion errors
	 */
	protected abstract T readFromSource(Class<? extends T> clazz, HttpHeaders headers, Source source) throws Exception;

	/**
	 * Abstract template method called from {@link #writeInternal(Object, HttpOutputMessage)}.
	 * @param t the object to write to the output message
	 * @param headers the HTTP output headers
	 * @param result the HTTP output body
	 * @throws Exception in case of I/O or conversion errors
	 */
	protected abstract void writeToResult(T t, HttpHeaders headers, Result result) throws Exception;

}
