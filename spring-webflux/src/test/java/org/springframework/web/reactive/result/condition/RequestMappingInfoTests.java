/*
 * Copyright 2002-2017 the original author or authors.
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

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PatternParseException;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.web.reactive.result.method.RequestMappingInfo.paths;

/**
 * Unit tests for {@link RequestMappingInfo}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestMappingInfoTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	// TODO: CORS pre-flight (see @Ignore)


	@Test
	public void createEmpty() {
		RequestMappingInfo info = paths().build();

		assertEquals(0, info.getPatternsCondition().getPatterns().size());
		assertEquals(0, info.getMethodsCondition().getMethods().size());
		assertEquals(true, info.getConsumesCondition().isEmpty());
		assertEquals(true, info.getProducesCondition().isEmpty());
		assertNotNull(info.getParamsCondition());
		assertNotNull(info.getHeadersCondition());
		assertNull(info.getCustomCondition());
	}

	@Test
	public void throwWhenInvalidPattern() {
		this.thrown.expect(PatternParseException.class);
		this.thrown.expectMessage("Expected close capture character after variable name }");
		paths("/{foo").build();
	}

	@Test
	public void prependPatternWithSlash() {
		RequestMappingInfo actual = paths("foo").build();
		List<PathPattern> patterns = new ArrayList<>(actual.getPatternsCondition().getPatterns());
		assertEquals(1, patterns.size());
		assertEquals("/foo", patterns.get(0).getPatternString());
	}

	@Test
	public void matchPatternsCondition() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/foo"));

		RequestMappingInfo info = paths("/foo*", "/bar").build();
		RequestMappingInfo expected = paths("/foo*").build();

		assertEquals(expected, info.getMatchingCondition(exchange));

		info = paths("/**", "/foo*", "/foo").build();
		expected = paths("/foo", "/foo*", "/**").build();

		assertEquals(expected, info.getMatchingCondition(exchange));
	}

	@Test
	public void matchParamsCondition() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/foo?foo=bar"));

		RequestMappingInfo info = paths("/foo").params("foo=bar").build();
		RequestMappingInfo match = info.getMatchingCondition(exchange);

		assertNotNull(match);

		info = paths("/foo").params("foo!=bar").build();
		match = info.getMatchingCondition(exchange);

		assertNull(match);
	}

	@Test
	public void matchHeadersCondition() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/foo").header("foo", "bar").build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);

		RequestMappingInfo info = paths("/foo").headers("foo=bar").build();
		RequestMappingInfo match = info.getMatchingCondition(exchange);

		assertNotNull(match);

		info = paths("/foo").headers("foo!=bar").build();
		match = info.getMatchingCondition(exchange);

		assertNull(match);
	}

	@Test
	public void matchConsumesCondition() {
		MockServerHttpRequest request = MockServerHttpRequest.post("/foo").contentType(MediaType.TEXT_PLAIN).build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);

		RequestMappingInfo info = paths("/foo").consumes("text/plain").build();
		RequestMappingInfo match = info.getMatchingCondition(exchange);

		assertNotNull(match);

		info = paths("/foo").consumes("application/xml").build();
		match = info.getMatchingCondition(exchange);

		assertNull(match);
	}

	@Test
	public void matchProducesCondition() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/foo").accept(MediaType.TEXT_PLAIN).build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);

		RequestMappingInfo info = paths("/foo").produces("text/plain").build();
		RequestMappingInfo match = info.getMatchingCondition(exchange);

		assertNotNull(match);

		info = paths("/foo").produces("application/xml").build();
		match = info.getMatchingCondition(exchange);

		assertNull(match);
	}

	@Test
	public void matchCustomCondition() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/foo?foo=bar"));

		RequestMappingInfo info = paths("/foo").params("foo=bar").build();
		RequestMappingInfo match = info.getMatchingCondition(exchange);

		assertNotNull(match);

		info = paths("/foo").params("foo!=bar")
				.customCondition(new ParamsRequestCondition("foo!=bar")).build();

		match = info.getMatchingCondition(exchange);

		assertNull(match);
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

		assertEquals(oneMethodOneParam, list.get(0));
		assertEquals(oneMethod, list.get(1));
		assertEquals(none, list.get(2));
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

		assertEquals(info1, info2);
		assertEquals(info1.hashCode(), info2.hashCode());

		info2 = paths("/foo", "/NOOOOOO").methods(RequestMethod.GET)
				.params("foo=bar").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.customCondition(new ParamsRequestCondition("customFoo=customBar"))
				.build();

		assertFalse(info1.equals(info2));
		assertNotEquals(info1.hashCode(), info2.hashCode());

		info2 = paths("/foo").methods(RequestMethod.GET, RequestMethod.POST)
				.params("foo=bar").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.customCondition(new ParamsRequestCondition("customFoo=customBar"))
				.build();

		assertFalse(info1.equals(info2));
		assertNotEquals(info1.hashCode(), info2.hashCode());

		info2 = paths("/foo").methods(RequestMethod.GET)
				.params("/NOOOOOO").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.customCondition(new ParamsRequestCondition("customFoo=customBar"))
				.build();

		assertFalse(info1.equals(info2));
		assertNotEquals(info1.hashCode(), info2.hashCode());

		info2 = paths("/foo").methods(RequestMethod.GET)
				.params("foo=bar").headers("/NOOOOOO")
				.consumes("text/plain").produces("text/plain")
				.customCondition(new ParamsRequestCondition("customFoo=customBar"))
				.build();

		assertFalse(info1.equals(info2));
		assertNotEquals(info1.hashCode(), info2.hashCode());

		info2 = paths("/foo").methods(RequestMethod.GET)
				.params("foo=bar").headers("foo=bar")
				.consumes("text/NOOOOOO").produces("text/plain")
				.customCondition(new ParamsRequestCondition("customFoo=customBar"))
				.build();

		assertFalse(info1.equals(info2));
		assertNotEquals(info1.hashCode(), info2.hashCode());

		info2 = paths("/foo").methods(RequestMethod.GET)
				.params("foo=bar").headers("foo=bar")
				.consumes("text/plain").produces("text/NOOOOOO")
				.customCondition(new ParamsRequestCondition("customFoo=customBar"))
				.build();

		assertFalse(info1.equals(info2));
		assertNotEquals(info1.hashCode(), info2.hashCode());

		info2 = paths("/foo").methods(RequestMethod.GET)
				.params("foo=bar").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.customCondition(new ParamsRequestCondition("customFoo=NOOOOOO"))
				.build();

		assertFalse(info1.equals(info2));
		assertNotEquals(info1.hashCode(), info2.hashCode());
	}

	@Test
	@Ignore
	public void preFlightRequest() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.options("/foo")
				.header("Origin", "https://domain.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "POST")
				);

		RequestMappingInfo info = paths("/foo").methods(RequestMethod.POST).build();
		RequestMappingInfo match = info.getMatchingCondition(exchange);
		assertNotNull(match);

		info = paths("/foo").methods(RequestMethod.OPTIONS).build();
		match = info.getMatchingCondition(exchange);
		assertNull("Pre-flight should match the ACCESS_CONTROL_REQUEST_METHOD", match);
	}

}
