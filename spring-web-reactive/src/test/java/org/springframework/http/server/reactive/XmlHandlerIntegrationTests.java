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

package org.springframework.http.server.reactive;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferAllocator;
import org.springframework.core.io.buffer.support.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.boot.ReactorHttpServer;
import org.springframework.http.server.reactive.boot.RxNettyHttpServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

/**
 * @author Arjen Poutsma
 */
public class XmlHandlerIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private final XmlHandler handler = new XmlHandler();

	@Override
	protected HttpHandler createHttpHandler() {
		return handler;
	}

	@Test
	public void xml() throws Exception {
		// TODO: fix Reactor and RxNetty support
		assumeFalse(server instanceof ReactorHttpServer ||
				server instanceof RxNettyHttpServer);

		RestTemplate restTemplate = new RestTemplate();

		Person johnDoe = new Person("John Doe");
		Person janeDoe = new Person("Jane Doe");

		RequestEntity<Person> request = RequestEntity.post(new URI("http://localhost:" + port)).body(
				johnDoe);
		ResponseEntity<Person> response = restTemplate.exchange(request, Person.class);
		assertEquals(janeDoe, response.getBody());

		handler.requestComplete.await(10, TimeUnit.SECONDS);
		if (handler.requestError != null) {
			throw handler.requestError;
		}
		assertEquals(johnDoe, handler.requestPerson);

	}

	private static class XmlHandler implements HttpHandler {

		private CountDownLatch requestComplete = new CountDownLatch(1);

		private Person requestPerson;

		private Exception requestError;


		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			requestError = null;
			try {
				JAXBContext jaxbContext = JAXBContext.newInstance(Person.class);
				Marshaller marshaller = jaxbContext.createMarshaller();

				Runnable r = () -> {
					try {
						InputStream bis =
								DataBufferUtils.toInputStream(request.getBody());

						Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
						requestPerson = (Person) unmarshaller.unmarshal(bis);

					}
					catch (Exception ex) {
						requestError = ex;
					}
					finally {
						requestComplete.countDown();
					}
				};

				Thread t = new Thread(r);
				t.start();

				response.getHeaders().setContentType(MediaType.APPLICATION_XML);
				Person janeDoe = new Person("Jane Doe");

				DataBuffer buffer = new DefaultDataBufferAllocator().allocateBuffer();
				OutputStream bos = buffer.asOutputStream();
				marshaller.marshal(janeDoe, bos);
				bos.close();

				return response.setBody(Flux.just(buffer));
			}
			catch (Exception ex) {
				return Mono.error(ex);
			}
		}
	}

	@XmlRootElement
	private static class Person {

		private String name;

		public Person() {
		}

		public Person(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {
			return name.equals(((Person) o).name);
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public String toString() {
			return name;
		}
	}

}
