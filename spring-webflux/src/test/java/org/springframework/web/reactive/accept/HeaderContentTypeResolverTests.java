/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.accept;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link HeaderContentTypeResolver}.
 *
 * @author Rossen Stoyanchev
 */
class HeaderContentTypeResolverTests {

	private final HeaderContentTypeResolver resolver = new HeaderContentTypeResolver();


	@Test
	void resolveMediaTypes() {
		String header = "text/plain; q=0.5, text/html, text/x-dvi; q=0.8, text/x-c";
		List<MediaType> mediaTypes = this.resolver.resolveMediaTypes(
				MockServerWebExchange.from(MockServerHttpRequest.get("/").header("accept", header)));

		assertThat(mediaTypes).hasSize(4);
		assertThat(mediaTypes.get(0).toString()).isEqualTo("text/html");
		assertThat(mediaTypes.get(1).toString()).isEqualTo("text/x-c");
		assertThat(mediaTypes.get(2).toString()).isEqualTo("text/x-dvi;q=0.8");
		assertThat(mediaTypes.get(3).toString()).isEqualTo("text/plain;q=0.5");
	}

	@Test
	void resolveMediaTypesParseError() {
		String header = "textplain; q=0.5";
		assertThatExceptionOfType(NotAcceptableStatusException.class).isThrownBy(() ->
				this.resolver.resolveMediaTypes(
						MockServerWebExchange.from(MockServerHttpRequest.get("/").header("accept", header))));
	}

}
