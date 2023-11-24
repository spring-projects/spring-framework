/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.http.codec.xml;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import org.reactivestreams.Publisher;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDecoder;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.xml.StaxUtils;

/**
 * Decode from a bytes stream containing XML elements to a stream of
 * {@code Object}s (POJOs).
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @since 5.0
 * @see Jaxb2XmlEncoder
 */
public class Jaxb2XmlDecoder extends AbstractDecoder<Object> {

	private static final XMLInputFactory inputFactory = StaxUtils.createDefensiveInputFactory();


	private final XmlEventDecoder xmlEventDecoder = new XmlEventDecoder();

	private final JaxbContextContainer jaxbContexts = new JaxbContextContainer();

	private Function<Unmarshaller, Unmarshaller> unmarshallerProcessor = Function.identity();

	private int maxInMemorySize = 256 * 1024;


	public Jaxb2XmlDecoder() {
		super(MimeTypeUtils.APPLICATION_XML, MimeTypeUtils.TEXT_XML, new MediaType("application", "*+xml"));
	}

	/**
	 * Create a {@code Jaxb2XmlDecoder} with the specified MIME types.
	 * @param supportedMimeTypes supported MIME types
	 * @since 5.1.9
	 */
	public Jaxb2XmlDecoder(MimeType... supportedMimeTypes) {
		super(supportedMimeTypes);
	}


	/**
	 * Configure a processor function to customize Unmarshaller instances.
	 * @param processor the function to use
	 * @since 5.1.3
	 */
	public void setUnmarshallerProcessor(Function<Unmarshaller, Unmarshaller> processor) {
		this.unmarshallerProcessor = this.unmarshallerProcessor.andThen(processor);
	}

	/**
	 * Return the configured processor for customizing Unmarshaller instances.
	 * @since 5.1.3
	 */
	public Function<Unmarshaller, Unmarshaller> getUnmarshallerProcessor() {
		return this.unmarshallerProcessor;
	}

	/**
	 * Set the max number of bytes that can be buffered by this decoder.
	 * This is either the size of the entire input when decoding as a whole, or when
	 * using async parsing with Aalto XML, it is the size of one top-level XML tree.
	 * When the limit is exceeded, {@link DataBufferLimitException} is raised.
	 * <p>By default this is set to 256K.
	 * @param byteCount the max number of bytes to buffer, or -1 for unlimited
	 * @since 5.1.11
	 */
	public void setMaxInMemorySize(int byteCount) {
		this.maxInMemorySize = byteCount;
		this.xmlEventDecoder.setMaxInMemorySize(byteCount);
	}

	/**
	 * Return the {@link #setMaxInMemorySize configured} byte count limit.
	 * @since 5.1.11
	 */
	public int getMaxInMemorySize() {
		return this.maxInMemorySize;
	}


	@Override
	public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
		Class<?> outputClass = elementType.toClass();
		return (outputClass.isAnnotationPresent(XmlRootElement.class) ||
				outputClass.isAnnotationPresent(XmlType.class)) && super.canDecode(elementType, mimeType);
	}

	@Override
	public Flux<Object> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		Flux<XMLEvent> xmlEventFlux = this.xmlEventDecoder.decode(
				inputStream, ResolvableType.forClass(XMLEvent.class), mimeType, hints);

		Class<?> outputClass = elementType.toClass();
		Set<QName> typeNames = Jaxb2Helper.toQNames(outputClass);
		Flux<List<XMLEvent>> splitEvents = Jaxb2Helper.split(xmlEventFlux, typeNames);

		return splitEvents.map(events -> {
			Object value = unmarshal(events, outputClass);
			LogFormatUtils.traceDebug(logger, traceOn -> {
				String formatted = LogFormatUtils.formatValue(value, !traceOn);
				return Hints.getLogPrefix(hints) + "Decoded [" + formatted + "]";
			});
			return value;
		});
	}

	@Override
	public Mono<Object> decodeToMono(Publisher<DataBuffer> input, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return DataBufferUtils.join(input, this.maxInMemorySize)
				.map(dataBuffer -> decode(dataBuffer, elementType, mimeType, hints));
	}

	@Override
	@NonNull
	public Object decode(DataBuffer dataBuffer, ResolvableType targetType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) throws DecodingException {

		try {
			Iterator<Object> eventReader = inputFactory.createXMLEventReader(dataBuffer.asInputStream(),
					encoding(mimeType));
			List<XMLEvent> events = new ArrayList<>();
			eventReader.forEachRemaining(event -> events.add((XMLEvent) event));
			return unmarshal(events, targetType.toClass());
		}
		catch (XMLStreamException ex) {
			throw new DecodingException(ex.getMessage(), ex);
		}
		catch (Throwable ex) {
			Throwable cause = ex.getCause();
			if (cause instanceof XMLStreamException) {
				throw new DecodingException(cause.getMessage(), cause);
			}
			else {
				throw Exceptions.propagate(ex);
			}
		}
		finally {
			DataBufferUtils.release(dataBuffer);
		}
	}

	@Nullable
	private static String encoding(@Nullable MimeType mimeType) {
		if (mimeType == null) {
			return null;
		}
		Charset charset = mimeType.getCharset();
		if (charset == null) {
			return null;
		}
		else {
			return charset.name();
		}
	}

	private Object unmarshal(List<XMLEvent> events, Class<?> outputClass) {
		try {
			Unmarshaller unmarshaller = initUnmarshaller(outputClass);
			XMLEventReader eventReader = StaxUtils.createXMLEventReader(events);
			if (outputClass.isAnnotationPresent(XmlRootElement.class) ||
				outputClass.isAnnotationPresent(XmlSeeAlso.class)) {
				return unmarshaller.unmarshal(eventReader);
			}
			else {
				JAXBElement<?> jaxbElement = unmarshaller.unmarshal(eventReader, outputClass);
				return jaxbElement.getValue();
			}
		}
		catch (UnmarshalException ex) {
			throw new DecodingException("Could not unmarshal XML to " + outputClass, ex);
		}
		catch (JAXBException ex) {
			throw new CodecException("Invalid JAXB configuration", ex);
		}
	}

	private Unmarshaller initUnmarshaller(Class<?> outputClass) throws CodecException, JAXBException {
		Unmarshaller unmarshaller = this.jaxbContexts.createUnmarshaller(outputClass);
		return this.unmarshallerProcessor.apply(unmarshaller);
	}

}
