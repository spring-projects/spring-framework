/*
 * Copyright 2002-2023 the original author or authors.
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
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.handler.PathPatternsParameterizedTest;
import org.springframework.web.servlet.handler.PathPatternsTestUtils;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.util.pattern.PathPatternParser;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.HEAD;

/**
 * Test fixture for {@link RequestMappingInfo} tests.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
class RequestMappingInfoTests {

	@SuppressWarnings("unused")
	static Stream<RequestMappingInfo.Builder> pathPatternsArguments() {
		RequestMappingInfo.BuilderConfiguration config = new RequestMappingInfo.BuilderConfiguration();
		config.setPatternParser(new PathPatternParser());
		return Stream.of(RequestMappingInfo.paths().options(config), RequestMappingInfo.paths());
	}


	@PathPatternsParameterizedTest
	void createEmpty(RequestMappingInfo.Builder infoBuilder) {

		// gh-22543
		RequestMappingInfo info = infoBuilder.build();
		assertThat(info.getPatternValues()).isEqualTo(Collections.singleton(""));
		assertThat(info.getMethodsCondition().getMethods()).isEmpty();
		assertThat(info.getParamsCondition()).isNotNull();
		assertThat(info.getHeadersCondition()).isNotNull();
		assertThat(info.getConsumesCondition().isEmpty()).isTrue();
		assertThat(info.getProducesCondition().isEmpty()).isTrue();
		assertThat(info.getCustomCondition()).isNull();

		RequestMappingInfo anotherInfo = infoBuilder.build();
		assertThat(info.getActivePatternsCondition()).isSameAs(anotherInfo.getActivePatternsCondition());
		assertThat(info.getPatternsCondition()).isSameAs(anotherInfo.getPatternsCondition());
		assertThat(info.getMethodsCondition()).isSameAs(anotherInfo.getMethodsCondition());
		assertThat(info.getParamsCondition()).isSameAs(anotherInfo.getParamsCondition());
		assertThat(info.getHeadersCondition()).isSameAs(anotherInfo.getHeadersCondition());
		assertThat(info.getConsumesCondition()).isSameAs(anotherInfo.getConsumesCondition());
		assertThat(info.getProducesCondition()).isSameAs(anotherInfo.getProducesCondition());
		assertThat(info.getCustomCondition()).isSameAs(anotherInfo.getCustomCondition());

		RequestMappingInfo result = info.combine(anotherInfo);
		assertThat(result.getPatternValues()).containsExactly("", "/");
		assertThat(info.getMethodsCondition()).isSameAs(result.getMethodsCondition());
		assertThat(info.getParamsCondition()).isSameAs(result.getParamsCondition());
		assertThat(info.getHeadersCondition()).isSameAs(result.getHeadersCondition());
		assertThat(info.getConsumesCondition()).isSameAs(result.getConsumesCondition());
		assertThat(info.getProducesCondition()).isSameAs(result.getProducesCondition());
		assertThat(info.getCustomCondition()).isSameAs(result.getCustomCondition());
	}

	@PathPatternsParameterizedTest
	void matchPatternsCondition(RequestMappingInfo.Builder builder) {

		boolean useParsedPatterns = builder.build().getPathPatternsCondition() != null;
		HttpServletRequest request = PathPatternsTestUtils.initRequest("GET", "/foo", useParsedPatterns);

		RequestMappingInfo info = builder.paths("/foo*", "/bar").build();
		RequestMappingInfo expected = builder.paths("/foo*").build();

		assertThat(info.getMatchingCondition(request)).isEqualTo(expected);

		info = builder.paths("/**", "/foo*", "/foo").build();
		expected = builder.paths("/foo", "/foo*", "/**").build();

		assertThat(info.getMatchingCondition(request)).isEqualTo(expected);
	}

	@Test
	void matchParamsCondition() {
		MockHttpServletRequest request = PathPatternsTestUtils.initRequest("GET", "/foo", false);
		request.setParameter("foo", "bar");

		RequestMappingInfo info = RequestMappingInfo.paths("/foo").params("foo=bar").build();
		RequestMappingInfo match = info.getMatchingCondition(request);

		assertThat(match).isNotNull();

		info = RequestMappingInfo.paths("/foo").params("foo!=bar").build();
		match = info.getMatchingCondition(request);

		assertThat(match).isNull();
	}

	@Test
	void matchHeadersCondition() {
		MockHttpServletRequest request = PathPatternsTestUtils.initRequest("GET", "/foo", false);
		request.addHeader("foo", "bar");

		RequestMappingInfo info = RequestMappingInfo.paths("/foo").headers("foo=bar").build();
		RequestMappingInfo match = info.getMatchingCondition(request);

		assertThat(match).isNotNull();

		info = RequestMappingInfo.paths("/foo").headers("foo!=bar").build();
		match = info.getMatchingCondition(request);

		assertThat(match).isNull();
	}

	@Test
	void matchConsumesCondition() {
		MockHttpServletRequest request = PathPatternsTestUtils.initRequest("GET", "/foo", false);
		request.setContentType("text/plain");

		RequestMappingInfo info = RequestMappingInfo.paths("/foo").consumes("text/plain").build();
		RequestMappingInfo match = info.getMatchingCondition(request);

		assertThat(match).isNotNull();

		info = RequestMappingInfo.paths("/foo").consumes("application/xml").build();
		match = info.getMatchingCondition(request);

		assertThat(match).isNull();
	}

	@Test
	void matchProducesCondition() {
		MockHttpServletRequest request = PathPatternsTestUtils.initRequest("GET", "/foo", false);
		request.addHeader("Accept", "text/plain");

		RequestMappingInfo info = RequestMappingInfo.paths("/foo").produces("text/plain").build();
		RequestMappingInfo match = info.getMatchingCondition(request);

		assertThat(match).isNotNull();

		info = RequestMappingInfo.paths("/foo").produces("application/xml").build();
		match = info.getMatchingCondition(request);

		assertThat(match).isNull();
	}

	@Test
	void matchCustomCondition() {
		MockHttpServletRequest request = PathPatternsTestUtils.initRequest("GET", "/foo", false);
		request.setParameter("foo", "bar");

		RequestMappingInfo info = RequestMappingInfo.paths("/foo").params("foo=bar").build();
		RequestMappingInfo match = info.getMatchingCondition(request);

		assertThat(match).isNotNull();

		info = RequestMappingInfo.paths("/foo").params("foo!=bar").params("foo!=bar").build();
		match = info.getMatchingCondition(request);

		assertThat(match).isNull();
	}

	@Test
	void compareToWithImplicitVsExplicitHttpMethodDeclaration() {
		RequestMappingInfo noMethods = RequestMappingInfo.paths().build();
		RequestMappingInfo oneMethod = RequestMappingInfo.paths().methods(GET).build();
		RequestMappingInfo oneMethodOneParam = RequestMappingInfo.paths().methods(GET).params("foo").build();

		MockHttpServletRequest request = PathPatternsTestUtils.initRequest("GET", "/", false);
		Comparator<RequestMappingInfo> comparator = (info, otherInfo) -> info.compareTo(otherInfo, request);

		List<RequestMappingInfo> list = asList(noMethods, oneMethod, oneMethodOneParam);
		Collections.shuffle(list);
		list.sort(comparator);

		assertThat(list.get(0)).isEqualTo(oneMethodOneParam);
		assertThat(list.get(1)).isEqualTo(oneMethod);
		assertThat(list.get(2)).isEqualTo(noMethods);
	}

	@Test
		// SPR-14383
	void compareToWithHttpHeadMapping() {
		MockHttpServletRequest request = PathPatternsTestUtils.initRequest("GET", "/", false);
		request.setMethod("HEAD");
		request.addHeader("Accept", "application/json");

		RequestMappingInfo noMethods = RequestMappingInfo.paths().build();
		RequestMappingInfo getMethod = RequestMappingInfo.paths().methods(GET).produces("application/json").build();
		RequestMappingInfo headMethod = RequestMappingInfo.paths().methods(HEAD).build();

		Comparator<RequestMappingInfo> comparator = (info, otherInfo) -> info.compareTo(otherInfo, request);

		List<RequestMappingInfo> list = asList(noMethods, getMethod, headMethod);
		Collections.shuffle(list);
		list.sort(comparator);

		assertThat(list.get(0)).isEqualTo(headMethod);
		assertThat(list.get(1)).isEqualTo(getMethod);
		assertThat(list.get(2)).isEqualTo(noMethods);
	}

	@PathPatternsParameterizedTest
	void equalsMethod(RequestMappingInfo.Builder infoBuilder) {
		RequestMappingInfo info1 = infoBuilder.paths("/foo").methods(GET)
				.params("foo=bar", "customFoo=customBar").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.build();

		RequestMappingInfo info2 = infoBuilder.paths("/foo").methods(GET)
				.params("foo=bar", "customFoo=customBar").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.build();

		assertThat(info2).isEqualTo(info1);
		assertThat(info2.hashCode()).isEqualTo(info1.hashCode());

		info2 = infoBuilder.paths("/foo", "/NOOOOOO").methods(GET)
				.params("foo=bar", "customFoo=customBar").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.build();

		assertThat(info1.equals(info2)).isFalse();
		assertThat(info2.hashCode()).isNotEqualTo(info1.hashCode());

		info2 = infoBuilder.paths("/foo").methods(GET, RequestMethod.POST)
				.params("foo=bar", "customFoo=customBar").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.build();

		assertThat(info1.equals(info2)).isFalse();
		assertThat(info2.hashCode()).isNotEqualTo(info1.hashCode());

		info2 = infoBuilder.paths("/foo").methods(GET)
				.params("/NOOOOOO", "customFoo=customBar").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.build();

		assertThat(info1.equals(info2)).isFalse();
		assertThat(info2.hashCode()).isNotEqualTo(info1.hashCode());

		info2 = infoBuilder.paths("/foo").methods(GET)
				.params("foo=bar", "customFoo=customBar").headers("/NOOOOOO")
				.consumes("text/plain").produces("text/plain")
				.build();

		assertThat(info1.equals(info2)).isFalse();
		assertThat(info2.hashCode()).isNotEqualTo(info1.hashCode());

		info2 = infoBuilder.paths("/foo").methods(GET)
				.params("foo=bar", "customFoo=customBar").headers("foo=bar")
				.consumes("text/NOOOOOO").produces("text/plain")
				.build();

		assertThat(info1.equals(info2)).isFalse();
		assertThat(info2.hashCode()).isNotEqualTo(info1.hashCode());

		info2 = infoBuilder.paths("/foo").methods(GET)
				.params("foo=bar", "customFoo=customBar").headers("foo=bar")
				.consumes("text/plain").produces("text/NOOOOOO")
				.build();

		assertThat(info1.equals(info2)).isFalse();
		assertThat(info2.hashCode()).isNotEqualTo(info1.hashCode());

		info2 = infoBuilder.paths("/foo").methods(GET)
				.params("foo=bar", "customFoo=NOOOOOO").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.build();

		assertThat(info1.equals(info2)).isFalse();
		assertThat(info2.hashCode()).isNotEqualTo(info1.hashCode());
	}

	@Test
	void preFlightRequest() {
		MockHttpServletRequest request = PathPatternsTestUtils.initRequest("OPTIONS", "/foo", false);
		request.addHeader(HttpHeaders.ORIGIN, "https://domain.com");
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");

		RequestMappingInfo info = RequestMappingInfo.paths("/foo").methods(RequestMethod.POST).build();
		RequestMappingInfo match = info.getMatchingCondition(request);
		assertThat(match).isNotNull();

		info = RequestMappingInfo.paths("/foo").methods(RequestMethod.OPTIONS).build();
		match = info.getMatchingCondition(request);
		assertThat(match).as("Pre-flight should match the ACCESS_CONTROL_REQUEST_METHOD").isNull();
	}

	@Test
	void mutate() {
		RequestMappingInfo.BuilderConfiguration options = new RequestMappingInfo.BuilderConfiguration();
		options.setPatternParser(new PathPatternParser());

		RequestMappingInfo info1 = RequestMappingInfo.paths("/foo")
				.methods(GET).headers("h1=hv1").params("q1=qv1")
				.consumes("application/json").produces("application/json")
				.mappingName("testMapping").options(options)
				.build();

		RequestMappingInfo info2 = info1.mutate().produces("application/hal+json").build();

		assertThat(info2.getName()).isEqualTo(info1.getName());
		assertThat(info2.getPatternsCondition()).isNull();
		assertThat(info2.getPathPatternsCondition()).isEqualTo(info1.getPathPatternsCondition());
		assertThat(info2.getHeadersCondition()).isEqualTo(info1.getHeadersCondition());
		assertThat(info2.getParamsCondition()).isEqualTo(info1.getParamsCondition());
		assertThat(info2.getConsumesCondition()).isEqualTo(info1.getConsumesCondition());
		assertThat(info2.getProducesCondition().getProducibleMediaTypes())
				.containsOnly(MediaType.parseMediaType("application/hal+json"));
	}

}
