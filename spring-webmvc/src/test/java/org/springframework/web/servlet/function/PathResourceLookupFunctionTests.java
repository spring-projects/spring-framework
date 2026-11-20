/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.function;

import java.io.File;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.handler.PathPatternsTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author Arjen Poutsma
 */
class PathResourceLookupFunctionTests {

	@Test
	void normal() throws Exception {
		ClassPathResource location = new ClassPathResource("org/springframework/web/servlet/function/");
		PathResourceLookupFunction function = new PathResourceLookupFunction("/resources/**", location);
		ServerRequest request = initRequest("GET", "/resources/response.txt");

		Optional<Resource> result = function.apply(request);
		assertThat(result).isPresent();

		File expected = new ClassPathResource("response.txt", getClass()).getFile();
		assertThat(result.get().getFile()).isEqualTo(expected);
	}

	@Test
	void subPath() throws Exception {
		ClassPathResource location = new ClassPathResource("org/springframework/web/servlet/function/");
		PathResourceLookupFunction function = new PathResourceLookupFunction("/resources/**", location);
		ServerRequest request = initRequest("GET", "/resources/child/response.txt");

		Optional<Resource> result = function.apply(request);
		assertThat(result).isPresent();

		File expected = new ClassPathResource("org/springframework/web/servlet/function/child/response.txt").getFile();
		assertThat(result.get().getFile()).isEqualTo(expected);
	}

	@Test
	void notFound() {
		ClassPathResource location = new ClassPathResource("org/springframework/web/reactive/function/server/");
		PathResourceLookupFunction function = new PathResourceLookupFunction("/resources/**", location);
		ServerRequest request = initRequest("GET", "/resources/foo.txt");

		Optional<Resource> result = function.apply(request);
		assertThat(result).isNotPresent();
	}

	@Test
	void composeResourceLookupFunction() throws Exception {
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

		ServerRequest request = initRequest("GET", "/resources/foo");

		Optional<Resource> result = customLookupFunction.apply(request);
		assertThat(result).isPresent();

		assertThat(result.get().getFile()).isEqualTo(defaultResource.getFile());
	}

	private ServerRequest initRequest(String httpMethod, String requestUri) {
		return new DefaultServerRequest(
				PathPatternsTestUtils.initRequest(httpMethod, requestUri, true),
				Collections.emptyList());
	}

	@Test
	@SuppressWarnings("removal")
	void withPathResource() {
		org.springframework.core.io.PathResource location = new org.springframework.core.io.PathResource("/static/");
		assertThatNoException().isThrownBy(() -> new PathResourceLookupFunction("/resources/**", location));
	}

	@Test
	void withFileSystemResource() {
		FileSystemResource location = new FileSystemResource("/static/");
		assertThatNoException().isThrownBy(() -> new PathResourceLookupFunction("/resources/**", location));
	}

}
