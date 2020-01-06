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

package org.springframework.web.reactive.result.condition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PatternParseException;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.web.reactive.result.method.RequestMappingInfo.paths;

/**
 * Unit tests for {@link RequestMappingInfo}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestMappingInfoTests {

	// TODO: CORS pre-flight (see @Disabled)


	@Test
	public void createEmpty() {
		RequestMappingInfo info = paths().build();

		PathPattern emptyPattern = (new PathPatternParser()).parse("");
		assertThat(info.getPatternsCondition().getPatterns()).isEqualTo(Collections.singleton(emptyPattern));
		assertThat(info.getMethodsCondition().getMethods().size()).isEqualTo(0);
		assertThat(info.getConsumesCondition().isEmpty()).isEqualTo(true);
		assertThat(info.getProducesCondition().isEmpty()).isEqualTo(true);
		assertThat(info.getParamsCondition()).isNotNull();
		assertThat(info.getHeadersCondition()).isNotNull();
		assertThat(info.getCustomCondition()).isNull();
	}

	@Test
	public void throwWhenInvalidPattern() {
		assertThatExceptionOfType(PatternParseException.class).isThrownBy(() ->
				paths("/{foo").build())
			.withMessageContaining("Expected close capture character after variable name }");
	}

	@Test
	public void prependPatternWithSlash() {
		RequestMappingInfo actual = paths("foo").build();
		List<PathPattern> patterns = new ArrayList<>(actual.getPatternsCondition().getPatterns());
		assertThat(patterns.size()).isEqualTo(1);
		assertThat(patterns.get(0).getPatternString()).isEqualTo("/foo");
	}

	@Test
	public void matchPatternsCondition() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/foo"));

		RequestMappingInfo info = paths("/foo*", "/bar").build();
		RequestMappingInfo expected = paths("/foo*").build();

		assertThat(info.getMatchingCondition(exchange)).isEqualTo(expected);

		info = paths("/**", "/foo*", "/foo").build();
		expected = paths("/foo", "/foo*", "/**").build();

		assertThat(info.getMatchingCondition(exchange)).isEqualTo(expected);
	}

	@Test
	public void matchParamsCondition() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/foo?foo=bar"));

		RequestMappingInfo info = paths("/foo").params("foo=bar").build();
		RequestMappingInfo match = info.getMatchingCondition(exchange);

		assertThat(match).isNotNull();

		info = paths("/foo").params("foo!=bar").build();
		match = info.getMatchingCondition(exchange);

		assertThat(match).isNull();
	}

	@Test
	public void matchHeadersCondition() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/foo").header("foo", "bar").build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);

		RequestMappingInfo info = paths("/foo").headers("foo=bar").build();
		RequestMappingInfo match = info.getMatchingCondition(exchange);

		assertThat(match).isNotNull();

		info = paths("/foo").headers("foo!=bar").build();
		match = info.getMatchingCondition(exchange);

		assertThat(match).isNull();
	}

	@Test
	public void matchConsumesCondition() {
		MockServerHttpRequest request = MockServerHttpRequest.post("/foo").contentType(MediaType.TEXT_PLAIN).build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);

		RequestMappingInfo info = paths("/foo").consumes("text/plain").build();
		RequestMappingInfo match = info.getMatchingCondition(exchange);

		assertThat(match).isNotNull();

		info = paths("/foo").consumes("application/xml").build();
		match = info.getMatchingCondition(exchange);

		assertThat(match).isNull();
	}

	@Test
	public void matchProducesCondition() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/foo").accept(MediaType.TEXT_PLAIN).build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);

		RequestMappingInfo info = paths("/foo").produces("text/plain").build();
		RequestMappingInfo match = info.getMatchingCondition(exchange);

		assertThat(match).isNotNull();

		info = paths("/foo").produces("application/xml").build();
		match = info.getMatchingCondition(exchange);

		assertThat(match).isNull();
	}

	@Test
	public void matchCustomCondition() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/foo?foo=bar"));

		RequestMappingInfo info = paths("/foo").params("foo=bar").build();
		RequestMappingInfo match = info.getMatchingCondition(exchange);

		assertThat(match).isNotNull();

		info = paths("/foo").params("foo!=bar")
				.customCondition(new ParamsRequestCondition("foo!=bar")).build();

		match = info.getMatchingCondition(exchange);

		assertThat(match).isNull();
	}

	@Test
	public void compareTwoHttpMethodsOneParam() {
		RequestMappingInfo none = paths().build();
		RequestMappingInfo oneMethod = paths().methods(RequestMethod.GET).build();
		RequestMappingInfo oneMethodOneParam = paths().methods(RequestMethod.GET).params("foo").build();

		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/foo"));
		Comparator<RequestMappingInfo> comparator = (info, otherInfo) -> info.compareTo(otherInfo, exchange);

		List<RequestMappingInfo> list = asList(none, oneMethod, oneMethodOneParam);
		Collections.shuffle(list);
		list.sort(comparator);

		assertThat(list.get(0)).isEqualTo(oneMethodOneParam);
		assertThat(list.get(1)).isEqualTo(oneMethod);
		assertThat(list.get(2)).isEqualTo(none);
	}

	@Test
	public void equals() {
		RequestMappingInfo info1 = paths("/foo").methods(RequestMethod.GET)
				.params("foo=bar").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.customCondition(new ParamsRequestCondition("customFoo=customBar"))
				.build();

		RequestMappingInfo info2 = paths("/foo").methods(RequestMethod.GET)
				.params("foo=bar").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.customCondition(new ParamsRequestCondition("customFoo=customBar"))
				.build();

		assertThat(info2).isEqualTo(info1);
		assertThat(info2.hashCode()).isEqualTo(info1.hashCode());

		info2 = paths("/foo", "/NOOOOOO").methods(RequestMethod.GET)
				.params("foo=bar").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.customCondition(new ParamsRequestCondition("customFoo=customBar"))
				.build();

		assertThat(info1.equals(info2)).isFalse();
		assertThat(info2.hashCode()).isNotEqualTo((long) info1.hashCode());

		info2 = paths("/foo").methods(RequestMethod.GET, RequestMethod.POST)
				.params("foo=bar").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.customCondition(new ParamsRequestCondition("customFoo=customBar"))
				.build();

		assertThat(info1.equals(info2)).isFalse();
		assertThat(info2.hashCode()).isNotEqualTo((long) info1.hashCode());

		info2 = paths("/foo").methods(RequestMethod.GET)
				.params("/NOOOOOO").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.customCondition(new ParamsRequestCondition("customFoo=customBar"))
				.build();

		assertThat(info1.equals(info2)).isFalse();
		assertThat(info2.hashCode()).isNotEqualTo((long) info1.hashCode());

		info2 = paths("/foo").methods(RequestMethod.GET)
				.params("foo=bar").headers("/NOOOOOO")
				.consumes("text/plain").produces("text/plain")
				.customCondition(new ParamsRequestCondition("customFoo=customBar"))
				.build();

		assertThat(info1.equals(info2)).isFalse();
		assertThat(info2.hashCode()).isNotEqualTo((long) info1.hashCode());

		info2 = paths("/foo").methods(RequestMethod.GET)
				.params("foo=bar").headers("foo=bar")
				.consumes("text/NOOOOOO").produces("text/plain")
				.customCondition(new ParamsRequestCondition("customFoo=customBar"))
				.build();

		assertThat(info1.equals(info2)).isFalse();
		assertThat(info2.hashCode()).isNotEqualTo((long) info1.hashCode());

		info2 = paths("/foo").methods(RequestMethod.GET)
				.params("foo=bar").headers("foo=bar")
				.consumes("text/plain").produces("text/NOOOOOO")
				.customCondition(new ParamsRequestCondition("customFoo=customBar"))
				.build();

		assertThat(info1.equals(info2)).isFalse();
		assertThat(info2.hashCode()).isNotEqualTo((long) info1.hashCode());

		info2 = paths("/foo").methods(RequestMethod.GET)
				.params("foo=bar").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.customCondition(new ParamsRequestCondition("customFoo=NOOOOOO"))
				.build();

		assertThat(info1.equals(info2)).isFalse();
		assertThat(info2.hashCode()).isNotEqualTo((long) info1.hashCode());
	}

	@Test
	@Disabled
	public void preFlightRequest() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.options("/foo")
				.header("Origin", "https://domain.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "POST")
				);

		RequestMappingInfo info = paths("/foo").methods(RequestMethod.POST).build();
		RequestMappingInfo match = info.getMatchingCondition(exchange);
		assertThat(match).isNotNull();

		info = paths("/foo").methods(RequestMethod.OPTIONS).build();
		match = info.getMatchingCondition(exchange);
		assertThat(match).as("Pre-flight should match the ACCESS_CONTROL_REQUEST_METHOD").isNull();
	}

}
