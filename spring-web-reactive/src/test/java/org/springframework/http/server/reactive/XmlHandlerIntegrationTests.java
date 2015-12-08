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

import java.net.URI;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Test;

import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * @author Arjen Poutsma
 */
public class XmlHandlerIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	@Override
	protected HttpHandler createHttpHandler() {
		return new XmlHandler();
	}

	@Test
	public void xml() throws Exception {
		RestTemplate restTemplate = new RestTemplate();

		Person johnDoe = new Person("John Doe");

		RequestEntity<Person> request = RequestEntity.post(new URI("http://localhost:" + port)).body(
				johnDoe);
		ResponseEntity<Person> response = restTemplate.exchange(request, Person.class);
		System.out.println(response.getBody());
	}

	@XmlRootElement
	static class Person {

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
