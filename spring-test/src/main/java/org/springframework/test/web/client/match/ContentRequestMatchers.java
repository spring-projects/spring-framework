/*
 * Copyright 2002-2021 the original author or authors.
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
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.hamcrest.Matcher;
import org.w3c.dom.Node;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.JsonExpectationsHelper;
import org.springframework.test.util.XmlExpectationsHelper;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Factory for request content {@code RequestMatcher}'s. An instance of this
 * class is typically accessed via {@link MockRestRequestMatchers#content()}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class ContentRequestMatchers {

	private final XmlExpectationsHelper xmlHelper;

	private final JsonExpectationsHelper jsonHelper;


	/**
	 * Class constructor, not for direct instantiation.
	 * Use {@link MockRestRequestMatchers#content()}.
	 */
	protected ContentRequestMatchers() {
		this.xmlHelper = new XmlExpectationsHelper();
		this.jsonHelper = new JsonExpectationsHelper();
	}


	/**
	 * Assert the request content type as a String.
	 */
	public RequestMatcher contentType(String expectedContentType) {
		return contentType(MediaType.parseMediaType(expectedContentType));
	}

	/**
	 * Assert the request content type as a {@link MediaType}.
	 */
	public RequestMatcher contentType(MediaType expectedContentType) {
		return request -> {
			MediaType actualContentType = request.getHeaders().getContentType();
			assertTrue("Content type not set", actualContentType != null);
			assertEquals("Content type", expectedContentType, actualContentType);
		};
	}

	/**
	 * Assert the request content type is compatible with the given
	 * content type as defined by {@link MediaType#isCompatibleWith(MediaType)}.
	 */
	public RequestMatcher contentTypeCompatibleWith(String contentType) {
		return contentTypeCompatibleWith(MediaType.parseMediaType(contentType));
	}

	/**
	 * Assert the request content type is compatible with the given
	 * content type as defined by {@link MediaType#isCompatibleWith(MediaType)}.
	 */
	public RequestMatcher contentTypeCompatibleWith(MediaType contentType) {
		return request -> {
			MediaType actualContentType = request.getHeaders().getContentType();
			assertTrue("Content type not set", actualContentType != null);
			if (actualContentType != null) {
				assertTrue("Content type [" + actualContentType + "] is not compatible with [" + contentType + "]",
						actualContentType.isCompatibleWith(contentType));
			}
		};
	}

	/**
	 * Get the body of the request as a UTF-8 string and apply the given {@link Matcher}.
	 */
	public RequestMatcher string(Matcher<? super String> matcher) {
		return request -> {
			MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
			assertThat("Request content", mockRequest.getBodyAsString(), matcher);
		};
	}

	/**
	 * Get the body of the request as a UTF-8 string and compare it to the given String.
	 */
	public RequestMatcher string(String expectedContent) {
		return request -> {
			MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
			assertEquals("Request content", expectedContent, mockRequest.getBodyAsString());
		};
	}

	/**
	 * Compare the body of the request to the given byte array.
	 */
	public RequestMatcher bytes(byte[] expectedContent) {
		return request -> {
			MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
			assertEquals("Request content", expectedContent, mockRequest.getBodyAsBytes());
		};
	}

	/**
	 * Parse the body as form data and compare to the given {@code MultiValueMap}.
	 * @since 4.3
	 */
	public RequestMatcher formData(MultiValueMap<String, String> expected) {
		return formData(expected, true);
	}

	/**
	 * Variant of {@link #formData(MultiValueMap)} that matches the given subset
	 * of expected form parameters.
	 * @since 5.3
	 */
	public RequestMatcher formDataContains(Map<String, String> expected) {
		MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>(expected.size());
		expected.forEach(multiValueMap::add);
		return formData(multiValueMap, false);
	}

	private RequestMatcher formData(MultiValueMap<String, String> expectedMap, boolean containsExactly) {
		return request -> {
			MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
			MockHttpInputMessage message = new MockHttpInputMessage(mockRequest.getBodyAsBytes());
			message.getHeaders().putAll(mockRequest.getHeaders());
			MultiValueMap<String, String> actualMap = new FormHttpMessageConverter().read(null, message);
			if (containsExactly) {
				assertEquals("Form data", expectedMap, actualMap);
			}
			else {
				assertTrue("Form data " + actualMap, expectedMap.size() <= actualMap.size());
				for (Map.Entry<String, ? extends List<?>> entry : expectedMap.entrySet()) {
					String name = entry.getKey();
					List<?> values = entry.getValue();
					assertTrue("No form parameter '" + name + "'", actualMap.get(name) != null);
					assertTrue("Parameter value count " + values.size(), values.size() <= actualMap.get(name).size());
					for (int i = 0; i < values.size(); i++) {
						assertEquals("Form parameter", values.get(i), actualMap.get(name).get(i));
					}
				}
			}
		};
	}

	/**
	 * Parse the body as multipart data and assert it contains exactly the
	 * values from the given {@code MultiValueMap}. Values may be of type:
	 * <ul>
	 * <li>{@code String} - form field
	 * <li>{@link Resource} - content from a file
	 * <li>{@code byte[]} - other raw content
	 * </ul>
	 * <p><strong>Note:</strong> This method uses the Apache Commons FileUpload
	 * library to parse the multipart data and it must be on the test classpath.
	 * @param expectedMap the expected multipart values
	 * @since 5.3
	 */
	public RequestMatcher multipartData(MultiValueMap<String, ?> expectedMap) {
		return multipartData(expectedMap, true);
	}

	/**
	 * Variant of {@link #multipartData(MultiValueMap)} that does the same but
	 * only for a subset of the actual values.
	 * @param expectedMap the expected multipart values
	 * @since 5.3
	 */
	public RequestMatcher multipartDataContains(Map<String, ?> expectedMap) {
		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>(expectedMap.size());
		expectedMap.forEach(map::add);
		return multipartData(map, false);
	}

	@SuppressWarnings("ConstantConditions")
	private RequestMatcher multipartData(MultiValueMap<String, ?> expectedMap, boolean containsExactly) {
		return request -> {
			MultiValueMap<String, ?> actualMap = MultipartHelper.parse(request);
			if (containsExactly) {
				assertEquals("Multipart request content: " + actualMap, expectedMap.size(), actualMap.size());
			}
			for (Map.Entry<String, ? extends List<?>> entry : expectedMap.entrySet()) {
				String name = entry.getKey();
				List<?> values = entry.getValue();
				assertTrue("No Multipart '" + name + "'", actualMap.get(name) != null);
				assertTrue("Multipart value count " + values.size(), containsExactly ?
						values.size() == actualMap.get(name).size() :
						values.size() <= actualMap.get(name).size());
				for (int i = 0; i < values.size(); i++) {
					Object expected = values.get(i);
					Object actual = actualMap.get(name).get(i);
					if (expected instanceof Resource) {
						expected = StreamUtils.copyToByteArray(((Resource) expected).getInputStream());
					}
					if (expected instanceof byte[]) {
						assertTrue("Multipart is not a file", actual instanceof MultipartFile);
						assertEquals("Multipart content", expected, ((MultipartFile) actual).getBytes());
					}
					else if (expected instanceof String) {
						assertTrue("Multipart is not a String", actual instanceof String);
						assertEquals("Multipart content", expected, actual);
					}
					else {
						throw new IllegalArgumentException("Unexpected multipart value: " + expected.getClass());
					}
				}
			}
		};
	}

	/**
	 * Parse the request body and the given String as XML and assert that the
	 * two are "similar" - i.e. they contain the same elements and attributes
	 * regardless of order.
	 * <p>Use of this matcher assumes the
	 * <a href="http://xmlunit.sourceforge.net/">XMLUnit</a> library is available.
	 * @param expectedXmlContent the expected XML content
	 */
	public RequestMatcher xml(String expectedXmlContent) {
		return new AbstractXmlRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xmlHelper.assertXmlEqual(expectedXmlContent, request.getBodyAsString());
			}
		};
	}

	/**
	 * Parse the request content as {@link Node} and apply the given {@link Matcher}.
	 */
	public RequestMatcher node(Matcher<? super Node> matcher) {
		return new AbstractXmlRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xmlHelper.assertNode(request.getBodyAsString(), matcher);
			}
		};
	}

	/**
	 * Parse the request content as {@link DOMSource} and apply the given {@link Matcher}.
	 * @see <a href="https://code.google.com/p/xml-matchers/">https://code.google.com/p/xml-matchers/</a>
	 */
	public RequestMatcher source(Matcher<? super Source> matcher) {
		return new AbstractXmlRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xmlHelper.assertSource(request.getBodyAsString(), matcher);
			}
		};
	}

	/**
	 * Parse the expected and actual strings as JSON and assert the two
	 * are "similar" - i.e. they contain the same attribute-value pairs
	 * regardless of formatting with a lenient checking (extensible, and non-strict array
	 * ordering).
	 * <p>Use of this matcher requires the <a
	 * href="https://jsonassert.skyscreamer.org/">JSONassert</a> library.
	 * @param expectedJsonContent the expected JSON content
	 * @since 5.0.5
	 */
	public RequestMatcher json(String expectedJsonContent) {
		return json(expectedJsonContent, false);
	}

	/**
	 * Parse the request body and the given string as JSON and assert the two
	 * are "similar" - i.e. they contain the same attribute-value pairs
	 * regardless of formatting.
	 * <p>Can compare in two modes, depending on {@code strict} parameter value:
	 * <ul>
	 * <li>{@code true}: strict checking. Not extensible, and strict array ordering.</li>
	 * <li>{@code false}: lenient checking. Extensible, and non-strict array ordering.</li>
	 * </ul>
	 * <p>Use of this matcher requires the <a
	 * href="https://jsonassert.skyscreamer.org/">JSONassert</a> library.
	 * @param expectedJsonContent the expected JSON content
	 * @param strict enables strict checking
	 * @since 5.0.5
	 */
	public RequestMatcher json(String expectedJsonContent, boolean strict) {
		return request -> {
			try {
				MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
				this.jsonHelper.assertJsonEqual(expectedJsonContent, mockRequest.getBodyAsString(), strict);
			}
			catch (Exception ex) {
				throw new AssertionError("Failed to parse expected or actual JSON request content", ex);
			}
		};
	}


	/**
	 * Abstract base class for XML {@link RequestMatcher}'s.
	 */
	private abstract static class AbstractXmlRequestMatcher implements RequestMatcher {

		@Override
		public final void match(ClientHttpRequest request) throws IOException, AssertionError {
			try {
				MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
				matchInternal(mockRequest);
			}
			catch (Exception ex) {
				throw new AssertionError("Failed to parse expected or actual XML request content", ex);
			}
		}

		protected abstract void matchInternal(MockClientHttpRequest request) throws Exception;
	}


	private static class MultipartHelper {

		public static MultiValueMap<String, ?> parse(ClientHttpRequest request) {
			MultipartHttpServletRequest servletRequest = adaptToMultipartRequest(request);
			MultiValueMap<String, Object> result = new LinkedMultiValueMap<>();
			for (Map.Entry<String, List<MultipartFile>> entry : servletRequest.getMultiFileMap().entrySet()) {
				for (MultipartFile value : entry.getValue()) {
					result.add(entry.getKey(), value);
				}
			}
			for (Map.Entry<String, String[]> entry : servletRequest.getParameterMap().entrySet()) {
				for (String value : entry.getValue()) {
					result.add(entry.getKey(), value);
				}
			}
			return result;
		}

		private static MultipartHttpServletRequest adaptToMultipartRequest(ClientHttpRequest request) {
			MockClientHttpRequest source = (MockClientHttpRequest) request;
			MockHttpServletRequest target = new MockHttpServletRequest();
			target.setContent(source.getBodyAsBytes());
			source.getHeaders().forEach((name, values) -> values.forEach(v -> target.addHeader(name, v)));
			return new CommonsMultipartResolver().resolveMultipart(target);
		}
	}

}
