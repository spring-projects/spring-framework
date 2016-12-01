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

package org.springframework.web.reactive.function;

import java.net.URI;
import java.util.Optional;

import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Arjen Poutsma
 */
public class PathResourceLookupFunctionTests {

	@Test
	public void normal() throws Exception {
		ClassPathResource location = new ClassPathResource("org/springframework/web/reactive/function/");

		PathResourceLookupFunction function = new PathResourceLookupFunction("/resources/**", location);
		MockServerRequest<Void> request = MockServerRequest.builder()
				.uri(new URI("http://localhost/resources/response.txt"))
				.build();
		Optional<Resource> result = function.apply(request);
		assertTrue(result.isPresent());

		ClassPathResource expected = new ClassPathResource("response.txt", getClass());
		assertEquals(expected.getFile(), result.get().getFile());
	}

	@Test
	public void subPath() throws Exception {
		ClassPathResource location = new ClassPathResource("org/springframework/web/reactive/function/");

		PathResourceLookupFunction function = new PathResourceLookupFunction("/resources/**", location);
		MockServerRequest<Void> request = MockServerRequest.builder()
				.uri(new URI("http://localhost/resources/child/response.txt"))
				.build();
		Optional<Resource> result = function.apply(request);
		assertTrue(result.isPresent());

		ClassPathResource expected = new ClassPathResource("org/springframework/web/reactive/function/child/response.txt");
		assertEquals(expected.getFile(), result.get().getFile());
	}

	@Test
	public void notFound() throws Exception {
		ClassPathResource location = new ClassPathResource("org/springframework/web/reactive/function/");

		PathResourceLookupFunction function = new PathResourceLookupFunction("/resources/**", location);
		MockServerRequest<Void> request = MockServerRequest.builder()
				.uri(new URI("http://localhost/resources/foo"))
				.build();
		Optional<Resource> result = function.apply(request);
		assertFalse(result.isPresent());
	}

}