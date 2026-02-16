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

package org.springframework.http.server;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@code MultiValueMap} implementation for wrapping Servlet request headers.
 *
 * @author Rossen Stoyanchev
 * @since 7.0.5
 */
final class ServletRequestHeadersAdapter implements MultiValueMap<String, String> {

	private final HttpServletRequest request;


	private ServletRequestHeadersAdapter(HttpServletRequest request) {
		this.request = request;
	}


	@Override
	public @Nullable String getFirst(String key) {
		return this.request.getHeader(key);
	}

	@Override
	public void add(String key, @Nullable String value) {
		throw immutableRequestException();
	}

	@Override
	public void addAll(String key, List<? extends String> values) {
		throw immutableRequestException();
	}

	@Override
	public void addAll(MultiValueMap<String, String> map) {
		throw httpHeadersMapException();
	}

	@Override
	public void set(String key, @Nullable String value) {
		throw immutableRequestException();
	}

	@Override
	public void setAll(Map<String, String> map) {
		throw immutableRequestException();
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		Map<String, String> map = new LinkedHashMap<>();
		Enumeration<String> names = this.request.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			map.put(name, this.request.getHeader(name));
		}
		return map;
	}

	@Override
	public int size() {
		return keySet().size();
	}

	@Override
	public boolean isEmpty() {
		return !this.request.getHeaderNames().hasMoreElements();
	}

	@Override
	public boolean containsKey(Object key) {
		if (key instanceof String headerName) {
			Enumeration<String> names = this.request.getHeaderNames();
			while (names.hasMoreElements()) {
				if (headerName.equalsIgnoreCase(names.nextElement())) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean containsValue(Object rawValue) {
		throw httpHeadersMapException();
	}

	@Override
	public @Nullable List<String> get(Object key) {
		if (key instanceof String headerName) {
			Enumeration<String> values = this.request.getHeaders(headerName);
			if (values.hasMoreElements()) {
				String value = values.nextElement();
				if (!values.hasMoreElements()) {
					return Collections.singletonList(value);
				}
				List<String> result = new ArrayList<>(4);
				result.add(value);
				while (values.hasMoreElements()) {
					result.add(values.nextElement());
				}
				return result;
			}
		}
		return null;
	}

	@Override
	public @Nullable List<String> put(String key, List<String> value) {
		throw immutableRequestException();
	}

	@Override
	public @Nullable List<String> remove(Object key) {
		throw immutableRequestException();
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		throw httpHeadersMapException();
	}

	@Override
	public void clear() {
		throw immutableRequestException();
	}

	@Override
	public Set<String> keySet() {
		return new HeaderNames();
	}

	@Override
	public Collection<List<String>> values() {
		throw httpHeadersMapException();
	}

	@Override
	public Set<Entry<String, List<String>>> entrySet() {
		throw httpHeadersMapException();
	}

	private static UnsupportedOperationException immutableRequestException() {
		return new UnsupportedOperationException("Request headers are immutable");
	}

	private static UnsupportedOperationException httpHeadersMapException() {
		return new UnsupportedOperationException("HttpHeaders does not support all Map operations");
	}

	@Override
	public String toString() {
		return HttpHeaders.formatHeaders(this);
	}


	/**
	 * Factory method to create a Servlet request headers adapter.
	 * @param request the request to access headers from
	 * @return the created adapter instance
	 */
	static MultiValueMap<String, String> create(HttpServletRequest request) {
		return new RequestHeaderOverrideWrapper(new ServletRequestHeadersAdapter(request));
	}


	private class HeaderNames extends AbstractSet<String> {

		@Override
		public Iterator<String> iterator() {
			return new HeaderNamesIterator(request.getHeaderNames());
		}

		@Override
		public int size() {
			Enumeration<String> names = request.getHeaderNames();
			int size = 0;
			while (names.hasMoreElements()) {
				names.nextElement();
				size++;
			}
			return size;
		}
	}


	private static final class HeaderNamesIterator implements Iterator<String> {

		private final Enumeration<String> enumeration;

		private HeaderNamesIterator(Enumeration<String> enumeration) {
			this.enumeration = enumeration;
		}

		@Override
		public boolean hasNext() {
			return this.enumeration.hasMoreElements();
		}

		@Override
		public String next() {
			return this.enumeration.nextElement();
		}

		@Override
		public void remove() {
			throw immutableRequestException();
		}
	}


	/**
	 * Wrapper that holds override values.
	 */
	private static class RequestHeaderOverrideWrapper implements MultiValueMap<String, String> {

		private final MultiValueMap<String, String> delegate;

		private @Nullable MultiValueMap<String, String> overrideMap;

		RequestHeaderOverrideWrapper(MultiValueMap<String, String> delegate) {
			this.delegate = delegate;
		}

		@Override
		public @Nullable String getFirst(String key) {
			String value = (this.overrideMap != null ? this.overrideMap.getFirst(key) : null);
			return (value != null ? value : this.delegate.getFirst(key));
		}

		@Override
		public void add(String key, @Nullable String value) {
			initOverrideMap().add(key, value);
		}

		@Override
		public void addAll(String key, List<? extends String> values) {
			initOverrideMap().addAll(key, values);
		}

		@Override
		public void addAll(MultiValueMap<String, String> map) {
			throw httpHeadersMapException();
		}

		@Override
		public void set(String key, @Nullable String value) {
			initOverrideMap().set(key, value);
		}

		@Override
		public void setAll(Map<String, String> map) {
			initOverrideMap().setAll(map);
		}

		@Override
		public Map<String, String> toSingleValueMap() {
			Map<String, String> map = this.delegate.toSingleValueMap();
			if (this.overrideMap != null) {
				this.overrideMap.forEach((key, values) -> map.put(key, values.get(0)));
			}
			return map;
		}

		@Override
		public int size() {
			if (this.overrideMap == null) {
				return this.delegate.size();
			}
			Set<String> set = new LinkedHashSet<>();
			for (String name : this.delegate.keySet()) {
				set.add(name.toLowerCase(Locale.ROOT));
			}
			this.overrideMap.keySet().forEach(key -> set.add(key.toLowerCase(Locale.ROOT)));
			return set.size();
		}

		@Override
		public boolean isEmpty() {
			return (this.delegate.isEmpty() && (this.overrideMap == null || this.overrideMap.isEmpty()));
		}

		@Override
		public boolean containsKey(Object key) {
			if (key instanceof String headerName) {
				if (this.delegate.containsKey(headerName)) {
					return true;
				}
				if (this.overrideMap != null) {
					return this.overrideMap.containsKey(headerName);
				}
			}
			return false;
		}

		@Override
		public boolean containsValue(Object rawValue) {
			throw httpHeadersMapException();
		}

		@Override
		public @Nullable List<String> get(Object key) {
			if (key instanceof String headerName) {
				if (this.overrideMap != null) {
					List<String> values = this.overrideMap.get(headerName);
					if (values != null) {
						return values;
					}
				}
				return this.delegate.get(headerName);
			}
			return null;
		}

		@Override
		public @Nullable List<String> put(String key, List<String> value) {
			return initOverrideMap().put(key, value);
		}

		@Override
		public @Nullable List<String> remove(Object key) {
			return initOverrideMap().remove(key);
		}

		@Override
		public void putAll(Map<? extends String, ? extends List<String>> map) {
			throw httpHeadersMapException();
		}

		@Override
		public void clear() {
			if (this.overrideMap != null) {
				this.overrideMap.clear();
			}
		}

		@Override
		public Set<String> keySet() {
			if (this.overrideMap != null) {
				Set<String> set = new LinkedHashSet<>(this.delegate.keySet());
				set.addAll(this.overrideMap.keySet());
				return set;
			}
			return this.delegate.keySet();
		}

		@Override
		public Collection<List<String>> values() {
			throw httpHeadersMapException();
		}

		@Override
		public Set<Entry<String, List<String>>> entrySet() {
			throw httpHeadersMapException();
		}

		private MultiValueMap<String, String> initOverrideMap() {
			if (this.overrideMap == null) {
				this.overrideMap = CollectionUtils.toMultiValueMap(new LinkedCaseInsensitiveMap<>(8, Locale.ROOT));
			}
			return this.overrideMap;
		}

		@Override
		public int hashCode() {
			return toMultiValueMap().hashCode();
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof MultiValueMap<?,?> that && toMultiValueMap().equals(that)));
		}

		private MultiValueMap<String, String> toMultiValueMap() {
			MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
			for (String name : keySet()) {
				List<String> values = get(name);
				if (values != null) {
					for (String value : values) {
						map.add(name, value);
					}
				}
			}
			return map;
		}

		@Override
		public String toString() {
			return HttpHeaders.formatHeaders(this);
		}
	}

}
