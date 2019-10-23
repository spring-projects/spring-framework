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

package org.springframework.web.servlet.mvc.method;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.HEAD;
import static org.springframework.web.servlet.mvc.method.RequestMappingInfo.paths;

/**
 * Test fixture for {@link RequestMappingInfo} tests.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestMappingInfoTests {

	@Test
	public void createEmpty() {
		RequestMappingInfo info = paths().build();

		// gh-22543
		assertThat(info.getPatternsCondition().getPatterns()).isEqualTo(Collections.singleton(""));
		assertThat(info.getMethodsCondition().getMethods().size()).isEqualTo(0);
		assertThat(info.getConsumesCondition().isEmpty()).isEqualTo(true);
		assertThat(info.getProducesCondition().isEmpty()).isEqualTo(true);
		assertThat(info.getParamsCondition()).isNotNull();
		assertThat(info.getHeadersCondition()).isNotNull();
		assertThat(info.getCustomCondition()).isNull();
	}

	@Test
	public void matchPatternsCondition() {
		HttpServletRequest request = new MockHttpServletRequest("GET", "/foo");

		RequestMappingInfo info = paths("/foo*", "/bar").build();
		RequestMappingInfo expected = paths("/foo*").build();

		assertThat(info.getMatchingCondition(request)).isEqualTo(expected);

		info = paths("/**", "/foo*", "/foo").build();
		expected = paths("/foo", "/foo*", "/**").build();

		assertThat(info.getMatchingCondition(request)).isEqualTo(expected);
	}

	@Test
	public void matchParamsCondition() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setParameter("foo", "bar");

		RequestMappingInfo info = paths("/foo").params("foo=bar").build();
		RequestMappingInfo match = info.getMatchingCondition(request);

		assertThat(match).isNotNull();

		info = paths("/foo").params("foo!=bar").build();
		match = info.getMatchingCondition(request);

		assertThat(match).isNull();
	}

	@Test
	public void matchHeadersCondition() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.addHeader("foo", "bar");

		RequestMappingInfo info = paths("/foo").headers("foo=bar").build();
		RequestMappingInfo match = info.getMatchingCondition(request);

		assertThat(match).isNotNull();

		info = paths("/foo").headers("foo!=bar").build();
		match = info.getMatchingCondition(request);

		assertThat(match).isNull();
	}

	@Test
	public void matchConsumesCondition() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setContentType("text/plain");

		RequestMappingInfo info = paths("/foo").consumes("text/plain").build();
		RequestMappingInfo match = info.getMatchingCondition(request);

		assertThat(match).isNotNull();

		info = paths("/foo").consumes("application/xml").build();
		match = info.getMatchingCondition(request);

		assertThat(match).isNull();
	}

	@Test
	public void matchProducesCondition() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.addHeader("Accept", "text/plain");

		RequestMappingInfo info = paths("/foo").produces("text/plain").build();
		RequestMappingInfo match = info.getMatchingCondition(request);

		assertThat(match).isNotNull();

		info = paths("/foo").produces("application/xml").build();
		match = info.getMatchingCondition(request);

		assertThat(match).isNull();
	}

	@Test
	public void matchCustomCondition() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setParameter("foo", "bar");

		RequestMappingInfo info = paths("/foo").params("foo=bar").build();
		RequestMappingInfo match = info.getMatchingCondition(request);

		assertThat(match).isNotNull();

		info = paths("/foo").params("foo!=bar").params("foo!=bar").build();
		match = info.getMatchingCondition(request);

		assertThat(match).isNull();
	}

	@Test
	public void compareToWithImpicitVsExplicitHttpMethodDeclaration() {
		RequestMappingInfo noMethods = paths().build();
		RequestMappingInfo oneMethod = paths().methods(GET).build();
		RequestMappingInfo oneMethodOneParam = paths().methods(GET).params("foo").build();

		Comparator<RequestMappingInfo> comparator =
				(info, otherInfo) -> info.compareTo(otherInfo, new MockHttpServletRequest());

		List<RequestMappingInfo> list = asList(noMethods, oneMethod, oneMethodOneParam);
		Collections.shuffle(list);
		Collections.sort(list, comparator);

		assertThat(list.get(0)).isEqualTo(oneMethodOneParam);
		assertThat(list.get(1)).isEqualTo(oneMethod);
		assertThat(list.get(2)).isEqualTo(noMethods);
	}

	@Test // SPR-14383
	public void compareToWithHttpHeadMapping() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("HEAD");
		request.addHeader("Accept", "application/json");

		RequestMappingInfo noMethods = paths().build();
		RequestMappingInfo getMethod = paths().methods(GET).produces("application/json").build();
		RequestMappingInfo headMethod = paths().methods(HEAD).build();

		Comparator<RequestMappingInfo> comparator = (info, otherInfo) -> info.compareTo(otherInfo, request);

		List<RequestMappingInfo> list = asList(noMethods, getMethod, headMethod);
		Collections.shuffle(list);
		Collections.sort(list, comparator);

		assertThat(list.get(0)).isEqualTo(headMethod);
		assertThat(list.get(1)).isEqualTo(getMethod);
		assertThat(list.get(2)).isEqualTo(noMethods);
	}

	@Test
	public void equals() {
		RequestMappingInfo info1 = paths("/foo").methods(GET)
				.params("foo=bar", "customFoo=customBar").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.build();

		RequestMappingInfo info2 = paths("/foo").methods(GET)
				.params("foo=bar", "customFoo=customBar").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.build();

		assertThat(info2).isEqualTo(info1);
		assertThat(info2.hashCode()).isEqualTo(info1.hashCode());

		info2 = paths("/foo", "/NOOOOOO").methods(GET)
				.params("foo=bar", "customFoo=customBar").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.build();

		assertThat(info1.equals(info2)).isFalse();
		assertThat(info2.hashCode()).isNotEqualTo((long) info1.hashCode());

		info2 = paths("/foo").methods(GET, RequestMethod.POST)
				.params("foo=bar", "customFoo=customBar").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.build();

		assertThat(info1.equals(info2)).isFalse();
		assertThat(info2.hashCode()).isNotEqualTo((long) info1.hashCode());

		info2 = paths("/foo").methods(GET)
				.params("/NOOOOOO", "customFoo=customBar").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.build();

		assertThat(info1.equals(info2)).isFalse();
		assertThat(info2.hashCode()).isNotEqualTo((long) info1.hashCode());

		info2 = paths("/foo").methods(GET)
				.params("foo=bar", "customFoo=customBar").headers("/NOOOOOO")
				.consumes("text/plain").produces("text/plain")
				.build();

		assertThat(info1.equals(info2)).isFalse();
		assertThat(info2.hashCode()).isNotEqualTo((long) info1.hashCode());

		info2 = paths("/foo").methods(GET)
				.params("foo=bar", "customFoo=customBar").headers("foo=bar")
				.consumes("text/NOOOOOO").produces("text/plain")
				.build();

		assertThat(info1.equals(info2)).isFalse();
		assertThat(info2.hashCode()).isNotEqualTo((long) info1.hashCode());

		info2 = paths("/foo").methods(GET)
				.params("foo=bar", "customFoo=customBar").headers("foo=bar")
				.consumes("text/plain").produces("text/NOOOOOO")
				.build();

		assertThat(info1.equals(info2)).isFalse();
		assertThat(info2.hashCode()).isNotEqualTo((long) info1.hashCode());

		info2 = paths("/foo").methods(GET)
				.params("foo=bar", "customFoo=NOOOOOO").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.build();

		assertThat(info1.equals(info2)).isFalse();
		assertThat(info2.hashCode()).isNotEqualTo((long) info1.hashCode());
	}

	@Test
	public void preFlightRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/foo");
		request.addHeader(HttpHeaders.ORIGIN, "https://domain.com");
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");

		RequestMappingInfo info = paths("/foo").methods(RequestMethod.POST).build();
		RequestMappingInfo match = info.getMatchingCondition(request);
		assertThat(match).isNotNull();

		info = paths("/foo").methods(RequestMethod.OPTIONS).build();
		match = info.getMatchingCondition(request);
		assertThat(match).as("Pre-flight should match the ACCESS_CONTROL_REQUEST_METHOD").isNull();
	}

}
