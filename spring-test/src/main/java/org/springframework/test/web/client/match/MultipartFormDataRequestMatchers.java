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
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.fail;

/**
 * Factory for assertions on the multipart form data parameters. Handles only {@link MediaType#MULTIPART_FORM_DATA}
 *
 * <p>An instance of this class is typically accessed via {@link ContentRequestMatchers#multipart()}
 *
 * @author Valentin Spac
 * @since 5.3
 */
public class MultipartFormDataRequestMatchers {

	public RequestMatcher param(String parameter, String... expectedValues) {
		List<Matcher<? super String>> matcherList = Arrays.stream(expectedValues)
				.map(Matchers::equalTo)
				.collect(Collectors.toList());

		return this.param(parameter, matcherList);
	}

	@SafeVarargs
	@SuppressWarnings("varargs")
	public final RequestMatcher param(String parameter, Matcher<? super String>... matchers) {
		return this.param(parameter, Arrays.stream(matchers).collect(Collectors.toList()));
	}

	public RequestMatcher param(String parameter, Matcher<Iterable<? extends String>> matchers) {
		return request -> {
			Map<String, String[]> requestParams = MultipartRequestParser.parameterMap(request);
			assertValueCount(parameter, requestParams, 1);

			String[] values = requestParams.get(parameter);
			assertThat("Request parameter [" + parameter + "]", Arrays.asList(values), matchers);
		};
	}

	private RequestMatcher param(String parameter, List<Matcher<? super String>> matchers) {
		return request -> {
			Map<String, String[]> requestParams = MultipartRequestParser.parameterMap(request);
			assertValueCount(parameter, requestParams, matchers.size());

			String[] values = requestParams.get(parameter);

			Assert.state(values != null, "No values for request parameter " + parameter);
			for (int i = 0; i < matchers.size(); i++) {
				assertThat("Request parameter [" + parameter + "]", values[i], matchers.get(i));
			}
		};
	}

	public RequestMatcher params(MultiValueMap<String, String> expectedParameters) {
		return request -> {
			Map<String, String[]> requestParams = MultipartRequestParser.parameterMap(request);

			expectedParameters.forEach((param, values) -> {
				String[] actualValues = requestParams.get(param);
				Assert.state(actualValues != null, "No values for request parameter " + param);

				assertValueCount(param, requestParams, values.size());

				assertEquals("Parameter " + param, values, Arrays.asList(actualValues));
			});
		};
	}

	public RequestMatcher file(String parameter, byte[]... resources) {
		return request -> {
			MultiValueMap<String, MultipartFile> files = MultipartRequestParser.multiFileMap(request);

			assertValueCount(parameter, files, resources.length);

			assertByteArrayMatch(parameter, Arrays.asList(resources), files.get(parameter));
		};
	}

	@SafeVarargs
	@SuppressWarnings("varargs")
	public final RequestMatcher file(String parameter, Matcher<? super Resource>... matchers) {
		return request -> {
			MultiValueMap<String, MultipartFile> files = MultipartRequestParser.multiFileMap(request);
			assertValueCount(parameter, files, matchers.length);
			List<MultipartFile> parts = files.get(parameter);

			for (int i = 0; i < matchers.length; i++) {
				assertThat("File [" + parameter + "]", parts.get(i).getResource(), matchers[i]);
			}
		};
	}

	public RequestMatcher file(String parameter, Resource... resources) {
		return request -> {
			MultiValueMap<String, MultipartFile> files = MultipartRequestParser.multiFileMap(request);
			assertValueCount(parameter, files, resources.length);

			assertResourceMatch(parameter, Arrays.asList(resources), files.get(parameter));
		};
	}

	public RequestMatcher files(MultiValueMap<String, Resource> expectedFiles) {
		return request -> {
			MultiValueMap<String, MultipartFile> actualFiles = MultipartRequestParser.multiFileMap(request);

			expectedFiles.forEach((param, parts) -> {
				assertValueCount(param, actualFiles, parts.size());
				assertResourceMatch(param, parts, actualFiles.get(param));
			});
		};
	}


	private void assertByteArrayMatch(String parameterName, List<byte[]> expectedFiles,
									  List<MultipartFile> actualFiles) {
		for (int index = 0; index < actualFiles.size(); index++) {
			MultipartFile multiPartFile = actualFiles.get(index);
			byte[] expectedContent = expectedFiles.get(index);

			try {
				assertEquals("Content mismatch for file " + parameterName, expectedContent,
						multiPartFile.getBytes());
			}
			catch (IOException ex) {
				throw new AssertionError("Could not get bytes from actual multipart files", ex);
			}
		}
	}

	private void assertResourceMatch(String parameterName, List<Resource> expectedFiles,
									List<MultipartFile> actualFiles) {
		for (int index = 0; index < actualFiles.size(); index++) {
			MultipartFile multiPartFile = actualFiles.get(index);
			Resource expectedResource = expectedFiles.get(index);
			try {
				byte[] fileContent = IOUtils.toByteArray(expectedResource.getInputStream());

				assertEquals("Content mismatch for file " + parameterName, fileContent,
						multiPartFile.getBytes());
				assertEquals("Filename ", expectedResource.getFilename(), multiPartFile.getOriginalFilename());
			}
			catch (IOException ex) {
				throw new AssertionError("Could not get bytes from actual multipart files", ex);
			}
		}
	}

	private static void assertValueCount(String parameter, Map<String, String[]> map, int count) {
		String[] values = map.get(parameter);
		if (values == null) {
			fail("Expected <" + parameter + "> to exist but was null");
		}
		assertValueCount(parameter, count, Arrays.asList(values));
	}

	private static void assertValueCount(String parameter, MultiValueMap<String, ?> map, int count) {
		List<?> values = map.get(parameter);
		assertValueCount(parameter, count, values);
	}

	private static void assertValueCount(String parameter, int count, List<?> values) {
		String message = "Expected multipart file  <" + parameter + ">";
		if (count > values.size()) {
			fail(message + " to have at least <" + count + "> values but found " + values.size());
		}
	}


	private static class MultipartRequestParser {
		private static MultipartHttpServletRequest extract(ClientHttpRequest request) {
			MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
			final MockHttpServletRequest mockHttpServletRequest = toMockHttpServletRequest(mockRequest);

			CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver();
			return multipartResolver.resolveMultipart(mockHttpServletRequest);
		}

		@NotNull
		private static MockHttpServletRequest toMockHttpServletRequest(MockClientHttpRequest mockRequest) {
			final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
			mockHttpServletRequest.setContent(mockRequest.getBodyAsBytes());

			// copy headers
			mockRequest.getHeaders()
					.forEach((headerName, headerValue) ->
							headerValue.forEach(value -> mockHttpServletRequest.addHeader(headerName, value)));
			return mockHttpServletRequest;
		}

		private static Map<String, String[]> parameterMap(ClientHttpRequest request) {
			return extract(request).getParameterMap();
		}

		private static MultiValueMap<String, MultipartFile> multiFileMap(ClientHttpRequest request) {
			return extract(request).getMultiFileMap();
		}
	}
}
