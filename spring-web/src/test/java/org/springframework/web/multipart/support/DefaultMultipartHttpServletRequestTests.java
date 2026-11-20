/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.multipart.support;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultMultipartHttpServletRequest}.
 *
 * @author Rossen Stoyanchev
 */
class DefaultMultipartHttpServletRequestTests {

	private final MockHttpServletRequest servletRequest = new MockHttpServletRequest();

	private final Map<String, String[]> multipartParams = new LinkedHashMap<>();

	private final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();


	@Test // SPR-16590
	void parameterValues() {

		this.multipartParams.put("key", new String[] {"p"});
		this.queryParams.add("key", "q");

		String[] values = createMultipartRequest().getParameterValues("key");

		assertThat(values).isEqualTo(new String[] {"p", "q"});
	}

	@Test // SPR-16590
	void parameterMap() {

		this.multipartParams.put("key1", new String[] {"p1"});
		this.multipartParams.put("key2", new String[] {"p2"});

		this.queryParams.add("key1", "q1");
		this.queryParams.add("key3", "q3");

		Map<String, String[]> map = createMultipartRequest().getParameterMap();

		assertThat(map).hasSize(3);
		assertThat(map.get("key1")).isEqualTo(new String[] {"p1", "q1"});
		assertThat(map.get("key2")).isEqualTo(new String[] {"p2"});
		assertThat(map.get("key3")).isEqualTo(new String[] {"q3"});
	}

	private DefaultMultipartHttpServletRequest createMultipartRequest() {
		insertQueryParams();
		return new DefaultMultipartHttpServletRequest(this.servletRequest, new LinkedMultiValueMap<>(),
				this.multipartParams, new HashMap<>());
	}

	private void insertQueryParams() {
		StringBuilder query = new StringBuilder();
		for (String key : this.queryParams.keySet()) {
			for (String value : this.queryParams.get(key)) {
				this.servletRequest.addParameter(key, value);
				query.append(query.length() > 0 ? "&" : "").append(key).append('=').append(value);
			}
		}
		this.servletRequest.setQueryString(query.toString());
	}

}
