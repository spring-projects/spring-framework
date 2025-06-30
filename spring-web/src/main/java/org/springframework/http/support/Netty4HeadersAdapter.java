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

package org.springframework.http.support;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.netty.handler.codec.http.HttpHeaders;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.MultiValueMap;

/**
 * {@code MultiValueMap} implementation for wrapping Netty 4 HTTP headers.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Simon Baslé
 * @since 6.1
 */
public final class Netty4HeadersAdapter implements MultiValueMap<String, String> {

	private final HttpHeaders headers;


	/**
	 * Creates a new {@code Netty4HeadersAdapter} based on the given
	 * {@code HttpHeaders}.
	 */
	public Netty4HeadersAdapter(HttpHeaders headers) {
		Assert.notNull(headers, "Headers must not be null");
		this.headers = headers;
	}


	@Override
	public @Nullable String getFirst(String key) {
		return this.headers.get(key);
	}

	@Override
	public void add(String key, @Nullable String value) {
		if (value != null) {
			this.headers.add(key, value);
		}
	}

	@Override
	public void addAll(String key, List<? extends String> values) {
		this.headers.add(key, values);
	}

	@Override
	public void addAll(MultiValueMap<String, String> values) {
		values.forEach(this.headers::add);
	}

	@Override
	public void set(String key, @Nullable String value) {
		if (value != null) {
			this.headers.set(key, value);
		}
	}

	@Override
	public void setAll(Map<String, String> values) {
		values.forEach(this.headers::set);
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		Map<String, String> singleValueMap = new LinkedCaseInsensitiveMap<>(
				this.headers.size(), Locale.ROOT);
		this.headers.entries()
				.forEach(entry -> {
					if (!singleValueMap.containsKey(entry.getKey())) {
						singleValueMap.put(entry.getKey(), entry.getValue());
					}
				});
		return singleValueMap;
	}

	@Override
	public int size() {
		return this.headers.names().size();
	}

	@Override
	public boolean isEmpty() {
		return this.headers.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return (key instanceof String headerName && this.headers.contains(headerName));
	}

	@Override
	public boolean containsValue(Object value) {
		return (value instanceof String &&
				this.headers.entries().stream()
						.anyMatch(entry -> value.equals(entry.getValue())));
	}

	@Override
	public @Nullable List<String> get(Object key) {
		if (containsKey(key)) {
			return this.headers.getAll((String) key);
		}
		return null;
	}

	@Override
	public @Nullable List<String> put(String key, @Nullable List<String> value) {
		List<String> previousValues = this.headers.getAll(key);
		this.headers.set(key, value);
		return previousValues;
	}

	@Override
	public @Nullable List<String> remove(Object key) {
		if (key instanceof String headerName) {
			List<String> previousValues = this.headers.getAll(headerName);
			this.headers.remove(headerName);
			return previousValues;
		}
		return null;
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		map.forEach(this.headers::set);
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
		return this.headers.names().stream()
				.map(this.headers::getAll).toList();
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

		private final Iterator<String> names = headers.names().iterator();

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

		private final String key;

		HeaderEntry(String key) {
			this.key = key;
		}

		@Override
		public String getKey() {
			return this.key;
		}

		@Override
		public List<String> getValue() {
			return headers.getAll(this.key);
		}

		@Override
		public List<String> setValue(List<String> value) {
			List<String> previousValues = headers.getAll(this.key);
			headers.set(this.key, value);
			return previousValues;
		}
	}


	private class HeaderNames extends AbstractSet<String> {

		@Override
		public Iterator<String> iterator() {
			return new HeaderNamesIterator(headers.names().iterator());
		}

		@Override
		public int size() {
			return headers.names().size();
		}
	}

	private final class HeaderNamesIterator implements Iterator<String> {

		private final Iterator<String> iterator;

		private @Nullable String currentName;

		private HeaderNamesIterator(Iterator<String> iterator) {
			this.iterator = iterator;
		}

		@Override
		public boolean hasNext() {
			return this.iterator.hasNext();
		}

		@Override
		public String next() {
			this.currentName = this.iterator.next();
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
