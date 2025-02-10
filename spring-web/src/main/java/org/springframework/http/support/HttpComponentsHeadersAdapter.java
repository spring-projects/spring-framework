/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpMessage;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.MultiValueMap;

/**
 * {@code MultiValueMap} implementation for wrapping Apache HttpComponents
 * HttpClient headers.
 *
 * @author Rossen Stoyanchev
 * @author Simon Baslé
 * @since 6.1
 */
public final class HttpComponentsHeadersAdapter implements MultiValueMap<String, String> {

	private final HttpMessage message;
	private final Set<String> headerNames;


	/**
	 * Create a new {@code HttpComponentsHeadersAdapter} based on the given
	 * {@code HttpMessage}.
	 */
	public HttpComponentsHeadersAdapter(HttpMessage message) {
		Assert.notNull(message, "Message must not be null");
		this.message = message;
		this.headerNames = Collections.newSetFromMap(new LinkedCaseInsensitiveMap<>(
				message.getHeaders().length, Locale.ROOT));
		for (Header header : message.getHeaders()) {
			this.headerNames.add(header.getName());
		}
	}


	@Override
	public @Nullable String getFirst(String key) {
		Header header = this.message.getFirstHeader(key);
		return (header != null ? header.getValue() : null);
	}

	@Override
	public void add(String key, @Nullable String value) {
		this.headerNames.add(key);
		this.message.addHeader(key, value);
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
		this.headerNames.add(key);
		this.message.setHeader(key, value);
	}

	@Override
	public void setAll(Map<String, String> values) {
		values.forEach(this::set);
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		Map<String, String> map = new LinkedCaseInsensitiveMap<>(this.message.getHeaders().length, Locale.ROOT);
		this.message.headerIterator().forEachRemaining(h -> map.putIfAbsent(h.getName(), h.getValue()));
		return map;
	}

	@Override
	public int size() {
		return this.headerNames.size();
	}

	@Override
	public boolean isEmpty() {
		return (this.message.getHeaders().length == 0);
	}

	@Override
	public boolean containsKey(Object key) {
		return (key instanceof String headerName && this.message.containsHeader(headerName));
	}

	@Override
	public boolean containsValue(Object value) {
		return (value instanceof String &&
				Arrays.stream(this.message.getHeaders()).anyMatch(h -> h.getValue().equals(value)));
	}

	@Override
	public @Nullable List<String> get(Object key) {
		List<String> values = null;
		if (containsKey(key)) {
			Header[] headers = this.message.getHeaders((String) key);
			values = new ArrayList<>(headers.length);
			for (Header header : headers) {
				values.add(header.getValue());
			}
		}
		return values;
	}

	@Override
	public @Nullable List<String> put(String key, List<String> values) {
		List<String> oldValues = remove(key);
		values.forEach(value -> add(key, value));
		return oldValues;
	}

	@Override
	public @Nullable List<String> remove(Object key) {
		if (key instanceof String headerName) {
			List<String> oldValues = get(key);
			this.headerNames.remove(headerName);
			this.message.removeHeaders(headerName);
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
		this.headerNames.clear();
		this.message.setHeaders();
	}

	@Override
	public Set<String> keySet() {
		return new HeaderNames();
	}

	@Override
	public Collection<List<String>> values() {
		Collection<List<String>> values = new ArrayList<>(this.message.getHeaders().length);
		for (String headerName : keySet()) {
			values.add(get(headerName));
		}
		return values;
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
				return HttpComponentsHeadersAdapter.this.size();
			}
		};
	}


	@Override
	public String toString() {
		return HttpHeaders.formatHeaders(this);
	}

	private class HeaderNames extends AbstractSet<String> {

		@Override
		public Iterator<String> iterator() {
			return new HeaderNamesIterator(headerNames.iterator());
		}

		@Override
		public int size() {
			return headerNames.size();
		}
	}

	private class EntryIterator implements Iterator<Entry<String, List<String>>> {

		private final Iterator<String> iterator = keySet().iterator();

		@Override
		public boolean hasNext() {
			return this.iterator.hasNext();
		}

		@Override
		public Entry<String, List<String>> next() {
			return new HeaderEntry(this.iterator.next());
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
			List<String> values = HttpComponentsHeadersAdapter.this.get(this.key);
			return values != null ? values : Collections.emptyList();
		}

		@Override
		public List<String> setValue(List<String> value) {
			List<String> previousValues = getValue();
			HttpComponentsHeadersAdapter.this.put(this.key, value);
			return previousValues;
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
			if (!message.containsHeader(this.currentName)) {
				throw new IllegalStateException("Header not present: " + this.currentName);
			}
			headerNames.remove(this.currentName);
			message.removeHeaders(this.currentName);
		}
	}


}
