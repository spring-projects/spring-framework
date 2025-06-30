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

package org.springframework.test.web.servlet.result;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.Conventions;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.StubMvcResult;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link StatusResultMatchers}.
 *
 * @author Rossen Stoyanchev
 */
public class StatusResultMatchersTests {

	private final StatusResultMatchers matchers = new StatusResultMatchers();

	private final MockHttpServletRequest request = new MockHttpServletRequest();


	@Test
	public void testHttpStatusCodeResultMatchers() throws Exception {
		List<AssertionError> failures = new ArrayList<>();
		for (HttpStatus status : HttpStatus.values()) {
			MockHttpServletResponse response = new MockHttpServletResponse();
			response.setStatus(status.value());
			MvcResult mvcResult = new StubMvcResult(request, null, null, null, null, null, response);
			try {
				Method method = getMethodForHttpStatus(status);
				ResultMatcher matcher = (ResultMatcher) ReflectionUtils.invokeMethod(method, this.matchers);
				try {
					matcher.match(mvcResult);
				}
				catch (AssertionError error) {
					failures.add(error);
				}
			}
			catch (Exception ex) {
				throw new Exception("Failed to obtain ResultMatcher for status " + status, ex);
			}
		}
		if (!failures.isEmpty()) {
			fail("Failed status codes: " + failures);
		}
	}

	private Method getMethodForHttpStatus(HttpStatus status) throws NoSuchMethodException {
		String name = status.name().toLowerCase().replace("_", "-");
		name = "is" + StringUtils.capitalize(Conventions.attributeNameToPropertyName(name));
		return StatusResultMatchers.class.getMethod(name);
	}

	@Test
	public void statusRanges() throws Exception {
		for (HttpStatus status : HttpStatus.values()) {
			MockHttpServletResponse response = new MockHttpServletResponse();
			response.setStatus(status.value());
			MvcResult mvcResult = new StubMvcResult(request, null, null, null, null, null, response);
			switch (status.series().value()) {
				case 1 -> this.matchers.is1xxInformational().match(mvcResult);
				case 2 -> this.matchers.is2xxSuccessful().match(mvcResult);
				case 3 -> this.matchers.is3xxRedirection().match(mvcResult);
				case 4 -> this.matchers.is4xxClientError().match(mvcResult);
				case 5 -> this.matchers.is5xxServerError().match(mvcResult);
				default -> fail("Unexpected range for status code value " + status);
			}
		}
	}

}
