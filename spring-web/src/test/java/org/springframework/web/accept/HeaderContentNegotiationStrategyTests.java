/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.accept;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link HeaderContentNegotiationStrategy}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class HeaderContentNegotiationStrategyTests {

	private final HeaderContentNegotiationStrategy strategy = new HeaderContentNegotiationStrategy();

	private final MockHttpServletRequest servletRequest = new MockHttpServletRequest();

	private final NativeWebRequest webRequest = new ServletWebRequest(this.servletRequest);


	@Test
	void resolveMediaTypes() throws Exception {
		this.servletRequest.addHeader("Accept", "text/plain; q=0.5, text/html, text/x-dvi; q=0.8, text/x-c");
		List<MediaType> mediaTypes = this.strategy.resolveMediaTypes(this.webRequest);

		assertThat(mediaTypes).map(Object::toString)
				.containsExactly("text/html", "text/x-c", "text/x-dvi;q=0.8", "text/plain;q=0.5");
	}

	@Test  // gh-19075
	void resolveMediaTypesFromMultipleHeaderValues() throws Exception {
		this.servletRequest.addHeader("Accept", "text/plain; q=0.5, text/html");
		this.servletRequest.addHeader("Accept", "text/x-dvi; q=0.8, text/x-c");
		List<MediaType> mediaTypes = this.strategy.resolveMediaTypes(this.webRequest);

		assertThat(mediaTypes).map(Object::toString)
				.containsExactly("text/html", "text/x-c", "text/x-dvi;q=0.8", "text/plain;q=0.5");
	}

	@Test  // gh-32483
	void resolveMediaTypesWithMaxElements() throws Exception {
		String acceptHeaderValue = "text/plain, text/html,".repeat(25);
		this.servletRequest.addHeader("Accept", acceptHeaderValue);
		List<MediaType> mediaTypes = this.strategy.resolveMediaTypes(this.webRequest);

		assertThat(mediaTypes).hasSize(50);
		assertThat(mediaTypes.stream().map(Object::toString).distinct())
				.containsExactly("text/plain", "text/html");
	}

	@Test  // gh-32483
	void resolveMediaTypesWithTooManyElements() {
		String acceptHeaderValue = "text/plain,".repeat(51);
		this.servletRequest.addHeader("Accept", acceptHeaderValue);
		assertThatExceptionOfType(HttpMediaTypeNotAcceptableException.class)
				.isThrownBy(() -> this.strategy.resolveMediaTypes(this.webRequest))
				.withMessageStartingWith("Could not parse 'Accept' header")
				.withMessageEndingWith("Too many elements");
	}

	@Test
	void resolveMediaTypesParseError() {
		this.servletRequest.addHeader("Accept", "textplain; q=0.5");
		assertThatExceptionOfType(HttpMediaTypeNotAcceptableException.class)
				.isThrownBy(() -> this.strategy.resolveMediaTypes(this.webRequest))
				.withMessageStartingWith("Could not parse 'Accept' header")
				.withMessageContaining("Invalid mime type");
	}

}
