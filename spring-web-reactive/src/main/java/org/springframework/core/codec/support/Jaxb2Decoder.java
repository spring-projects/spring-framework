/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.codec.support;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.reactivestreams.Publisher;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.support.DataBufferUtils;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * Decode from a bytes stream of XML elements to a stream of {@code Object} (POJO).
 *
 * @author Sebastien Deleuze
 * @see Jaxb2Encoder
 */
public class Jaxb2Decoder extends AbstractDecoder<Object> {

	private final ConcurrentMap<Class<?>, JAXBContext> jaxbContexts = new ConcurrentHashMap<>(64);


	public Jaxb2Decoder() {
		super(MimeTypeUtils.APPLICATION_XML, MimeTypeUtils.TEXT_XML);
	}


	@Override
	public Flux<Object> decode(Publisher<DataBuffer> inputStream, ResolvableType type,
			MimeType mimeType, Object... hints) {

		Class<?> outputClass = type.getRawClass();
		try {
			Source source = processSource(
					new StreamSource(DataBufferUtils.toInputStream(inputStream)));
			Unmarshaller unmarshaller = createUnmarshaller(outputClass);
			if (outputClass.isAnnotationPresent(XmlRootElement.class)) {
				return Flux.just(unmarshaller.unmarshal(source));
			}
			else {
				JAXBElement<?> jaxbElement = unmarshaller.unmarshal(source, outputClass);
				return Flux.just(jaxbElement.getValue());
			}
		}
		catch (UnmarshalException ex) {
			return Flux.error(
			  new CodecException("Could not unmarshal to [" + outputClass + "]: " + ex.getMessage(), ex));
		}
		catch (JAXBException ex) {
			return Flux.error(new CodecException("Could not instantiate JAXBContext: " +
					ex.getMessage(), ex));
		}
	}

	protected Source processSource(Source source) {
		if (source instanceof StreamSource) {
			StreamSource streamSource = (StreamSource) source;
			InputSource inputSource = new InputSource(streamSource.getInputStream());
			try {
				XMLReader xmlReader = XMLReaderFactory.createXMLReader();
				return new SAXSource(xmlReader, inputSource);
			}
			catch (SAXException ex) {
				throw new CodecException("Error while processing the source", ex);
			}
		}
		else {
			return source;
		}
	}

	protected final Unmarshaller createUnmarshaller(Class<?> clazz) throws JAXBException {
		try {
			JAXBContext jaxbContext = getJaxbContext(clazz);
			return jaxbContext.createUnmarshaller();
		}
		catch (JAXBException ex) {
			throw new CodecException("Could not create Unmarshaller for class " +
					"[" + clazz + "]: " + ex.getMessage(), ex);
		}
	}

	protected final JAXBContext getJaxbContext(Class<?> clazz) {
		Assert.notNull(clazz, "'clazz' must not be null");
		JAXBContext jaxbContext = this.jaxbContexts.get(clazz);
		if (jaxbContext == null) {
			try {
				jaxbContext = JAXBContext.newInstance(clazz);
				this.jaxbContexts.putIfAbsent(clazz, jaxbContext);
			}
			catch (JAXBException ex) {
				throw new CodecException("Could not instantiate JAXBContext for class " +
						"[" + clazz + "]: " + ex.getMessage(), ex);
			}
		}
		return jaxbContext;
	}

}
