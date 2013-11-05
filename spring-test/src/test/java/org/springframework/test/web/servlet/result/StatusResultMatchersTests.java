/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.servlet.result;

import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.core.Conventions;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.StubMvcResult;
import org.springframework.test.web.servlet.result.StatusResultMatchers;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Tests for {@link StatusResultMatchers}.
 *
 * @author Rossen Stoyanchev
 */
public class StatusResultMatchersTests {

	@Test
	public void testHttpStatusCodeResultMatchers() throws Exception {

		StatusResultMatchers resultMatchers = new StatusResultMatchers();

		List<AssertionError> failures = new ArrayList<AssertionError>();

		for(HttpStatus status : HttpStatus.values()) {
			MockHttpServletResponse response = new MockHttpServletResponse();
			response.setStatus(status.value());

			String methodName = statusToMethodName(status);
			Method method = StatusResultMatchers.class.getMethod(methodName);
			try {
				ResultMatcher matcher = (ResultMatcher) ReflectionUtils.invokeMethod(method, resultMatchers);
				try {
					MvcResult mvcResult = new StubMvcResult(new MockHttpServletRequest(), null, null, null, null, null, response);
					matcher.match(mvcResult);
				}
				catch (AssertionError error) {
					failures.add(error);
				}
			}
			catch (Exception ex) {
				throw new Exception("Failed to obtain ResultMatcher: " + method.toString(), ex);
			}
		}

		if (!failures.isEmpty()) {
			fail("Failed status codes: " + failures);
		}
	}

	private String statusToMethodName(HttpStatus status) throws NoSuchMethodException {
		String name = status.name().toLowerCase().replace("_", "-");
		return "is" + StringUtils.capitalize(Conventions.attributeNameToPropertyName(name));
	}

}
