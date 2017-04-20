/*
 * Copyright 2002-2017 the original author or authors.
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

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractSingleValueEncoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * Encode from {@code Object} stream to a byte stream containing XML elements.
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @since 5.0
 * @see Jaxb2XmlDecoder
 */
public class Jaxb2XmlEncoder extends AbstractSingleValueEncoder<Object> {

	private final JaxbContextContainer jaxbContexts = new JaxbContextContainer();


	public Jaxb2XmlEncoder() {
		super(MimeTypeUtils.APPLICATION_XML, MimeTypeUtils.TEXT_XML);
	}


	@Override
	public boolean canEncode(ResolvableType elementType, MimeType mimeType) {
		if (super.canEncode(elementType, mimeType)) {
			Class<?> outputClass = elementType.resolve(Object.class);
			return (outputClass.isAnnotationPresent(XmlRootElement.class) ||
					outputClass.isAnnotationPresent(XmlType.class));
		}
		else {
			return false;
		}

	}

	@Override
	protected Flux<DataBuffer> encode(Object value, DataBufferFactory dataBufferFactory,
			ResolvableType type, MimeType mimeType, Map<String, Object> hints) {
		try {
			DataBuffer buffer = dataBufferFactory.allocateBuffer(1024);
			OutputStream outputStream = buffer.asOutputStream();
			Class<?> clazz = ClassUtils.getUserClass(value);
			Marshaller marshaller = jaxbContexts.createMarshaller(clazz);
			marshaller
					.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
			marshaller.marshal(value, outputStream);
			return Flux.just(buffer);
		}
		catch (JAXBException ex) {
			return Flux.error(ex);
		}
	}

}
