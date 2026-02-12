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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;

/**
 * {@code MultiValueMap} implementation for wrapping Servlet response headers.
 *
 * @author Rossen Stoyanchev
 * @since 7.0.5
 */
class ServletResponseHeadersAdapter implements MultiValueMap<String, String> {

	private final HttpServletResponse response;


	ServletResponseHeadersAdapter(HttpServletResponse response) {
		this.response = response;
	}


	@Override
	public String getFirst(String key) {
		return this.response.getHeader(key);
	}

	@Override
	public void add(String key, @Nullable String value) {
		this.response.addHeader(key, value);
	}

	@Override
	public void addAll(String key, List<? extends String> values) {
		values.forEach(value -> this.response.addHeader(key, value));
	}

	@Override
	public void addAll(MultiValueMap<String, String> map) {
		for (Entry<String, List<String>> entry : map.entrySet()) {
			for (String value : entry.getValue()) {
				this.response.addHeader(entry.getKey(), value);
			}
		}
	}

	@Override
	public void set(String key, @Nullable String value) {
		this.response.setHeader(key, value);
	}

	@Override
	public void setAll(Map<String, String> map) {
		for (Entry<String, String> entry : map.entrySet()) {
			this.response.setHeader(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		Map<String, String> map = new LinkedHashMap<>();
		Collection<String> names = this.response.getHeaderNames();
		for (String name : names) {
			map.put(name, this.response.getHeader(name));
		}
		return map;
	}

	@Override
	public int size() {
		return this.response.getHeaderNames().size();
	}

	@Override
	public boolean isEmpty() {
		return this.response.getHeaderNames().isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		if (key instanceof String headerName) {
			return this.response.containsHeader(headerName);
		}
		return false;
	}

	@Override
	public boolean containsValue(Object rawValue) {
		if (rawValue instanceof String text) {
			for (String name : this.response.getHeaderNames()) {
				Collection<String> values = this.response.getHeaders(name);
				for (String value : values) {
					if (text.equals(value)) {
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
			Collection<String> values = this.response.getHeaders(headerName);
			if (!values.isEmpty()) {
				return (values instanceof List ? (List<String>) values : new ArrayList<>(values));
			}
			return (this.response.containsHeader(headerName) ? Collections.emptyList() : null);
		}
		return null;
	}

	@Override
	public @Nullable List<String> put(String key, List<String> values) {
		List<String> previous = remove(key);
		for (String value : values) {
			this.response.addHeader(key, value);
		}
		return previous;
	}

	@Override
	public @Nullable List<String> remove(Object key) {
		if (key instanceof String headerName) {
			Collection<String> previous = this.response.getHeaders(headerName);
			if (previous != null) {
				this.response.setHeader(headerName, null);
			}
			return (previous != null ? new ArrayList<>(previous) : null);
		}
		return null;
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		for (Entry<? extends String, ? extends List<String>> entry : map.entrySet()) {
			this.response.setHeader(entry.getKey(), null);
			for (String value : entry.getValue()) {
				this.response.addHeader(entry.getKey(), value);
			}
		}
	}

	@Override
	public void clear() {
		for (String headerName : this.response.getHeaderNames()) {
			this.response.setHeader(headerName, null);
		}
	}

	@Override
	public Set<String> keySet() {
		return new LinkedHashSet<>(this.response.getHeaderNames());
	}

	@Override
	public Collection<List<String>> values() {
		List<List<String>> allValues = new ArrayList<>();
		for (String name : this.response.getHeaderNames()) {
			allValues.add(new ArrayList<>(this.response.getHeaders(name)));
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
				return ServletResponseHeadersAdapter.this.size();
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


	private class EntryIterator implements Iterator<Entry<String, List<String>>> {

		private final Iterator<String> names =
				ServletResponseHeadersAdapter.this.response.getHeaderNames().iterator();

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
			return ServletResponseHeadersAdapter.this.get(this.key);
		}

		@Override
		public @Nullable List<String> setValue(List<String> values) {
			return ServletResponseHeadersAdapter.this.put(this.key, values);
		}
	}

}
