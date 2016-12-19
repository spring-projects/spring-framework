/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.filter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link javax.servlet.Filter} that makes form encoded data available through
 * the {@code ServletRequest.getParameter*()} family of methods during HTTP PUT
 * or PATCH requests.
 *
 * <p>The Servlet spec requires form data to be available for HTTP POST but
 * not for HTTP PUT or PATCH requests. This filter intercepts HTTP PUT and PATCH
 * requests where content type is {@code 'application/x-www-form-urlencoded'},
 * reads form encoded content from the body of the request, and wraps the ServletRequest
 * in order to make the form data available as request parameters just like
 * it is for HTTP POST requests.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class HttpPutFormContentFilter extends OncePerRequestFilter {

	private final FormHttpMessageConverter formConverter = new AllEncompassingFormHttpMessageConverter();


	/**
	 * The default character set to use for reading form data.
	 */
	public void setCharset(Charset charset) {
		this.formConverter.setCharset(charset);
	}


	@Override
	protected void doFilterInternal(final HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		if (("PUT".equals(request.getMethod()) || "PATCH".equals(request.getMethod())) && isFormContentType(request)) {
			HttpInputMessage inputMessage = new ServletServerHttpRequest(request) {
				@Override
				public InputStream getBody() throws IOException {
					return request.getInputStream();
				}
			};
			MultiValueMap<String, String> formParameters = formConverter.read(null, inputMessage);
			HttpServletRequest wrapper = new HttpPutFormContentRequestWrapper(request, formParameters);
			filterChain.doFilter(wrapper, response);
		}
		else {
			filterChain.doFilter(request, response);
		}
	}

	private boolean isFormContentType(HttpServletRequest request) {
		String contentType = request.getContentType();
		if (contentType != null) {
			try {
				MediaType mediaType = MediaType.parseMediaType(contentType);
				return (MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType));
			}
			catch (IllegalArgumentException ex) {
				return false;
			}
		}
		else {
			return false;
		}
	}


	private static class HttpPutFormContentRequestWrapper extends HttpServletRequestWrapper {

		private MultiValueMap<String, String> formParameters;

		public HttpPutFormContentRequestWrapper(HttpServletRequest request, MultiValueMap<String, String> parameters) {
			super(request);
			this.formParameters = (parameters != null ? parameters : new LinkedMultiValueMap<>());
		}

		@Override
		public String getParameter(String name) {
			String queryStringValue = super.getParameter(name);
			String formValue = this.formParameters.getFirst(name);
			return (queryStringValue != null ? queryStringValue : formValue);
		}

		@Override
		public Map<String, String[]> getParameterMap() {
			Map<String, String[]> result = new LinkedHashMap<>();
			Enumeration<String> names = getParameterNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				result.put(name, getParameterValues(name));
			}
			return result;
		}

		@Override
		public Enumeration<String> getParameterNames() {
			Set<String> names = new LinkedHashSet<>();
			names.addAll(Collections.list(super.getParameterNames()));
			names.addAll(this.formParameters.keySet());
			return Collections.enumeration(names);
		}

		@Override
		public String[] getParameterValues(String name) {
			String[] queryStringValues = super.getParameterValues(name);
			List<String> formValues = this.formParameters.get(name);
			if (formValues == null) {
				return queryStringValues;
			}
			else if (queryStringValues == null) {
				return formValues.toArray(new String[formValues.size()]);
			}
			else {
				List<String> result = new ArrayList<>(queryStringValues.length + formValues.size());
				result.addAll(Arrays.asList(queryStringValues));
				result.addAll(formValues);
				return result.toArray(new String[result.size()]);
			}
		}
	}

}
