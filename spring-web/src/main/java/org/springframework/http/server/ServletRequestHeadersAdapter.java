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
import org.springframework.util.MultiValueMap;

/**
 * {@code MultiValueMap} implementation for wrapping Servlet request headers.
 *
 * @author Rossen Stoyanchev
 * @since 7.0.5
 */
final class ServletRequestHeadersAdapter implements MultiValueMap<String, String> {

	private final HttpServletRequest request;


	ServletRequestHeadersAdapter(HttpServletRequest request) {
		this.request = request;
	}


	@Override
	public @Nullable String getFirst(String key) {
		return this.request.getHeader(key);
	}

	@Override
	public void add(String key, @Nullable String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addAll(String key, List<? extends String> values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addAll(MultiValueMap<String, String> map) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void set(String key, @Nullable String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setAll(Map<String, String> map) {
		throw new UnsupportedOperationException();
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
		Enumeration<String> names = this.request.getHeaderNames();
		Set<String> set = new LinkedHashSet<>();
		while (names.hasMoreElements()) {
			set.add(names.nextElement().toLowerCase(Locale.ROOT));
		}
		return set.size();
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
		if (rawValue instanceof String text) {
			Enumeration<String> names = this.request.getHeaderNames();
			while (names.hasMoreElements()) {
				Enumeration<String> values = this.request.getHeaders(names.nextElement());
				while (values.hasMoreElements()) {
					if (text.equals(values.nextElement())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public @Nullable List<String> get(Object key) {
		if (key instanceof String headerName) {
			Enumeration<String> values = this.request.getHeaders(headerName);
			if (values.hasMoreElements()) {
				List<String> result = new ArrayList<>();
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
		throw new UnsupportedOperationException();
	}

	@Override
	public @Nullable List<String> remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<String> keySet() {
		Set<String> set = new LinkedHashSet<>();
		Enumeration<String> names = this.request.getHeaderNames();
		while (names.hasMoreElements()) {
			set.add(names.nextElement());
		}
		return set;
	}

	@Override
	public Collection<List<String>> values() {
		List<List<String>> allValues = new ArrayList<>();
		Enumeration<String> names = this.request.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			List<String> currentValues = new ArrayList<>();
			Enumeration<String> values = this.request.getHeaders(name);
			while (values.hasMoreElements()) {
				currentValues.add(values.nextElement());
			}
			allValues.add(currentValues);
		}
		return allValues;
	}

	@Override
	public Set<Entry<String, List<String>>> entrySet() {
		return new AbstractSet<>() {
			@Override
			public Iterator<Entry<String, List<String>>> iterator() {
				return new EntryIterator();
			}

			@Override
			public int size() {
				return ServletRequestHeadersAdapter.this.size();
			}
		};
	}


	@Override
	public int hashCode() {
		return Map.copyOf(this).hashCode();
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other ||
				(other instanceof MultiValueMap<?,?> that && Map.copyOf(this).equals(that)));
	}

	@Override
	public String toString() {
		return HttpHeaders.formatHeaders(this);
	}


	/**
	 * Apply a wrapper that allows headers to be set or added, and treats those
	 * as overrides to the headers in the given MultiValueMap.
	 * @param headers the headers map to wrap
	 * @return the wrapper instance
	 */
	public static MultiValueMap<String, String> overrideHeadersWrapper(MultiValueMap<String, String> headers) {
		return new OverrideHeaderWrapper(headers);
	}


	private class EntryIterator implements Iterator<Entry<String, List<String>>> {

		private final Iterator<String> names = ServletRequestHeadersAdapter.this.keySet().iterator();

		@Override
		public boolean hasNext() {
			return this.names.hasNext();
		}

		@Override
		public Entry<String, List<String>> next() {
			return new HeaderEntry(this.names.next());
		}
	}


	private final class HeaderEntry implements Entry<String, List<String>> {

		private final String key;

		HeaderEntry(String key) {
			this.key = key;
		}

		@Override
		public String getKey() {
			return this.key;
		}

		@Override
		public @Nullable List<String> getValue() {
			return get(this.key);
		}

		@Override
		public @Nullable List<String> setValue(List<String> value) {
			List<String> previous = getValue();
			remove(this.key);
			addAll(this.key, value);
			return previous;
		}
	}


	/**
	 * Wrapper that supports optional override values.
	 */
	private static class OverrideHeaderWrapper implements MultiValueMap<String, String> {

		private final MultiValueMap<String, String> delegate;

		private @Nullable MultiValueMap<String, String> overrideHeaders;

		OverrideHeaderWrapper(MultiValueMap<String, String> delegate) {
			this.delegate = delegate;
		}

		@Override
		public @Nullable String getFirst(String key) {
			String value = (this.overrideHeaders != null ? this.overrideHeaders.getFirst(key) : null);
			return (value != null ? value : this.delegate.getFirst(key));
		}

		@Override
		public void add(String key, @Nullable String value) {
			initOverrideHeaders().add(key, value);
		}

		@Override
		public void addAll(String key, List<? extends String> values) {
			initOverrideHeaders().addAll(key, values);
		}

		@Override
		public void addAll(MultiValueMap<String, String> map) {
			initOverrideHeaders().addAll(map);
		}

		@Override
		public void set(String key, @Nullable String value) {
			initOverrideHeaders().set(key, value);
		}

		@Override
		public void setAll(Map<String, String> map) {
			initOverrideHeaders().setAll(map);
		}

		@Override
		public Map<String, String> toSingleValueMap() {
			Map<String, String> map = this.delegate.toSingleValueMap();
			if (this.overrideHeaders != null) {
				this.overrideHeaders.forEach((key, values) -> map.put(key, values.get(0)));
			}
			return map;
		}

		@Override
		public int size() {
			if (this.overrideHeaders == null) {
				return this.delegate.size();
			}
			Set<String> set = new LinkedHashSet<>();
			for (String name : this.delegate.keySet()) {
				set.add(name.toLowerCase(Locale.ROOT));
			}
			this.overrideHeaders.keySet().forEach(key -> set.add(key.toLowerCase(Locale.ROOT)));
			return set.size();
		}

		@Override
		public boolean isEmpty() {
			return (this.delegate.isEmpty() && (this.overrideHeaders == null || this.overrideHeaders.isEmpty()));
		}

		@Override
		public boolean containsKey(Object key) {
			if (key instanceof String headerName) {
				if (this.delegate.containsKey(headerName)) {
					return true;
				}
				if (this.overrideHeaders != null) {
					return this.overrideHeaders.containsKey(headerName);
				}
			}
			return false;
		}

		@Override
		public boolean containsValue(Object rawValue) {
			if (rawValue instanceof String text) {
				if (this.delegate.containsValue(text)) {
					return true;
				}
				if (this.overrideHeaders != null) {
					return this.overrideHeaders.containsValue(rawValue);
				}
			}
			return false;
		}

		@Override
		public @Nullable List<String> get(Object key) {
			if (key instanceof String headerName) {
				if (this.overrideHeaders != null) {
					List<String> values = this.overrideHeaders.get(headerName);
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
			return initOverrideHeaders().put(key, value);
		}

		@Override
		public @Nullable List<String> remove(Object key) {
			return initOverrideHeaders().remove(key);
		}

		@Override
		public void putAll(Map<? extends String, ? extends List<String>> map) {
			initOverrideHeaders().putAll(map);
		}

		@Override
		public void clear() {
			initOverrideHeaders().clear();
		}

		@Override
		public Set<String> keySet() {
			Set<String> set = this.delegate.keySet();
			if (this.overrideHeaders != null) {
				set.addAll(this.overrideHeaders.keySet());
			}
			return set;
		}

		@Override
		public Collection<List<String>> values() {
			List<List<String>> allValues = new ArrayList<>();
			for (String name : keySet()) {
				if (this.overrideHeaders != null && this.overrideHeaders.containsKey(name)) {
					allValues.add(this.overrideHeaders.get(name));
				}
				else {
					allValues.add(this.delegate.get(name));
				}
			}
			return allValues;
		}

		@Override
		public Set<Entry<String, List<String>>> entrySet() {
			return new AbstractSet<>() {
				@Override
				public Iterator<Entry<String, List<String>>> iterator() {
					return new OverrideHeaderWrapper.EntryIterator();
				}

				@Override
				public int size() {
					return OverrideHeaderWrapper.this.size();
				}
			};
		}

		private MultiValueMap<String, String> initOverrideHeaders() {
			if (this.overrideHeaders == null) {
				this.overrideHeaders = CollectionUtils.toMultiValueMap(new LinkedCaseInsensitiveMap<>(8, Locale.ROOT));
			}
			return this.overrideHeaders;
		}


		@Override
		public int hashCode() {
			return Map.copyOf(this).hashCode();
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other ||
					(other instanceof MultiValueMap<?,?> that && Map.copyOf(this).equals(that)));
		}

		@Override
		public String toString() {
			return HttpHeaders.formatHeaders(this);
		}


		private class EntryIterator implements Iterator<Entry<String, List<String>>> {

			private final Iterator<String> names = OverrideHeaderWrapper.this.keySet().iterator();

			@Override
			public boolean hasNext() {
				return this.names.hasNext();
			}

			@Override
			public Entry<String, List<String>> next() {
				return new OverrideHeaderWrapper.HeaderEntry(this.names.next());
			}
		}


		private final class HeaderEntry implements Entry<String, List<String>> {

			private final String key;

			HeaderEntry(String key) {
				this.key = key;
			}

			@Override
			public String getKey() {
				return this.key;
			}

			@Override
			public @Nullable List<String> getValue() {
				return get(this.key);
			}

			@Override
			public @Nullable List<String> setValue(List<String> value) {
				List<String> previous = getValue();
				remove(this.key);
				addAll(this.key, value);
				return previous;
			}
		}
	}

}
