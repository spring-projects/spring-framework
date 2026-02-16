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
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.MimeHeaders;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

/**
 * {@code MultiValueMap} implementation for wrapping Tomcat HTTP headers.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 * @author Simon Baslé
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
		for (String value : values) {
			add(key, value);
		}
	}

	@Override
	public void addAll(MultiValueMap<String, String> values) {
		for (Entry<String, List<String>> entry : values.entrySet()) {
			addAll(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void set(String key, @Nullable String value) {
		this.headers.setValue(key).setString(value);
	}

	@Override
	public void setAll(Map<String, String> values) {
		for (Entry<String, String> entry : values.entrySet()) {
			set(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		Map<String, String> map = CollectionUtils.newLinkedHashMap(this.headers.size());
		for (String name : this.keySet()) {
			map.put(name, getFirst(name));
		}
		return map;
	}

	@Override
	public int size() {
		Enumeration<String> names = this.headers.names();
		Set<String> set = new LinkedHashSet<>(this.headers.size());
		while (names.hasMoreElements()) {
			set.add(names.nextElement().toLowerCase(Locale.ROOT));
		}
		return set.size();
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
			MessageBytes bytes = MessageBytes.newInstance();
			bytes.setString(text);
			for (int i = 0; i < this.headers.size(); i++) {
				if (this.headers.getValue(i).equals(bytes)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public @Nullable List<String> get(Object key) {
		if (key instanceof String headerName) {
			Enumeration<String> values = this.headers.values(headerName);
			if (values.hasMoreElements()) {
				return Collections.list(values);
			}
		}
		return null;
	}

	@Override
	public @Nullable List<String> put(String key, List<String> value) {
		List<String> previous = get(key);
		this.headers.removeHeader(key);
		value.forEach(v -> this.headers.addValue(key).setString(v));
		return previous;
	}

	@Override
	public @Nullable List<String> remove(Object key) {
		if (key instanceof String headerName) {
			List<String> previous = get(key);
			this.headers.removeHeader(headerName);
			return previous;
		}
		return null;
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		for (Entry<? extends String, ? extends List<String>> entry : map.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void clear() {
		this.headers.recycle();
	}

	@Override
	public Set<String> keySet() {
		return new HeaderNames();
	}

	@Override
	public Collection<List<String>> values() {
		return keySet().stream().map(this::get).toList();
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
				return TomcatHeadersAdapter.this.size();
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

		@Override
		public @Nullable List<String> getValue() {
			return get(this.key);
		}

		@Override
		public @Nullable List<String> setValue(List<String> value) {
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

		private @Nullable String currentName;

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
			//implement a mix of removeHeader(String) and removeHeader(int)
			boolean found = false;
			for (int i = 0; i < headers.size(); i++) {
				if (headers.getName(i).equalsIgnoreCase(this.currentName)) {
					headers.removeHeader(i--);
					found = true;
				}
			}
			if (!found) {
				throw new IllegalStateException("Header not present: " + this.currentName);
			}
		}
	}

}
