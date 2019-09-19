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

package org.springframework.test.web.client.match;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.fail;

/**
 * Factory for assertions on the form data parameters.
 *
 * <p>An instance of this class is typically accessed via {@link MockRestRequestMatchers#formData()}
 *
 * @author Valentin Spac
 * @since 5.2
 */
public class FormDataRequestMatchers {


	public RequestMatcher value(String parameter, String... expectedValues) {
		List<Matcher<? super String>> matcherList = Arrays.stream(expectedValues)
				.map(Matchers::equalTo)
				.collect(Collectors.toList());

		return this.value(parameter, matcherList);
	}

	@SafeVarargs
	@SuppressWarnings("varargs")
	public final RequestMatcher value(String parameter, Matcher<? super String>... matchers) {
		return this.value(parameter, Arrays.stream(matchers).collect(Collectors.toList()));
	}

	public RequestMatcher value(String parameter, Matcher<Iterable<? extends String>> matchers) {
		return new AbstractFormDataRequestMatcher() {
			@Override
			protected void matchInternal(MultiValueMap<String, String> requestParams) {
				assertValueCount(parameter, requestParams, 1);

				List<String> values = requestParams.get(parameter);
				assertThat("Request parameter [" + parameter + "]", values, matchers);
			}
		};
	}


	private RequestMatcher value(String parameter, List<Matcher<? super String>> matchers) {
		return new AbstractFormDataRequestMatcher() {
			@Override
			protected void matchInternal(MultiValueMap<String, String> requestParams) {
				assertValueCount(parameter, requestParams, matchers.size());

				List<String> values = requestParams.get(parameter);

				Assert.state(values != null, "No request parameter values");
				for (int i = 0; i < matchers.size(); i++) {
					assertThat("Request parameter [" + parameter + "]", values.get(i), matchers.get(i));
				}
			}
		};
	}

	private static void assertValueCount(String value, MultiValueMap<String, String> map, int count) {
		List<String> values = map.get(value);
		String message = "Expected request parameter <" + value + ">";
		if (values == null) {
			fail(message + " to exist but was null");
		}
		if (count > values.size()) {
			fail(message + " to have at least <" + count + "> values but found " + values);
		}
	}

	private abstract static class AbstractFormDataRequestMatcher implements RequestMatcher {
		private FormHttpMessageConverter formHttpMessageConverter;

		AbstractFormDataRequestMatcher() {
			this.formHttpMessageConverter = new FormHttpMessageConverter();
		}

		@Override
		public final void match(ClientHttpRequest request) throws IOException, AssertionError {
			MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
			MockHttpInputMessage mockHttpInputMessage = new MockHttpInputMessage(mockRequest.getBodyAsBytes());

			MultiValueMap<String, String> requestParams = this.formHttpMessageConverter.read(null, mockHttpInputMessage);

			matchInternal(requestParams);
		}

		abstract void matchInternal(MultiValueMap<String, String> requestParams) throws IOException;
	}

}
