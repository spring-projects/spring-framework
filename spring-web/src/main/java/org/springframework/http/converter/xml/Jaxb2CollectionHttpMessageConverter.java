/*
 * Copyright 2002-2022 the original author or authors.
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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Result;
import javax.xml.transform.Source;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.xml.StaxUtils;

/**
 * An {@code HttpMessageConverter} that can read XML collections using JAXB2.
 *
 * <p>This converter can read {@linkplain Collection collections} that contain classes
 * annotated with {@link XmlRootElement} and {@link XmlType}. Note that this converter
 * does not support writing.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 * @param <T> the converted object type
 */
@SuppressWarnings("rawtypes")
public class Jaxb2CollectionHttpMessageConverter<T extends Collection>
		extends AbstractJaxb2HttpMessageConverter<T> implements GenericHttpMessageConverter<T> {

	private final XMLInputFactory inputFactory = createXmlInputFactory();


	/**
	 * Always returns {@code false} since Jaxb2CollectionHttpMessageConverter
	 * required generic type information in order to read a Collection.
	 */
	@Override
	public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p>Jaxb2CollectionHttpMessageConverter can read a generic
	 * {@link Collection} where the generic type is a JAXB type annotated with
	 * {@link XmlRootElement} or {@link XmlType}.
	 */
	@Override
	public boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
		if (!(type instanceof ParameterizedType parameterizedType)) {
			return false;
		}
		if (!(parameterizedType.getRawType() instanceof Class<?> rawType)) {
			return false;
		}
		if (!(Collection.class.isAssignableFrom(rawType))) {
			return false;
		}
		if (parameterizedType.getActualTypeArguments().length != 1) {
			return false;
		}
		Type typeArgument = parameterizedType.getActualTypeArguments()[0];
		if (!(typeArgument instanceof Class<?> typeArgumentClass)) {
			return false;
		}
		return (typeArgumentClass.isAnnotationPresent(XmlRootElement.class) ||
				typeArgumentClass.isAnnotationPresent(XmlType.class)) && canRead(mediaType);
	}

	/**
	 * Always returns {@code false} since Jaxb2CollectionHttpMessageConverter
	 * does not convert collections to XML.
	 */
	@Override
	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
		return false;
	}

	/**
	 * Always returns {@code false} since Jaxb2CollectionHttpMessageConverter
	 * does not convert collections to XML.
	 */
	@Override
	public boolean canWrite(@Nullable Type type, @Nullable Class<?> clazz, @Nullable MediaType mediaType) {
		return false;
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		// should not be called, since we override canRead/Write
		throw new UnsupportedOperationException();
	}

	@Override
	protected T readFromSource(Class<? extends T> clazz, HttpHeaders headers, Source source) throws Exception {
		// should not be called, since we return false for canRead(Class)
		throw new UnsupportedOperationException();
	}

	@Override
	@SuppressWarnings("unchecked")
	public T read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		ParameterizedType parameterizedType = (ParameterizedType) type;
		T result = createCollection((Class<?>) parameterizedType.getRawType());
		Class<?> elementClass = (Class<?>) parameterizedType.getActualTypeArguments()[0];

		try {
			Unmarshaller unmarshaller = createUnmarshaller(elementClass);
			Charset detectedCharset = detectCharset(inputMessage.getHeaders());
			XMLStreamReader streamReader = (detectedCharset != null) ?
					this.inputFactory.createXMLStreamReader(inputMessage.getBody(), detectedCharset.name()) :
					this.inputFactory.createXMLStreamReader(inputMessage.getBody());
			int event = moveToFirstChildOfRootElement(streamReader);

			while (event != XMLStreamReader.END_DOCUMENT) {
				if (elementClass.isAnnotationPresent(XmlRootElement.class)) {
					result.add(unmarshaller.unmarshal(streamReader));
				}
				else if (elementClass.isAnnotationPresent(XmlType.class)) {
					result.add(unmarshaller.unmarshal(streamReader, elementClass).getValue());
				}
				else {
					// should not happen, since we check in canRead(Type)
					throw new HttpMessageNotReadableException(
							"Cannot unmarshal to [" + elementClass + "]", inputMessage);
				}
				event = moveToNextElement(streamReader);
			}
			return result;
		}
		catch (XMLStreamException ex) {
			throw new HttpMessageNotReadableException(
					"Failed to read XML stream: " + ex.getMessage(), ex, inputMessage);
		}
		catch (UnmarshalException ex) {
			throw new HttpMessageNotReadableException(
					"Could not unmarshal to [" + elementClass + "]: " + ex, ex, inputMessage);
		}
		catch (JAXBException ex) {
			throw new HttpMessageConversionException("Invalid JAXB setup: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Create a Collection of the given type, with the given initial capacity
	 * (if supported by the Collection type).
	 * @param collectionClass the type of Collection to instantiate
	 * @return the created Collection instance
	 */
	@SuppressWarnings("unchecked")
	protected T createCollection(Class<?> collectionClass) {
		if (!collectionClass.isInterface()) {
			try {
				return (T) ReflectionUtils.accessibleConstructor(collectionClass).newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException(
						"Could not instantiate collection class: " + collectionClass.getName(), ex);
			}
		}
		else if (List.class == collectionClass) {
			return (T) new ArrayList();
		}
		else if (SortedSet.class == collectionClass) {
			return (T) new TreeSet();
		}
		else {
			return (T) new LinkedHashSet();
		}
	}

	private int moveToFirstChildOfRootElement(XMLStreamReader streamReader) throws XMLStreamException {
		// root
		int event = streamReader.next();
		while (event != XMLStreamReader.START_ELEMENT) {
			event = streamReader.next();
		}

		// first child
		event = streamReader.next();
		while ((event != XMLStreamReader.START_ELEMENT) && (event != XMLStreamReader.END_DOCUMENT)) {
			event = streamReader.next();
		}
		return event;
	}

	private int moveToNextElement(XMLStreamReader streamReader) throws XMLStreamException {
		int event = streamReader.getEventType();
		while (event != XMLStreamReader.START_ELEMENT && event != XMLStreamReader.END_DOCUMENT) {
			event = streamReader.next();
		}
		return event;
	}

	@Override
	public void write(T t, @Nullable Type type, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		throw new UnsupportedOperationException();
	}

	@Override
	protected void writeToResult(T t, HttpHeaders headers, Result result) throws Exception {
		throw new UnsupportedOperationException();
	}

	/**
	 * Create an {@code XMLInputFactory} that this converter will use to create
	 * {@link javax.xml.stream.XMLStreamReader} and {@link javax.xml.stream.XMLEventReader}
	 * objects.
	 * <p>Can be overridden in subclasses, adding further initialization of the factory.
	 * The resulting factory is cached, so this method will only be called once.
	 * @see StaxUtils#createDefensiveInputFactory()
	 */
	protected XMLInputFactory createXmlInputFactory() {
		return StaxUtils.createDefensiveInputFactory();
	}

}
