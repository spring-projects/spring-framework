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

package org.springframework.http.codec.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.XMLEvent;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.support.AbstractDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.xml.StaxUtils2;

/**
 * Decode from a bytes stream of XML elements to a stream of {@code Object} (POJO).
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @see Jaxb2Encoder
 */
public class Jaxb2Decoder extends AbstractDecoder<Object> {

	/**
	 * The default value for JAXB annotations.
	 * @see XmlRootElement#name()
	 * @see XmlRootElement#namespace()
	 * @see XmlType#name()
	 * @see XmlType#namespace()
	 */
	private final static String JAXB_DEFAULT_ANNOTATION_VALUE = "##default";

	private final XmlEventDecoder xmlEventDecoder = new XmlEventDecoder();

	private final JaxbContextContainer jaxbContexts = new JaxbContextContainer();

	public Jaxb2Decoder() {
		super(MimeTypeUtils.APPLICATION_XML, MimeTypeUtils.TEXT_XML);
	}

	@Override
	public boolean canDecode(ResolvableType elementType, MimeType mimeType, Object... hints) {
		if (super.canDecode(elementType, mimeType, hints)) {
			Class<?> outputClass = elementType.getRawClass();
			return outputClass.isAnnotationPresent(XmlRootElement.class) ||
					outputClass.isAnnotationPresent(XmlType.class);
		}
		else {
			return false;
		}
	}

	@Override
	public Flux<Object> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
			MimeType mimeType, Object... hints) {
		Class<?> outputClass = elementType.getRawClass();
		Flux<XMLEvent> xmlEventFlux =
				this.xmlEventDecoder.decode(inputStream, null, mimeType);

		QName typeName = toQName(outputClass);
		Flux<List<XMLEvent>> splitEvents = split(xmlEventFlux, typeName);

		return splitEvents.map(events -> unmarshal(events, outputClass));
	}

	/**
	 * Returns the qualified name for the given class, according to the mapping rules
	 * in the JAXB specification.
	 */
	QName toQName(Class<?> outputClass) {
		String localPart;
		String namespaceUri;

		if (outputClass.isAnnotationPresent(XmlRootElement.class)) {
			XmlRootElement annotation = outputClass.getAnnotation(XmlRootElement.class);
			localPart = annotation.name();
			namespaceUri = annotation.namespace();
		}
		else if (outputClass.isAnnotationPresent(XmlType.class)) {
			XmlType annotation = outputClass.getAnnotation(XmlType.class);
			localPart = annotation.name();
			namespaceUri = annotation.namespace();
		}
		else {
			throw new IllegalArgumentException("Outputclass [" + outputClass + "] is " +
					"neither annotated with @XmlRootElement nor @XmlType");
		}

		if (JAXB_DEFAULT_ANNOTATION_VALUE.equals(localPart)) {
			localPart = ClassUtils.getShortNameAsProperty(outputClass);
		}
		if (JAXB_DEFAULT_ANNOTATION_VALUE.equals(namespaceUri)) {
			Package outputClassPackage = outputClass.getPackage();
			if (outputClassPackage != null &&
					outputClassPackage.isAnnotationPresent(XmlSchema.class)) {
				XmlSchema annotation = outputClassPackage.getAnnotation(XmlSchema.class);
				namespaceUri = annotation.namespace();
			}
			else {
				namespaceUri = XMLConstants.NULL_NS_URI;
			}
		}
		return new QName(namespaceUri, localPart);
	}

	/**
	 * Split a flux of {@link XMLEvent}s into a flux of XMLEvent lists, one list for each
	 * branch of the tree that starts with the given qualified name.
	 * That is, given the XMLEvents shown
	 * {@linkplain XmlEventDecoder here},
	 * and the {@code desiredName} "{@code child}", this method
	 * returns a flux of two lists, each of which containing the events of a particular
	 * branch of the tree that starts with "{@code child}".
	 * <ol>
	 * <li>The first list, dealing with the first branch of the tree
	 * <ol>
	 * <li>{@link javax.xml.stream.events.StartElement} {@code child}</li>
	 * <li>{@link javax.xml.stream.events.Characters} {@code foo}</li>
	 * <li>{@link javax.xml.stream.events.EndElement} {@code child}</li>
	 * </ol>
	 * <li>The second list, dealing with the second branch of the tree
	 * <ol>
	 * <li>{@link javax.xml.stream.events.StartElement} {@code child}</li>
	 * <li>{@link javax.xml.stream.events.Characters} {@code bar}</li>
	 * <li>{@link javax.xml.stream.events.EndElement} {@code child}</li>
	 * </ol>
	 * </li>
	 * </ol>
	 */
	Flux<List<XMLEvent>> split(Flux<XMLEvent> xmlEventFlux, QName desiredName) {
		return xmlEventFlux
				.flatMap(new Function<XMLEvent, Publisher<? extends List<XMLEvent>>>() {

					private List<XMLEvent> events = null;

					private int elementDepth = 0;

					private int barrier = Integer.MAX_VALUE;

					@Override
					public Publisher<? extends List<XMLEvent>> apply(XMLEvent event) {
						if (event.isStartElement()) {
							if (this.barrier == Integer.MAX_VALUE) {
								QName startElementName = event.asStartElement().getName();
								if (desiredName.equals(startElementName)) {
									this.events = new ArrayList<XMLEvent>();
									this.barrier = this.elementDepth;
								}
							}
							this.elementDepth++;
						}
						if (this.elementDepth > this.barrier) {
							this.events.add(event);
						}
						if (event.isEndElement()) {
							this.elementDepth--;
							if (this.elementDepth == this.barrier) {
								this.barrier = Integer.MAX_VALUE;
								return Mono.just(this.events);
							}
						}
						return Mono.empty();
					}
				});
	}

	private Object unmarshal(List<XMLEvent> events, Class<?> outputClass) {
		try {
			Unmarshaller unmarshaller = this.jaxbContexts.createUnmarshaller(outputClass);
			XMLEventReader eventReader = StaxUtils2.createXMLEventReader(events);
			if (outputClass.isAnnotationPresent(XmlRootElement.class)) {
				return unmarshaller.unmarshal(eventReader);
			}
			else {
				JAXBElement<?> jaxbElement =
						unmarshaller.unmarshal(eventReader, outputClass);
				return jaxbElement.getValue();
			}
		}
		catch (JAXBException ex) {
			throw new CodecException(ex.getMessage(), ex);
		}
	}

}
