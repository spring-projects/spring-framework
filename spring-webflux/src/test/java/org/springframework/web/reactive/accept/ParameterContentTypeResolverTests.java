/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.accept;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ParameterContentTypeResolver}.
 *
 * @author Rossen Stoyanchev
 */
class ParameterContentTypeResolverTests {

	@Test
	void noKey() {
		ParameterContentTypeResolver resolver = new ParameterContentTypeResolver(Collections.emptyMap());
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		List<MediaType> mediaTypes = resolver.resolveMediaTypes(exchange);

		assertThat(mediaTypes).isEqualTo(RequestedContentTypeResolver.MEDIA_TYPE_ALL_LIST);
	}

	@Test
	void noMatchForKey() {
		ParameterContentTypeResolver resolver = new ParameterContentTypeResolver(Collections.emptyMap());
		assertThatExceptionOfType(NotAcceptableStatusException.class).isThrownBy(() ->
				resolver.resolveMediaTypes(createExchange("blah")));
	}

	@Test
	void noMatchForInvalidKey() {
		ParameterContentTypeResolver resolver = new ParameterContentTypeResolver(Collections.emptyMap());
		assertThatExceptionOfType(NotAcceptableStatusException.class).isThrownBy(() ->
				resolver.resolveMediaTypes(createExchange("not.json")));
	}

	@Test
	void resolveRegisteredKeyWithDot() {
		Map<String, MediaType> mapping = Collections.singletonMap("custom.json", MediaType.APPLICATION_JSON);
		ParameterContentTypeResolver resolver = new ParameterContentTypeResolver(mapping);
		List<MediaType> mediaTypes = resolver.resolveMediaTypes(createExchange("custom.json"));

		assertThat(mediaTypes).containsExactly(MediaType.APPLICATION_JSON);
	}

	@Test
	void resolveKeyFromRegistrations() {
		ServerWebExchange exchange = createExchange("html");

		Map<String, MediaType> mapping = Collections.emptyMap();
		RequestedContentTypeResolver resolver = new ParameterContentTypeResolver(mapping);
		List<MediaType> mediaTypes = resolver.resolveMediaTypes(exchange);
		assertThat(mediaTypes).containsExactly(new MediaType("text", "html"));

		mapping = Collections.singletonMap("HTML", MediaType.APPLICATION_XHTML_XML);
		resolver = new ParameterContentTypeResolver(mapping);
		mediaTypes = resolver.resolveMediaTypes(exchange);
		assertThat(mediaTypes).containsExactly(new MediaType("application", "xhtml+xml"));
	}

	@Test
	void resolveKeyThroughMediaTypeFactory() {
		ServerWebExchange exchange = createExchange("xls");
		RequestedContentTypeResolver resolver = new ParameterContentTypeResolver(Collections.emptyMap());
		List<MediaType> mediaTypes = resolver.resolveMediaTypes(exchange);

		assertThat(mediaTypes).containsExactly(new MediaType("application", "vnd.ms-excel"));
	}

	@Test
	void resolveKeyThroughMediaTypeFactoryDoesNotAddMapping() {
		Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		ParameterContentTypeResolver resolver = new ParameterContentTypeResolver(mapping);

		assertThat(resolver.resolveMediaTypes(createExchange("xls")))
				.containsExactly(new MediaType("application", "vnd.ms-excel"));
		assertThatExceptionOfType(NotAcceptableStatusException.class)
				.isThrownBy(() -> resolver.resolveMediaTypes(createExchange("blah")))
				.satisfies(ex -> assertThat(ex.getSupportedMediaTypes()).containsExactly(MediaType.APPLICATION_JSON));
	}

	@Test // SPR-13747
	void resolveKeyIsCaseInsensitive() {
		ServerWebExchange exchange = createExchange("JSoN");
		Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		ParameterContentTypeResolver resolver = new ParameterContentTypeResolver(mapping);
		List<MediaType> mediaTypes = resolver.resolveMediaTypes(exchange);

		assertThat(mediaTypes).containsExactly(MediaType.APPLICATION_JSON);
	}

	private MockServerWebExchange createExchange(String format) {
		return MockServerWebExchange.from(MockServerHttpRequest.get("/path?format=" + format));
	}

}
