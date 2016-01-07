/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.http.server.reactive;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.Mono;
import reactor.io.buffer.Buffer;
import reactor.rx.Streams;

import org.springframework.http.MediaType;
import org.springframework.util.BufferOutputStream;
import org.springframework.util.ByteBufferPublisherInputStream;

import static org.junit.Assert.fail;

/**
 * @author Arjen Poutsma
 */
public class XmlHandler implements HttpHandler {

	private static final Log logger = LogFactory.getLog(XmlHandler.class);

	@Override
	public Mono<Void> handle(ServerHttpRequest request,
			ServerHttpResponse response) {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(XmlHandlerIntegrationTests.Person.class);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			Marshaller marshaller = jaxbContext.createMarshaller();

			Runnable r = () -> {
				try {
					ByteBufferPublisherInputStream bis = new ByteBufferPublisherInputStream(request.getBody());

					XmlHandlerIntegrationTests.Person johnDoe =
						(XmlHandlerIntegrationTests.Person) unmarshaller.unmarshal(bis);

					logger.info("Read: " + johnDoe);

				}
				catch (Exception e) {
					logger.error(e, e);
				}
			};

			Thread t = new Thread(r);
			t.start();

			response.getHeaders().setContentType(MediaType.APPLICATION_XML);
			XmlHandlerIntegrationTests.Person janeDoe = new XmlHandlerIntegrationTests.Person("Jane Doe");
			Buffer buffer = new Buffer();
			BufferOutputStream bos = new BufferOutputStream(buffer);
			marshaller.marshal(janeDoe, bos);
			bos.close();
			buffer.flip();

			return response.setBody(Streams.just(buffer.byteBuffer()));
		}
		catch (Exception ex) {
			logger.error(ex, ex);
			fail(ex.getMessage());
			return null;
		}
	}
}
