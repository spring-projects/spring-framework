/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.http.client.reactive;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.netty.handler.codec.http.HttpHeaders;

import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

/**
 * {@code MultiValueMap} implementation for wrapping Netty HTTP headers.
 *
 * <p>There is a duplicate of this class in the server package!
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
class NettyHeadersAdapter implements MultiValueMap<String, String> {

	private final HttpHeaders headers;


	NettyHeadersAdapter(HttpHeaders headers) {
		this.headers = headers;
	}


	@Override
	@Nullable
	public String getFirst(String key) {
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
		Map<String, String> singleValueMap = CollectionUtils.newLinkedHashMap(this.headers.size());
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
		return (key instanceof String && this.headers.contains((String) key));
	}

	@Override
	public boolean containsValue(Object value) {
		return (value instanceof String &&
				this.headers.entries().stream()
						.anyMatch(entry -> value.equals(entry.getValue())));
	}

	@Override
	@Nullable
	public List<String> get(Object key) {
		if (containsKey(key)) {
			return this.headers.getAll((String) key);
		}
		return null;
	}

	@Nullable
	@Override
	public List<String> put(String key, @Nullable List<String> value) {
		List<String> previousValues = this.headers.getAll(key);
		this.headers.set(key, value);
		return previousValues;
	}

	@Nullable
	@Override
	public List<String> remove(Object key) {
		if (key instanceof String) {
			List<String> previousValues = this.headers.getAll((String) key);
			this.headers.remove((String) key);
			return previousValues;
		}
		return null;
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		map.forEach(this.headers::add);
	}

	@Override
	public void clear() {
		this.headers.clear();
	}

	@Override
	public Set<String> keySet() {
		return this.headers.names();
	}

	@Override
	public Collection<List<String>> values() {
		return this.headers.names().stream()
				.map(this.headers::getAll).collect(Collectors.toList());
	}

	@Override
	public Set<Entry<String, List<String>>> entrySet() {
		return new AbstractSet<Entry<String, List<String>>>() {
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

		private Iterator<String> names = headers.names().iterator();

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

}
