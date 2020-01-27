/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/**
 * {@code MultiValueMap} implementation for wrapping Jetty HTTP headers.
 *
 * @author Brian Clozel
 * @since 5.1.1
 */
class JettyHeadersAdapter implements MultiValueMap<String, String> {

	private final HttpFields headers;


	JettyHeadersAdapter(HttpFields headers) {
		this.headers = headers;
	}


	@Override
	public String getFirst(String key) {
		return this.headers.get(key);
	}

	@Override
	public void add(String key, @Nullable String value) {
		this.headers.add(key, value);
	}

	@Override
	public void addAll(String key, List<? extends String> values) {
		values.forEach(value -> add(key, value));
	}

	@Override
	public void addAll(MultiValueMap<String, String> values) {
		values.forEach(this::addAll);
	}

	@Override
	public void set(String key, @Nullable String value) {
		this.headers.put(key, value);
	}

	@Override
	public void setAll(Map<String, String> values) {
		values.forEach(this::set);
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		Map<String, String> singleValueMap = new LinkedHashMap<>(this.headers.size());
		Iterator<HttpField> iterator = this.headers.iterator();
		iterator.forEachRemaining(field -> {
			if (!singleValueMap.containsKey(field.getName())) {
				singleValueMap.put(field.getName(), field.getValue());
			}
		});
		return singleValueMap;
	}

	@Override
	public int size() {
		return this.headers.getFieldNamesCollection().size();
	}

	@Override
	public boolean isEmpty() {
		return (this.headers.size() == 0);
	}

	@Override
	public boolean containsKey(Object key) {
		return (key instanceof String && this.headers.containsKey((String) key));
	}

	@Override
	public boolean containsValue(Object value) {
		return (value instanceof String &&
				this.headers.stream().anyMatch(field -> field.contains((String) value)));
	}

	@Nullable
	@Override
	public List<String> get(Object key) {
		if (containsKey(key)) {
			return this.headers.getValuesList((String) key);
		}
		return null;
	}

	@Nullable
	@Override
	public List<String> put(String key, List<String> value) {
		List<String> oldValues = get(key);
		this.headers.put(key, value);
		return oldValues;
	}

	@Nullable
	@Override
	public List<String> remove(Object key) {
		if (key instanceof String) {
			List<String> oldValues = get(key);
			this.headers.remove((String) key);
			return oldValues;
		}
		return null;
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		map.forEach(this::put);
	}

	@Override
	public void clear() {
		this.headers.clear();
	}

	@Override
	public Set<String> keySet() {
		return this.headers.getFieldNamesCollection();
	}

	@Override
	public Collection<List<String>> values() {
		return this.headers.getFieldNamesCollection().stream()
				.map(this.headers::getValuesList).collect(Collectors.toList());
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
		return HttpHeaders.formatHeaders(this);
	}


	private class EntryIterator implements Iterator<Entry<String, List<String>>> {

		private Enumeration<String> names = headers.getFieldNames();

		@Override
		public boolean hasNext() {
			return this.names.hasMoreElements();
		}

		@Override
		public Entry<String, List<String>> next() {
			return new HeaderEntry(this.names.nextElement());
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
			return headers.getValuesList(this.key);
		}

		@Override
		public List<String> setValue(List<String> value) {
			List<String> previousValues = headers.getValuesList(this.key);
			headers.put(this.key, value);
			return previousValues;
		}
	}

}
