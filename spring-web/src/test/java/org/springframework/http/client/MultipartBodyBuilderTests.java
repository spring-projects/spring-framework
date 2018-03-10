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

package org.springframework.http.client;

import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class MultipartBodyBuilderTests {

	@Test
	public void builder() throws Exception {
		MultiValueMap<String, String> multipartData = new LinkedMultiValueMap<>();
		multipartData.add("form field", "form value");
		Resource logo = new ClassPathResource("/org/springframework/http/converter/logo.jpg");
		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.add("foo", "bar");
		HttpEntity<String> entity = new HttpEntity<>("body", entityHeaders);
		Publisher<String> publisher = Flux.just("foo", "bar", "baz");

		MultipartBodyBuilder builder = new MultipartBodyBuilder();
		builder.part("key", multipartData).header("foo", "bar");
		builder.part("logo", logo).header("baz", "qux");
		builder.part("entity", entity).header("baz", "qux");
		builder.asyncPart("publisherClass", publisher, String.class).header("baz", "qux");
		builder.asyncPart("publisherPtr", publisher, new ParameterizedTypeReference<String>() {}).header("baz", "qux");

		MultiValueMap<String, HttpEntity<?>> result = builder.build();

		assertEquals(5, result.size());
		assertNotNull(result.getFirst("key"));
		assertEquals(multipartData, result.getFirst("key").getBody());
		assertEquals("bar", result.getFirst("key").getHeaders().getFirst("foo"));

		assertNotNull(result.getFirst("logo"));
		assertEquals(logo, result.getFirst("logo").getBody());
		assertEquals("qux", result.getFirst("logo").getHeaders().getFirst("baz"));

		assertNotNull(result.getFirst("entity"));
		assertEquals("body", result.getFirst("entity").getBody());
		assertEquals("bar", result.getFirst("entity").getHeaders().getFirst("foo"));
		assertEquals("qux", result.getFirst("entity").getHeaders().getFirst("baz"));

		assertNotNull(result.getFirst("publisherClass"));
		assertEquals(publisher, result.getFirst("publisherClass").getBody());
		assertEquals(ResolvableType.forClass(String.class), ((MultipartBodyBuilder.PublisherEntity<?,?>) result.getFirst("publisherClass")).getResolvableType());
		assertEquals("qux", result.getFirst("publisherClass").getHeaders().getFirst("baz"));

		assertNotNull(result.getFirst("publisherPtr"));
		assertEquals(publisher, result.getFirst("publisherPtr").getBody());
		assertEquals(ResolvableType.forClass(String.class), ((MultipartBodyBuilder.PublisherEntity<?,?>) result.getFirst("publisherPtr")).getResolvableType());
		assertEquals("qux", result.getFirst("publisherPtr").getHeaders().getFirst("baz"));
	}


}