/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.servlet.function;

import java.io.File;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.test.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class PathResourceLookupFunctionTests {

	@Test
	public void normal() throws Exception {
		ClassPathResource location =
				new ClassPathResource("org/springframework/web/servlet/function/");

		PathResourceLookupFunction function =
				new PathResourceLookupFunction("/resources/**", location);

		MockHttpServletRequest servletRequest =
				new MockHttpServletRequest("GET", "/resources/response.txt");
		ServerRequest request = new DefaultServerRequest(servletRequest, Collections.emptyList());

		Optional<Resource> result = function.apply(request);
		assertThat(result.isPresent()).isTrue();

		File expected = new ClassPathResource("response.txt", getClass()).getFile();
		assertThat(result.get().getFile()).isEqualTo(expected);
	}

	@Test
	public void subPath() throws Exception {
		ClassPathResource location =
				new ClassPathResource("org/springframework/web/servlet/function/");

		PathResourceLookupFunction function =
				new PathResourceLookupFunction("/resources/**", location);

		MockHttpServletRequest servletRequest =
				new MockHttpServletRequest("GET", "/resources/child/response.txt");
		ServerRequest request = new DefaultServerRequest(servletRequest, Collections.emptyList());

		Optional<Resource> result = function.apply(request);
		assertThat(result.isPresent()).isTrue();

		File expected =
				new ClassPathResource("org/springframework/web/servlet/function/child/response.txt")
						.getFile();
		assertThat(result.get().getFile()).isEqualTo(expected);
	}

	@Test
	public void notFound() {
		ClassPathResource location =
				new ClassPathResource("org/springframework/web/reactive/function/server/");

		PathResourceLookupFunction function =
				new PathResourceLookupFunction("/resources/**", location);

		MockHttpServletRequest servletRequest =
				new MockHttpServletRequest("GET", "/resources/foo.txt");
		ServerRequest request = new DefaultServerRequest(servletRequest, Collections.emptyList());

		Optional<Resource> result = function.apply(request);
		assertThat(result.isPresent()).isFalse();
	}

	@Test
	public void composeResourceLookupFunction() throws Exception {
		ClassPathResource defaultResource = new ClassPathResource("response.txt", getClass());

		Function<ServerRequest, Optional<Resource>> lookupFunction =
				new PathResourceLookupFunction("/resources/**",
						new ClassPathResource("org/springframework/web/servlet/function/"));

		Function<ServerRequest, Optional<Resource>> customLookupFunction =
				lookupFunction.andThen((Optional<Resource> optionalResource) -> {
					if (optionalResource.isPresent()) {
						return optionalResource;
					}
					else {
						return Optional.of(defaultResource);
					}
				});

		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/resources/foo");
		ServerRequest request = new DefaultServerRequest(servletRequest, Collections.emptyList());

		Optional<Resource> result = customLookupFunction.apply(request);
		assertThat(result.isPresent()).isTrue();

		assertThat(result.get().getFile()).isEqualTo(defaultResource.getFile());
	}

}
