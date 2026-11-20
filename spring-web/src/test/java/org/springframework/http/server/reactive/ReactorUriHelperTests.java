/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server.reactive;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import reactor.netty.http.server.HttpServerRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ReactorUriHelper}.
 *
 * @author Arjen Poutsma
 */
class ReactorUriHelperTests {

	private final HttpServerRequest nettyRequest = mock();


	@Test
	void hostnameWithZoneId() throws URISyntaxException {
		given(nettyRequest.scheme()).willReturn("http");
		given(nettyRequest.hostName()).willReturn("fe80::a%en1");
		given(nettyRequest.hostPort()).willReturn(80);
		given(nettyRequest.uri()).willReturn("/");

		URI uri = ReactorUriHelper.createUri(nettyRequest);
		assertThat(uri).hasScheme("http")
				.hasHost("[fe80::a%25en1]")
				.hasPort(-1)
				.hasPath("/")
				.hasToString("http://[fe80::a%25en1]/");
	}

	@Test
	void requestUriWithScheme() throws URISyntaxException {
		given(nettyRequest.scheme()).willReturn("https");
		given(nettyRequest.hostName()).willReturn("example.org");
		given(nettyRequest.hostPort()).willReturn(443);
		given(nettyRequest.uri()).willReturn("https://example.org/path");

		URI uri = ReactorUriHelper.createUri(nettyRequest);
		assertThat(uri).hasScheme("https")
				.hasHost("example.org")
				.hasPort(-1)
				.hasPath("/path")
				.hasToString("https://example.org/path");
	}

	@Test
	void requestUriWithoutLeadingSlash() throws URISyntaxException {
		given(nettyRequest.scheme()).willReturn("https");
		given(nettyRequest.hostName()).willReturn("example.org");
		given(nettyRequest.hostPort()).willReturn(443);
		given(nettyRequest.uri()).willReturn("foo/bar");

		URI uri = ReactorUriHelper.createUri(nettyRequest);
		assertThat(uri).hasScheme("https")
				.hasHost("example.org")
				.hasPort(-1)
				.hasPath("/foo/bar")
				.hasToString("https://example.org/foo/bar");
	}

	@ParameterizedTest(name = "{displayName}({arguments})")
	@CsvSource(delimiter='|', value = {
			"/prefix           | /prefix/",
			"/prefix1/prefix2  | /prefix1/prefix2/",
			"                  | /",
			"''                | /",
	})
	void forwardedPrefix(String forwardedPrefixHeader, String expectedPath) throws URISyntaxException {
		given(nettyRequest.scheme()).willReturn("https");
		given(nettyRequest.hostName()).willReturn("localhost");
		given(nettyRequest.hostPort()).willReturn(443);
		given(nettyRequest.uri()).willReturn("/");
		given(nettyRequest.forwardedPrefix()).willReturn(forwardedPrefixHeader);

		URI uri = ReactorUriHelper.createUri(nettyRequest);
		assertThat(uri).hasScheme("https")
				.hasHost("localhost")
				.hasPort(-1)
				.hasPath(expectedPath)
				.hasToString("https://localhost" + expectedPath);
	}

}
