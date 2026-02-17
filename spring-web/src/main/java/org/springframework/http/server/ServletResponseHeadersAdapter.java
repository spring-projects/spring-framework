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
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
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
	public @Nullable String getFirst(String key) {
		String header = this.response.getHeader(key);
		if (header == null && key.equalsIgnoreCase(HttpHeaders.CONTENT_TYPE)) {
			header = this.response.getContentType();
		}
		return header;
	}

	@Override
	public void add(String key, @Nullable String value) {
		this.response.addHeader(key, value);
	}

	@Override
	public void addAll(String key, List<? extends String> values) {
		for (String value : values) {
			this.response.addHeader(key, value);
		}
	}

	@Override
	public void addAll(MultiValueMap<String, String> map) {
		throw httpHeadersUnsupportedOperationException();
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
		throw httpHeadersUnsupportedOperationException();
	}

	@Override
	public @Nullable List<String> get(Object key) {
		if (key instanceof String headerName) {
			Collection<String> values = this.response.getHeaders(headerName);
			if (values.isEmpty() && headerName.equalsIgnoreCase(HttpHeaders.CONTENT_TYPE)) {
				String contentType = this.response.getContentType();
				return (contentType != null ? Collections.singletonList(contentType) : null);
			}
			if (!values.isEmpty()) {
				return new ArrayList<>(values);
			}
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
			List<String> previous = get(headerName);
			if (previous != null) {
				this.response.setHeader(headerName, null);
			}
			return previous;
		}
		return null;
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		throw httpHeadersUnsupportedOperationException();
	}

	@Override
	public void clear() {
		for (String headerName : this.response.getHeaderNames()) {
			this.response.setHeader(headerName, null);
		}
	}

	@Override
	public Set<String> keySet() {
		return new HeaderNames();
	}

	@Override
	public Collection<List<String>> values() {
		throw httpHeadersUnsupportedOperationException();
	}

	@Override
	public Set<Entry<String, List<String>>> entrySet() {
		throw httpHeadersUnsupportedOperationException();
	}

	private static UnsupportedOperationException httpHeadersUnsupportedOperationException() {
		return new UnsupportedOperationException("HttpHeaders does not support all Map operations");
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
		for (String name : this.response.getHeaderNames()) {
			for (String value : this.response.getHeaders(name)) {
				map.add(name, value);
			}
		}
		return map;
	}

	@Override
	public String toString() {
		return HttpHeaders.formatHeaders(this);
	}


	private class HeaderNames extends AbstractSet<String> {

		@Override
		public Iterator<String> iterator() {
			return new HeaderNamesIterator(response.getHeaderNames());
		}

		@Override
		public int size() {
			return ServletResponseHeadersAdapter.this.size();
		}
	}


	private static final class HeaderNamesIterator implements Iterator<String> {

		private final Iterator<String> values;

		private HeaderNamesIterator(Collection<String> values) {
			this.values = values.iterator();
		}

		@Override
		public boolean hasNext() {
			return this.values.hasNext();
		}

		@Override
		public String next() {
			return this.values.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

}
