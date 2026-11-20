/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
class StatusResultMatchersTests {

	private final StatusResultMatchers matchers = new StatusResultMatchers();

	private final MockHttpServletRequest request = new MockHttpServletRequest();


	@Test
	void httpStatusCodeResultMatchers() throws Exception {
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
	void statusRanges() throws Exception {
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
