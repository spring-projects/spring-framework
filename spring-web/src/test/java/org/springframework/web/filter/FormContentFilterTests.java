/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.filter;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.web.testfixture.servlet.MockFilterChain;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for {@link FormContentFilter}.
 *
 * @author Rossen Stoyanchev
 */
class FormContentFilterTests {

	private final FormContentFilter filter = new FormContentFilter();

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private MockFilterChain filterChain;


	@BeforeEach
	void setup() {
		this.request = new MockHttpServletRequest("PUT", "/");
		this.request.setContentType("application/x-www-form-urlencoded; charset=ISO-8859-1");
		this.response = new MockHttpServletResponse();
		this.filterChain = new MockFilterChain();
	}


	@Test
	void wrapPutPatchAndDeleteOnly() throws Exception {
		for (HttpMethod method : HttpMethod.values()) {
			MockHttpServletRequest request = new MockHttpServletRequest(method.name(), "/");
			request.setContent("foo=bar".getBytes(StandardCharsets.ISO_8859_1));
			request.setContentType("application/x-www-form-urlencoded; charset=ISO-8859-1");
			this.filterChain = new MockFilterChain();
			this.filter.doFilter(request, this.response, this.filterChain);
			if (method == HttpMethod.PUT || method == HttpMethod.PATCH || method == HttpMethod.DELETE) {
				assertThat(this.filterChain.getRequest()).isNotSameAs(request);
			}
			else {
				assertThat(this.filterChain.getRequest()).isSameAs(request);
			}
		}
	}

	@Test
	void wrapFormEncodedOnly() throws Exception {
		String[] contentTypes = new String[] {"text/plain", "multipart/form-data"};
		for (String contentType : contentTypes) {
			MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/");
			request.setContent("".getBytes(StandardCharsets.ISO_8859_1));
			request.setContentType(contentType);
			this.filterChain = new MockFilterChain();
			this.filter.doFilter(request, this.response, this.filterChain);
			assertThat(this.filterChain.getRequest()).isSameAs(request);
		}
	}

	@Test
	void invalidMediaType() throws Exception {
		this.request.setContent("".getBytes(StandardCharsets.ISO_8859_1));
		this.request.setContentType("foo");
		this.filterChain = new MockFilterChain();
		this.filter.doFilter(this.request, this.response, this.filterChain);
		assertThat(this.filterChain.getRequest()).isSameAs(this.request);
	}

	@Test
	void getParameter() throws Exception {
		this.request.setContent("name=value".getBytes(StandardCharsets.ISO_8859_1));
		this.filter.doFilter(this.request, this.response, this.filterChain);

		assertThat(this.filterChain.getRequest().getParameter("name")).isEqualTo("value");
	}

	@Test
	void getParameterFromQueryString() throws Exception {
		this.request.addParameter("name", "value1");
		this.request.setContent("name=value2".getBytes(StandardCharsets.ISO_8859_1));
		this.filter.doFilter(this.request, this.response, this.filterChain);

		assertThat(this.filterChain.getRequest()).as("Request not wrapped").isNotSameAs(this.request);
		assertThat(this.filterChain.getRequest().getParameter("name")).as("Query string parameters should be listed ahead of form parameters").isEqualTo("value1");
	}

	@Test
	void getParameterNullValue() throws Exception {
		this.request.setContent("name=value".getBytes(StandardCharsets.ISO_8859_1));
		this.filter.doFilter(this.request, this.response, this.filterChain);

		assertThat(this.filterChain.getRequest()).as("Request not wrapped").isNotSameAs(this.request);
		assertThat(this.filterChain.getRequest().getParameter("noSuchParam")).isNull();
	}

	@Test
	void getParameterNames() throws Exception {
		this.request.addParameter("name1", "value1");
		this.request.addParameter("name2", "value2");
		this.request.setContent("name1=value1&name3=value3&name4=value4".getBytes(StandardCharsets.ISO_8859_1));

		this.filter.doFilter(this.request, this.response, this.filterChain);
		List<String> names = Collections.list(this.filterChain.getRequest().getParameterNames());

		assertThat(this.filterChain.getRequest()).as("Request not wrapped").isNotSameAs(this.request);
		assertThat(names).isEqualTo(Arrays.asList("name1", "name2", "name3", "name4"));
	}

