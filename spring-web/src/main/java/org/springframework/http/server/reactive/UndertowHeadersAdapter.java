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

package org.springframework.http.server.reactive;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;
import org.jspecify.annotations.Nullable;

import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

/**
 * {@code MultiValueMap} implementation for wrapping Undertow HTTP headers.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 5.1.1
 */
class UndertowHeadersAdapter implements MultiValueMap<String, String> {

	private final HeaderMap headers;


	UndertowHeadersAdapter(HeaderMap headers) {
		this.headers = headers;
	}


	@Override
	public String getFirst(String key) {
		return this.headers.getFirst(key);
	}

	@Override
	public void add(String key, @Nullable String value) {
		this.headers.add(HttpString.tryFromString(key), value);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void addAll(String key, List<? extends String> values) {
		this.headers.addAll(HttpString.tryFromString(key), (List<String>) values);
	}

	@Override
	public void addAll(MultiValueMap<String, String> values) {
		values.forEach((key, list) -> this.headers.addAll(HttpString.tryFromString(key), list));
	}

	@Override
	public void set(String key, @Nullable String value) {
		this.headers.put(HttpString.tryFromString(key), value);
	}

	@Override
	public void setAll(Map<String, String> values) {
		values.forEach((key, list) -> this.headers.put(HttpString.tryFromString(key), list));
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		Map<String, String> singleValueMap = CollectionUtils.newLinkedHashMap(this.headers.size());
		this.headers.forEach(values ->
				singleValueMap.put(values.getHeaderName().toString(), values.getFirst()));
		return singleValueMap;
	}

	@Override
	public int size() {
		return this.headers.size();
	}

	@Override
	public boolean isEmpty() {
		return (this.headers.size() == 0);
	}

	@Override
	public boolean containsKey(Object key) {
		return (key instanceof String headerName && this.headers.contains(headerName));
	}

	@Override
	public boolean containsValue(Object value) {
		return (value instanceof String &&
				this.headers.getHeaderNames().stream()
						.map(this.headers::get)
						.anyMatch(values -> values.contains(value)));
	}

	@Override
	public @Nullable List<String> get(Object key) {
		return (key instanceof String headerName ? this.headers.get(headerName) : null);
	}

	@Override
	public @Nullable List<String> put(String key, List<String> value) {
		HeaderValues previousValues = this.headers.get(key);
		this.headers.putAll(HttpString.tryFromString(key), value);
		return previousValues;
	}

	@Override
	public @Nullable List<String> remove(Object key) {
		if (key instanceof String headerName) {
			Collection<String> removed = this.headers.remove(headerName);
			if (removed != null) {
				return new ArrayList<>(removed);
			}
		}
		return null;
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		map.forEach((key, values) ->
				this.headers.putAll(HttpString.tryFromString(key), values));
	}

	@Override
	public void clear() {
		this.headers.clear();
	}

	@Override
	public Set<String> keySet() {
		return new HeaderNames();
	}

	@Override
	public Collection<List<String>> values() {
		return this.headers.getHeaderNames().stream()
				.map(this.headers::get)
				.collect(Collectors.toList());
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
				return headers.size();
			}
		};
	}


	@Override
	public String toString() {
		return org.springframework.http.HttpHeaders.formatHeaders(this);
	}


	private class EntryIterator implements Iterator<Entry<String, List<String>>> {

		private final Iterator<HttpString> names = headers.getHeaderNames().iterator();

		@Override
		public boolean hasNext() {
			return this.names.hasNext();
		}

		@Override
		public Entry<String, List<String>> next() {
			return new HeaderEntry(this.names.next());
		}
	}


	private class HeaderEntry implements Entry<String, List<String>> {

		private final HttpString key;

		HeaderEntry(HttpString key) {
			this.key = key;
		}

		@Override
		public String getKey() {
			return this.key.toString();
		}

		@Override
		public List<String> getValue() {
			return headers.get(this.key);
		}

		@Override
		public List<String> setValue(List<String> value) {
			List<String> previousValues = headers.get(this.key);
			headers.putAll(this.key, value);
			return previousValues;
		}
	}


	private class HeaderNames extends AbstractSet<String> {

		@Override
		public Iterator<String> iterator() {
			return new HeaderNamesIterator(headers.getHeaderNames().iterator());
		}

		@Override
		public int size() {
			return headers.getHeaderNames().size();
		}
	}

	private final class HeaderNamesIterator implements Iterator<String> {

		private final Iterator<HttpString> iterator;

		private @Nullable String currentName;

		private HeaderNamesIterator(Iterator<HttpString> iterator) {
			this.iterator = iterator;
		}

		@Override
		public boolean hasNext() {
			return this.iterator.hasNext();
		}

		@Override
		public String next() {
			this.currentName = this.iterator.next().toString();
			return this.currentName;
		}

		@Override
		public void remove() {
			if (this.currentName == null) {
				throw new IllegalStateException("No current Header in iterator");
			}
			if (!headers.contains(this.currentName)) {
				throw new IllegalStateException("Header not present: " + this.currentName);
			}
			headers.remove(this.currentName);
		}
	}

}
