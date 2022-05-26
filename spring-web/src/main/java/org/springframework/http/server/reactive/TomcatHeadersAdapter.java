/*
 * Copyright 2002-2021 the original author or authors.
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
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.MimeHeaders;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

/**
 * {@code MultiValueMap} implementation for wrapping Tomcat HTTP headers.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 5.1.1
 */
class TomcatHeadersAdapter implements MultiValueMap<String, String> {

	private final MimeHeaders headers;


	TomcatHeadersAdapter(MimeHeaders headers) {
		this.headers = headers;
	}


	@Override
	public String getFirst(String key) {
		return this.headers.getHeader(key);
	}

	@Override
	public void add(String key, @Nullable String value) {
		this.headers.addValue(key).setString(value);
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
		this.headers.setValue(key).setString(value);
	}

	@Override
	public void setAll(Map<String, String> values) {
		values.forEach(this::set);
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		Map<String, String> singleValueMap = CollectionUtils.newLinkedHashMap(this.headers.size());
		this.keySet().forEach(key -> singleValueMap.put(key, getFirst(key)));
		return singleValueMap;
	}

	@Override
	public int size() {
		Enumeration<String> names = this.headers.names();
		int size = 0;
		while (names.hasMoreElements()) {
			size++;
			names.nextElement();
		}
		return size;
	}

	@Override
	public boolean isEmpty() {
		return (this.headers.size() == 0);
	}

	@Override
	public boolean containsKey(Object key) {
		if (key instanceof String headerName) {
			return (this.headers.findHeader(headerName, 0) != -1);
		}
		return false;
	}

	@Override
	public boolean containsValue(Object value) {
		if (value instanceof String text) {
			MessageBytes messageBytes = MessageBytes.newInstance();
			messageBytes.setString(text);
			for (int i = 0; i < this.headers.size(); i++) {
				if (this.headers.getValue(i).equals(messageBytes)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	@Nullable
	public List<String> get(Object key) {
		if (containsKey(key)) {
			return Collections.list(this.headers.values((String) key));
		}
		return null;
	}

	@Override
	@Nullable
	public List<String> put(String key, List<String> value) {
		List<String> previousValues = get(key);
		this.headers.removeHeader(key);
		value.forEach(v -> this.headers.addValue(key).setString(v));
		return previousValues;
	}

	@Override
	@Nullable
	public List<String> remove(Object key) {
		if (key instanceof String headerName) {
			List<String> previousValues = get(key);
			this.headers.removeHeader(headerName);
			return previousValues;
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
		return new HeaderNames();
	}

	@Override
	public Collection<List<String>> values() {
		return keySet().stream().map(this::get).collect(Collectors.toList());
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
		return HttpHeaders.formatHeaders(this);
	}


	private class EntryIterator implements Iterator<Entry<String, List<String>>> {

		private final Enumeration<String> names = headers.names();

		@Override
		public boolean hasNext() {
			return this.names.hasMoreElements();
		}

		@Override
		public Entry<String, List<String>> next() {
			return new HeaderEntry(this.names.nextElement());
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

		@Nullable
		@Override
		public List<String> getValue() {
			return get(this.key);
		}

		@Nullable
		@Override
		public List<String> setValue(List<String> value) {
			List<String> previous = getValue();
			headers.removeHeader(this.key);
			addAll(this.key, value);
			return previous;
		}
	}


	private class HeaderNames extends AbstractSet<String> {

		@Override
		public Iterator<String> iterator() {
			return new HeaderNamesIterator(headers.names());
		}

		@Override
		public int size() {
			Enumeration<String> names = headers.names();
			int size = 0;
			while (names.hasMoreElements()) {
				names.nextElement();
				size++;
			}
			return size;
		}
	}

	private final class HeaderNamesIterator implements Iterator<String> {

		private final Enumeration<String> enumeration;

		@Nullable
		private String currentName;

		private HeaderNamesIterator(Enumeration<String> enumeration) {
			this.enumeration = enumeration;
		}

		@Override
		public boolean hasNext() {
			return this.enumeration.hasMoreElements();
		}

		@Override
		public String next() {
			this.currentName = this.enumeration.nextElement();
			return this.currentName;
		}

		@Override
		public void remove() {
			if (this.currentName == null) {
				throw new IllegalStateException("No current Header in iterator");
			}
			int index = headers.findHeader(this.currentName, 0);
			if (index == -1) {
				throw new IllegalStateException("Header not present: " + this.currentName);
			}
			headers.removeHeader(index);
		}
	}

}
