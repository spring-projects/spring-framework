/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.web.util.WebUtils;

/**
 * Factory for request condition objects.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public abstract class RequestConditionFactory {

	/**
	 * Parses the given parameters, and returns them as a set of request conditions.
	 *
	 * @param params the parameters
	 * @return the request conditions
	 * @see org.springframework.web.bind.annotation.RequestMapping#params()
	 */
	public static Set<RequestCondition> parseParams(String... params) {
		if (params == null) {
			return Collections.emptySet();
		}
		Set<RequestCondition> result = new LinkedHashSet<RequestCondition>(params.length);
		for (String expression : params) {
			result.add(new ParamNameValueCondition(expression));
		}
		return result;
	}

	/**
	 * Parses the given headers, and returns them as a set of request conditions.
	 *
	 * @param headers the headers
	 * @return the request conditions
	 * @see org.springframework.web.bind.annotation.RequestMapping#headers()
	 */
	public static Set<RequestCondition> parseHeaders(String... headers) {
		if (headers == null) {
			return Collections.emptySet();
		}
		Set<RequestCondition> result = new LinkedHashSet<RequestCondition>(headers.length);
		for (String expression : headers) {
			HeaderNameValueCondition header = new HeaderNameValueCondition(expression);
			if (isMediaTypeHeader(header.name)) {
				result.add(new MediaTypeHeaderNameValueCondition(expression));
			}
			else {
				result.add(header);
			}
		}
		return result;
	}

	private static boolean isMediaTypeHeader(String name) {
		return "Accept".equalsIgnoreCase(name) || "Content-Type".equalsIgnoreCase(name);
	}

	/**
	 * A condition that supports simple "name=value" style expressions as documented in
	 * <code>@RequestMapping.params()</code> and <code>@RequestMapping.headers()</code>.
	 */
	private static abstract class AbstractNameValueCondition<T> implements RequestCondition {

		protected final String name;

		protected final T value;

		protected final boolean isNegated;

		protected AbstractNameValueCondition(String expression) {
			int separator = expression.indexOf('=');
			if (separator == -1) {
				this.isNegated = expression.startsWith("!");
				this.name = isNegated ? expression.substring(1) : expression;
				this.value = null;
			}
			else {
				this.isNegated = (separator > 0) && (expression.charAt(separator - 1) == '!');
				this.name = isNegated ? expression.substring(0, separator - 1) : expression.substring(0, separator);
				this.value = parseValue(expression.substring(separator + 1));
			}
		}

		protected abstract T parseValue(String valueExpression);

		public final boolean match(HttpServletRequest request) {
			boolean isMatch;
			if (this.value != null) {
				isMatch = matchValue(request);
			}
			else {
				isMatch = matchName(request);
			}
			return isNegated ? !isMatch : isMatch;
		}

		protected abstract boolean matchName(HttpServletRequest request);

		protected abstract boolean matchValue(HttpServletRequest request);

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			if (value != null) {
				builder.append(name);
				if (isNegated) {
					builder.append('!');
				}
				builder.append('=');
				builder.append(value);
			}
			else {
				if (isNegated) {
					builder.append('!');
				}
				builder.append(name);
			}
			return builder.toString();
		}


		@Override
		public int hashCode() {
			int result = name.hashCode();
			result = 31 * result + (value != null ? value.hashCode() : 0);
			result = 31 * result + (isNegated ? 1 : 0);
			return result;
		}
	}

	/**
	 * Request parameter name-value condition.
	 */
	private static class ParamNameValueCondition extends AbstractNameValueCondition<String> {

		private ParamNameValueCondition(String expression) {
			super(expression);
		}

		@Override
		protected String parseValue(String valueExpression) {
			return valueExpression;
		}

		@Override
		protected boolean matchName(HttpServletRequest request) {
			return WebUtils.hasSubmitParameter(request, name);
		}

		@Override
		protected boolean matchValue(HttpServletRequest request) {
			return value.equals(request.getParameter(name));
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj != null && obj instanceof ParamNameValueCondition) {
				ParamNameValueCondition other = (ParamNameValueCondition) obj;
				return ((this.name.equals(other.name)) &&
						(this.value != null ? this.value.equals(other.value) : other.value == null) &&
						this.isNegated == other.isNegated);
			}
			return false;
		}
	}

	/**
	 * Request header name-value condition.
	 */
	static class HeaderNameValueCondition extends AbstractNameValueCondition<String> {

		public HeaderNameValueCondition(String expression) {
			super(expression);
		}

		@Override
		protected String parseValue(String valueExpression) {
			return valueExpression;
		}

		@Override
		protected boolean matchName(HttpServletRequest request) {
			return request.getHeader(name) != null;
		}

		@Override
		final protected boolean matchValue(HttpServletRequest request) {
			return value.equals(request.getHeader(name));
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj != null && obj instanceof HeaderNameValueCondition) {
				HeaderNameValueCondition other = (HeaderNameValueCondition) obj;
				return ((this.name.equalsIgnoreCase(other.name)) &&
						(this.value != null ? this.value.equals(other.value) : other.value == null) &&
						this.isNegated == other.isNegated);
			}
			return false;
		}


	}

	/**
	 * A RequestCondition that for headers that contain {@link org.springframework.http.MediaType MediaTypes}.
	 */
	private static class MediaTypeHeaderNameValueCondition extends AbstractNameValueCondition<List<MediaType>> {

		public MediaTypeHeaderNameValueCondition(String expression) {
			super(expression);
		}

		@Override
		protected List<MediaType> parseValue(String valueExpression) {
			return Collections.unmodifiableList(MediaType.parseMediaTypes(valueExpression));
		}

		@Override
		protected boolean matchName(HttpServletRequest request) {
			return request.getHeader(name) != null;
		}

		@Override
		protected boolean matchValue(HttpServletRequest request) {
			List<MediaType> requestMediaTypes = MediaType.parseMediaTypes(request.getHeader(name));

			for (MediaType mediaType : this.value) {
				for (MediaType requestMediaType : requestMediaTypes) {
					if (mediaType.includes(requestMediaType)) {
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj != null && obj instanceof MediaTypeHeaderNameValueCondition) {
				MediaTypeHeaderNameValueCondition other = (MediaTypeHeaderNameValueCondition) obj;
				return ((this.name.equalsIgnoreCase(other.name)) &&
						(this.value != null ? this.value.equals(other.value) : other.value == null) &&
						this.isNegated == other.isNegated);
			}
			return false;
		}


	}
}
