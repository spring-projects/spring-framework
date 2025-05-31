/*
 * Copyright 2002-2025 the original author or authors.
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

import java.io.StringReader;
import java.nio.charset.Charset;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.MarshalException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.PropertyException;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import org.jspecify.annotations.Nullable;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.util.ClassUtils;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter
 * HttpMessageConverter} that can read and write XML using JAXB2.
 *
 * <p>This converter can read classes annotated with {@link XmlRootElement} and
 * {@link XmlType}, and write classes annotated with {@link XmlRootElement},
 * or subclasses thereof.
 *
 * <p>Note: When using Spring's Marshaller/Unmarshaller abstractions from {@code spring-oxm},
 * you should use the {@link MarshallingHttpMessageConverter} instead.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.0
 * @see MarshallingHttpMessageConverter
 */
public class Jaxb2RootElementHttpMessageConverter extends AbstractJaxb2HttpMessageConverter<Object> {

	private boolean supportDtd = false;

	private boolean processExternalEntities = false;

	private volatile @Nullable SAXParserFactory sourceParserFactory;


	/**
	 * Indicate whether DTD parsing should be supported.
	 * <p>Default is {@code false} meaning that DTD is disabled.
	 */
	public void setSupportDtd(boolean supportDtd) {
		this.supportDtd = supportDtd;
		this.sourceParserFactory = null;
	}

	/**
	 * Return whether DTD parsing is supported.
	 */
	public boolean isSupportDtd() {
		return this.supportDtd;
	}

	/**
	 * Indicate whether external XML entities are processed when converting to a Source.
	 * <p>Default is {@code false}, meaning that external entities are not resolved.
	 * <p><strong>Note:</strong> setting this option to {@code true} also
	 * automatically sets {@link #setSupportDtd} to {@code true}.
	 */
	public void setProcessExternalEntities(boolean processExternalEntities) {
		this.processExternalEntities = processExternalEntities;
		if (processExternalEntities) {
			this.supportDtd = true;
		}
		this.sourceParserFactory = null;
	}

	/**
	 * Return whether XML external entities are allowed.
	 */
	public boolean isProcessExternalEntities() {
		return this.processExternalEntities;
	}


	@Override
	public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
		return (clazz.isAnnotationPresent(XmlRootElement.class) || clazz.isAnnotationPresent(XmlType.class)) &&
				canRead(mediaType);
	}

	@Override
	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
		boolean supportedType = (JAXBElement.class.isAssignableFrom(clazz) ||
				AnnotationUtils.findAnnotation(clazz, XmlRootElement.class) != null);
		return (supportedType && canWrite(mediaType));
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		// should not be called, since we override canRead/Write
		throw new UnsupportedOperationException();
	}

	@Override
	protected Object readFromSource(Class<?> clazz, HttpHeaders headers, Source source) throws Exception {
		try {
			source = processSource(source, detectCharset(headers));
			Unmarshaller unmarshaller = createUnmarshaller(clazz);
			if (clazz.isAnnotationPresent(XmlRootElement.class)) {
				return unmarshaller.unmarshal(source);
			}
			else {
				JAXBElement<?> jaxbElement = unmarshaller.unmarshal(source, clazz);
				return jaxbElement.getValue();
			}
		}
		catch (NullPointerException ex) {
			if (!isSupportDtd()) {
				throw new IllegalStateException("NPE while unmarshalling. " +
						"This can happen due to the presence of DTD declarations which are disabled.", ex);
			}
			throw ex;
		}
		catch (UnmarshalException ex) {
			throw ex;
		}
		catch (JAXBException ex) {
			throw new HttpMessageConversionException("Invalid JAXB setup: " + ex.getMessage(), ex);
		}
	}

	protected Source processSource(Source source, @Nullable Charset charset) {
		if (source instanceof StreamSource streamSource) {
			InputSource inputSource = new InputSource(streamSource.getInputStream());
			if (charset != null) {
				inputSource.setEncoding(charset.name());
			}
			try {
				// By default, Spring will prevent the processing of external entities.
				// This is a mitigation against XXE attacks.
				SAXParserFactory saxParserFactory = this.sourceParserFactory;
				if (saxParserFactory == null) {
					saxParserFactory = SAXParserFactory.newInstance();
					saxParserFactory.setNamespaceAware(true);
					saxParserFactory.setFeature(
							"http://apache.org/xml/features/disallow-doctype-decl", !isSupportDtd());
					saxParserFactory.setFeature(
							"http://xml.org/sax/features/external-general-entities", isProcessExternalEntities());
					this.sourceParserFactory = saxParserFactory;
				}
				SAXParser saxParser = saxParserFactory.newSAXParser();
				XMLReader xmlReader = saxParser.getXMLReader();
				if (!isProcessExternalEntities()) {
					xmlReader.setEntityResolver(NO_OP_ENTITY_RESOLVER);
				}
				return new SAXSource(xmlReader, inputSource);
			}
			catch (SAXException | ParserConfigurationException ex) {
				logger.warn("Processing of external entities could not be disabled", ex);
				return source;
			}
		}
		else {
			return source;
		}
	}

	@Override
	protected void writeToResult(Object value, HttpHeaders headers, Result result) throws Exception {
		try {
			Class<?> clazz = getMarshallerType(value);
			Marshaller marshaller = createMarshaller(clazz);
			setCharset(headers.getContentType(), marshaller);
			marshaller.marshal(value, result);
		}
		catch (MarshalException ex) {
			throw ex;
		}
		catch (JAXBException ex) {
			throw new HttpMessageConversionException("Invalid JAXB setup: " + ex.getMessage(), ex);
		}
	}

	private static Class<?> getMarshallerType(Object value) {
		if (value instanceof JAXBElement<?> jaxbElement) {
			return jaxbElement.getDeclaredType();
		}
		else {
			return ClassUtils.getUserClass(value);
		}
	}

	private void setCharset(@Nullable MediaType contentType, Marshaller marshaller) throws PropertyException {
		if (contentType != null && contentType.getCharset() != null) {
			marshaller.setProperty(Marshaller.JAXB_ENCODING, contentType.getCharset().name());
		}
	}

	@Override
	protected boolean supportsRepeatableWrites(Object o) {
		return true;
	}


	private static final EntityResolver NO_OP_ENTITY_RESOLVER =
			(publicId, systemId) -> new InputSource(new StringReader(""));

}
