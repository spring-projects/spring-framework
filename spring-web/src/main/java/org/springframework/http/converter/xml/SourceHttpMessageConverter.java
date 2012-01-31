/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.http.converter.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.InputSource;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter} that can read and write {@link
 * Source} objects.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public class SourceHttpMessageConverter<T extends Source> extends AbstractXmlHttpMessageConverter<T> {

	@Override
	public boolean supports(Class<?> clazz) {
		return DOMSource.class.equals(clazz) || SAXSource.class.equals(clazz) || StreamSource.class.equals(clazz) ||
				Source.class.equals(clazz);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected T readFromSource(Class clazz, HttpHeaders headers, Source source) throws IOException {
		try {
			if (DOMSource.class.equals(clazz)) {
				DOMResult domResult = new DOMResult();
				transform(source, domResult);
				return (T) new DOMSource(domResult.getNode());
			}
			else if (SAXSource.class.equals(clazz)) {
				ByteArrayInputStream bis = transformToByteArrayInputStream(source);
				return (T) new SAXSource(new InputSource(bis));
			}
			else if (StreamSource.class.equals(clazz) || Source.class.equals(clazz)) {
				ByteArrayInputStream bis = transformToByteArrayInputStream(source);
				return (T) new StreamSource(bis);
			}
			else {
				throw new HttpMessageConversionException("Could not read class [" + clazz +
						"]. Only DOMSource, SAXSource, and StreamSource are supported.");
			}
		}
		catch (TransformerException ex) {
			throw new HttpMessageNotReadableException("Could not transform from [" + source + "] to [" + clazz + "]",
					ex);
		}
	}

	private ByteArrayInputStream transformToByteArrayInputStream(Source source) throws TransformerException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		transform(source, new StreamResult(bos));
		return new ByteArrayInputStream(bos.toByteArray());
	}

	@Override
	protected Long getContentLength(T t, MediaType contentType) {
		if (t instanceof DOMSource) {
			try {
				CountingOutputStream os = new CountingOutputStream();
				transform(t, new StreamResult(os));
				return os.count;
			}
			catch (TransformerException ex) {
				// ignore
			}
		}
		return null;
	}

	@Override
	protected void writeToResult(T t, HttpHeaders headers, Result result) throws IOException {
		try {
			transform(t, result);
		}
		catch (TransformerException ex) {
			throw new HttpMessageNotWritableException("Could not transform [" + t + "] to [" + result + "]", ex);
		}
	}

	private static class CountingOutputStream extends OutputStream {

		private long count = 0;

		@Override
		public void write(int b) throws IOException {
			count++;
		}

		@Override
		public void write(byte[] b) throws IOException {
			count += b.length;
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			count += len;
		}
	}

}
