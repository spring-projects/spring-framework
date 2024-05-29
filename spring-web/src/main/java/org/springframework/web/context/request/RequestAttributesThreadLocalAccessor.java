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

package org.springframework.web.context.request;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import io.micrometer.context.ThreadLocalAccessor;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;

/**
 * Adapt {@link RequestContextHolder} to the {@link ThreadLocalAccessor} contract
 * to assist the Micrometer Context Propagation library with
 * {@link RequestAttributes} propagation.
 *
 * @author Tadaya Tsuyukubo
 * @author Rossen Stoyanchev
 * @since 6.2
 */
public class RequestAttributesThreadLocalAccessor implements ThreadLocalAccessor<RequestAttributes> {

	/**
	 * Key under which this accessor is registered in
	 * {@link io.micrometer.context.ContextRegistry}.
	 */
	public static final String KEY = RequestAttributesThreadLocalAccessor.class.getName() + ".KEY";

	@Override
	public Object key() {
		return KEY;
	}

	@Override
	@Nullable
	public RequestAttributes getValue() {
		RequestAttributes request = RequestContextHolder.getRequestAttributes();
		if (request instanceof ServletRequestAttributes sra && !(sra instanceof SnapshotServletRequestAttributes)) {
			request = new SnapshotServletRequestAttributes(sra);
		}
		return request;
	}

	@Override
	public void setValue(RequestAttributes value) {
		RequestContextHolder.setRequestAttributes(value);
	}

	@Override
	public void setValue() {
		RequestContextHolder.resetRequestAttributes();
	}


	/**
	 * ServletRequestAttributes that takes another instance, and makes a copy of the
	 * request attributes at present to provides extended read access during async
	 * handling when the DispatcherServlet has exited from the initial REQUEST dispatch
	 * and marked the request {@link ServletRequestAttributes#requestCompleted()}.
	 * <p>Note that beyond access to request attributes, here is no attempt to support
	 * setting or removing request attributes, nor to access session attributes after
	 * the initial REQUEST dispatch has exited.
	 */
	private static final class SnapshotServletRequestAttributes extends ServletRequestAttributes {

		private final ServletRequestAttributes delegate;

		private final Map<String, Object> attributeMap;

		public SnapshotServletRequestAttributes(ServletRequestAttributes requestAttributes) {
			super(requestAttributes.getRequest(), requestAttributes.getResponse());
			this.delegate = requestAttributes;
			this.attributeMap = getAttributes(requestAttributes.getRequest());
		}

		private static Map<String, Object> getAttributes(HttpServletRequest request) {
			Map<String, Object> map = new HashMap<>();
			Enumeration<String> names = request.getAttributeNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				map.put(name, request.getAttribute(name));
			}
			return map;
		}

		// Delegate methods that check isRequestActive()

		@Nullable
		@Override
		public Object getAttribute(String name, int scope) {
			if (scope == RequestAttributes.SCOPE_REQUEST && !this.delegate.isRequestActive()) {
				return this.attributeMap.get(name);
			}
			try {
				return this.delegate.getAttribute(name, scope);
			}
			catch (IllegalStateException ex) {
				if (scope == RequestAttributes.SCOPE_REQUEST) {
					return this.attributeMap.get(name);
				}
				throw ex;
			}
		}

		@Override
		public String[] getAttributeNames(int scope) {
			if (scope == RequestAttributes.SCOPE_REQUEST && !this.delegate.isRequestActive()) {
				return this.attributeMap.keySet().toArray(new String[0]);
			}
			try {
				return this.delegate.getAttributeNames(scope);
			}
			catch (IllegalStateException ex) {
				if (scope == RequestAttributes.SCOPE_REQUEST) {
					return this.attributeMap.keySet().toArray(new String[0]);
				}
				throw ex;
			}
		}

		@Override
		public void setAttribute(String name, Object value, int scope) {
			this.delegate.setAttribute(name, value, scope);
		}

		@Override
		public void removeAttribute(String name, int scope) {
			this.delegate.removeAttribute(name, scope);
		}
	}

}