	@Test
	void getParameterValues() throws Exception {
		this.request.setQueryString("name=value1&name=value2");
		this.request.addParameter("name", "value1");
		this.request.addParameter("name", "value2");
		this.request.setContent("name=value3&name=value4".getBytes(StandardCharsets.ISO_8859_1));

		this.filter.doFilter(this.request, this.response, this.filterChain);
		String[] values = this.filterChain.getRequest().getParameterValues("name");

		assertThat(filterChain.getRequest()).as("Request not wrapped").isNotSameAs(this.request);
		assertThat(values).isEqualTo(new String[] {"value1", "value2", "value3", "value4"});
	}

	@Test
	void getParameterValuesFromQueryString() throws Exception {
		this.request.setQueryString("name=value1&name=value2");
		this.request.addParameter("name", "value1");
		this.request.addParameter("name", "value2");
		this.request.setContent("anotherName=anotherValue".getBytes(StandardCharsets.ISO_8859_1));

		this.filter.doFilter(this.request, this.response, this.filterChain);
		String[] values = this.filterChain.getRequest().getParameterValues("name");

		assertThat(this.filterChain.getRequest()).as("Request not wrapped").isNotSameAs(this.request);
		assertThat(values).isEqualTo(new String[] {"value1", "value2"});
	}

	@Test
	void getParameterValuesFromFormContent() throws Exception {
		this.request.addParameter("name", "value1");
		this.request.addParameter("name", "value2");
		this.request.setContent("anotherName=anotherValue".getBytes(StandardCharsets.ISO_8859_1));

		this.filter.doFilter(this.request, this.response, this.filterChain);
		String[] values = this.filterChain.getRequest().getParameterValues("anotherName");

		assertThat(this.filterChain.getRequest()).as("Request not wrapped").isNotSameAs(this.request);
		assertThat(values).isEqualTo(new String[] {"anotherValue"});
	}

	@Test
	void getParameterValuesInvalidName() throws Exception {
		this.request.addParameter("name", "value1");
		this.request.addParameter("name", "value2");
		this.request.setContent("anotherName=anotherValue".getBytes(StandardCharsets.ISO_8859_1));

		this.filter.doFilter(this.request, this.response, this.filterChain);
		String[] values = this.filterChain.getRequest().getParameterValues("noSuchParameter");

		assertThat(this.filterChain.getRequest()).as("Request not wrapped").isNotSameAs(this.request);
		assertThat(values).isNull();
	}

	@Test
	void getParameterMap() throws Exception {
		this.request.setQueryString("name=value1&name=value2");
		this.request.addParameter("name", "value1");
		this.request.addParameter("name", "value2");
		this.request.setContent("name=value3&name4=value4".getBytes(StandardCharsets.ISO_8859_1));

		this.filter.doFilter(this.request, this.response, this.filterChain);
		Map<String, String[]> parameters = this.filterChain.getRequest().getParameterMap();

		assertThat(this.filterChain.getRequest()).as("Request not wrapped").isNotSameAs(this.request);
		assertThat(parameters).hasSize(2);
		assertThat(parameters.get("name")).isEqualTo(new String[] {"value1", "value2", "value3"});
		assertThat(parameters.get("name4")).isEqualTo(new String[] {"value4"});
	}

	@Test  // SPR-15835
	public void hiddenHttpMethodFilterFollowedByHttpPutFormContentFilter() throws Exception {
		this.request.addParameter("_method", "PUT");
		this.request.addParameter("hiddenField", "testHidden");
		this.filter.doFilter(this.request, this.response, this.filterChain);

		assertThat(this.filterChain.getRequest().getParameterValues("hiddenField")).isEqualTo(new String[] {"testHidden"});
	}

}
