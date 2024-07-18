/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.web.servlet.request;

import java.net.URI;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.Cookie;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.MultiValueMap;

/**
 * Default builder for {@link MockHttpServletRequest} required as input to
 * perform requests in {@link MockMvc}.
 *
 * <p>Application tests will typically access this builder through the static
 * factory methods in {@link MockMvcRequestBuilders}.
 *
 * <p>This class is not open for extension. To apply custom initialization to
 * the created {@code MockHttpServletRequest}, please use the
 * {@link #with(RequestPostProcessor)} extension point.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @author Kamill Sokol
 * @since 3.2
 */
public class MockHttpServletRequestBuilder
		extends AbstractMockHttpServletRequestBuilder<MockHttpServletRequestBuilder> {

	/**
	 * Package private constructor. To get an instance, use static factory
	 * methods in {@link MockMvcRequestBuilders}.
	 * <p>Although this class cannot be extended, additional ways to initialize
	 * the {@code MockHttpServletRequest} can be plugged in via
	 * {@link #with(RequestPostProcessor)}.
	 * @param httpMethod the HTTP method (GET, POST, etc.)
	 */
	MockHttpServletRequestBuilder(HttpMethod httpMethod) {
		super(httpMethod);
	}


	// Override to keep binary compatibility.

	@Override
	public MockHttpServletRequestBuilder uri(URI uri) {
		return super.uri(uri);
	}

	@Override
	public MockHttpServletRequestBuilder uri(String uriTemplate, Object... uriVariables) {
		return super.uri(uriTemplate, uriVariables);
	}

	@Override
	public MockHttpServletRequestBuilder contextPath(String contextPath) {
		return super.contextPath(contextPath);
	}

	@Override
	public MockHttpServletRequestBuilder servletPath(String servletPath) {
		return super.servletPath(servletPath);
	}

	@Override
	public MockHttpServletRequestBuilder pathInfo(@Nullable String pathInfo) {
		return super.pathInfo(pathInfo);
	}

	@Override
	public MockHttpServletRequestBuilder secure(boolean secure) {
		return super.secure(secure);
	}

	@Override
	public MockHttpServletRequestBuilder characterEncoding(Charset encoding) {
		return super.characterEncoding(encoding);
	}

	@Override
	public MockHttpServletRequestBuilder characterEncoding(String encoding) {
		return super.characterEncoding(encoding);
	}

	@Override
	public MockHttpServletRequestBuilder content(byte[] content) {
		return super.content(content);
	}

	@Override
	public MockHttpServletRequestBuilder content(String content) {
		return super.content(content);
	}

	@Override
	public MockHttpServletRequestBuilder contentType(MediaType contentType) {
		return super.contentType(contentType);
	}

	@Override
	public MockHttpServletRequestBuilder contentType(String contentType) {
		return super.contentType(contentType);
	}

	@Override
	public MockHttpServletRequestBuilder accept(MediaType... mediaTypes) {
		return super.accept(mediaTypes);
	}

	@Override
	public MockHttpServletRequestBuilder accept(String... mediaTypes) {
		return super.accept(mediaTypes);
	}

	@Override
	public MockHttpServletRequestBuilder header(String name, Object... values) {
		return super.header(name, values);
	}

	@Override
	public MockHttpServletRequestBuilder headers(HttpHeaders httpHeaders) {
		return super.headers(httpHeaders);
	}

	@Override
	public MockHttpServletRequestBuilder param(String name, String... values) {
		return super.param(name, values);
	}

	@Override
	public MockHttpServletRequestBuilder params(MultiValueMap<String, String> params) {
		return super.params(params);
	}

	@Override
	public MockHttpServletRequestBuilder queryParam(String name, String... values) {
		return super.queryParam(name, values);
	}

	@Override
	public MockHttpServletRequestBuilder queryParams(MultiValueMap<String, String> params) {
		return super.queryParams(params);
	}

	@Override
	public MockHttpServletRequestBuilder formField(String name, String... values) {
		return super.formField(name, values);
	}

	@Override
	public MockHttpServletRequestBuilder formFields(MultiValueMap<String, String> formFields) {
		return super.formFields(formFields);
	}

	@Override
	public MockHttpServletRequestBuilder cookie(Cookie... cookies) {
		return super.cookie(cookies);
	}

	@Override
	public MockHttpServletRequestBuilder locale(Locale... locales) {
		return super.locale(locales);
	}

	@Override
	public MockHttpServletRequestBuilder locale(@Nullable Locale locale) {
		return super.locale(locale);
	}

	@Override
	public MockHttpServletRequestBuilder requestAttr(String name, Object value) {
		return super.requestAttr(name, value);
	}

	@Override
	public MockHttpServletRequestBuilder sessionAttr(String name, Object value) {
		return super.sessionAttr(name, value);
	}

	@Override
	public MockHttpServletRequestBuilder sessionAttrs(Map<String, Object> sessionAttributes) {
		return super.sessionAttrs(sessionAttributes);
	}

	@Override
	public MockHttpServletRequestBuilder flashAttr(String name, Object value) {
		return super.flashAttr(name, value);
	}

	@Override
	public MockHttpServletRequestBuilder flashAttrs(Map<String, Object> flashAttributes) {
		return super.flashAttrs(flashAttributes);
	}

	@Override
	public MockHttpServletRequestBuilder session(MockHttpSession session) {
		return super.session(session);
	}

	@Override
	public MockHttpServletRequestBuilder principal(Principal principal) {
		return super.principal(principal);
	}

	@Override
	public MockHttpServletRequestBuilder remoteAddress(String remoteAddress) {
		return super.remoteAddress(remoteAddress);
	}

	@Override
	public MockHttpServletRequestBuilder with(RequestPostProcessor postProcessor) {
		return super.with(postProcessor);
	}

}
