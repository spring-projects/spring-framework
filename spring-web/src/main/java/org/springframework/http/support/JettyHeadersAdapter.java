/*
 * Copyright 2002-2023 the original author or authors.
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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

/**
 * {@code MultiValueMap} implementation for wrapping Jetty HTTP headers.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 6.1
 */
public final class JettyHeadersAdapter implements MultiValueMap<String, String> {

	private final HttpFields headers;

	@Nullable
	private final HttpFields.Mutable mutable;


	/**
	 * Creates a new {@code JettyHeadersAdapter} based on the given
	 * {@code HttpFields} instance.
	 * @param headers the {@code HttpFields} to base this adapter on
	 */
	public JettyHeadersAdapter(HttpFields headers) {
		Assert.notNull(headers, "Headers must not be null");
		this.headers = headers;
		this.mutable = headers instanceof HttpFields.Mutable m ? m : null;
	}


	@Override
	public String getFirst(String key) {
		return this.headers.get(key);
	}

	@Override
	public void add(String key, @Nullable String value) {
		if (value != null) {
			HttpFields.Mutable mutableHttpFields = mutableFields();
			mutableHttpFields.add(key, value);
		}
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
		HttpFields.Mutable mutableHttpFields = mutableFields();
		if (value != null) {
			mutableHttpFields.put(key, value);
		}
		else {
			mutableHttpFields.remove(key);
		}
	}

	@Override
	public void setAll(Map<String, String> values) {
		values.forEach(this::set);
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		Map<String, String> singleValueMap = CollectionUtils.newLinkedHashMap(this.headers.size());
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
		return (key instanceof String name && this.headers.contains(name));
	}

	@Override
	public boolean containsValue(Object value) {
		if (value instanceof String searchString) {
			for (HttpField field : this.headers) {
				if (field.contains(searchString)) {
					return true;
				}
			}
		}
		return false;
	}

	@Nullable
	@Override
	public List<String> get(Object key) {
		List<String> list = null;
		if (key instanceof String name) {
			for (HttpField f : this.headers) {
				if (f.is(name)) {
					if (list == null) {
						list = new ArrayList<>();
					}
					list.add(f.getValue());
				}
			}
		}
		return list;
	}

	@Nullable
	@Override
	public List<String> put(String key, List<String> value) {
		HttpFields.Mutable mutableHttpFields = mutableFields();
		List<String> oldValues = get(key);

		if (oldValues == null) {
			switch (value.size()) {
				case 0 -> {}
				case 1 -> mutableHttpFields.add(key, value.get(0));
				default -> mutableHttpFields.add(key, value);
			}
		}
		else {
			switch (value.size()) {
				case 0 -> mutableHttpFields.remove(key);
				case 1 -> mutableHttpFields.put(key, value.get(0));
				default -> mutableHttpFields.put(key, value);
			}
		}
		return oldValues;
	}

	@Nullable
	@Override
	public List<String> remove(Object key) {
		HttpFields.Mutable mutableHttpFields = mutableFields();
		List<String> list = null;
		if (key instanceof String name) {
			for (ListIterator<HttpField> i = mutableHttpFields.listIterator(); i.hasNext(); ) {
				HttpField f = i.next();
				if (f.is(name)) {
					if (list == null) {
						list = new ArrayList<>();
					}
					list.add(f.getValue());
					i.remove();
				}
			}
		}
		return list;
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		map.forEach(this::put);
	}

	@Override
	public void clear() {
		HttpFields.Mutable mutableHttpFields = mutableFields();
		mutableHttpFields.clear();
	}

	@Override
	public Set<String> keySet() {
		return new HeaderNames();
	}

	@Override
	public Collection<List<String>> values() {
		return this.headers.getFieldNamesCollection().stream()
				.map(this.headers::getValuesList).toList();
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

	private HttpFields.Mutable mutableFields() {
		if (this.mutable == null) {
			throw new IllegalStateException("Immutable headers");
		}
		return this.mutable;
	}

	@Override
	public String toString() {
		return HttpHeaders.formatHeaders(this);
	}


	private class EntryIterator implements Iterator<Entry<String, List<String>>> {

		private final Iterator<String> names = headers.getFieldNamesCollection().iterator();

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
			return headers.getValuesList(this.key);
		}

		@Override
		public List<String> setValue(List<String> value) {
			HttpFields.Mutable mutableHttpFields = mutableFields();
			List<String> previousValues = headers.getValuesList(this.key);
			mutableHttpFields.put(this.key, value);
			return previousValues;
		}
	}


	private class HeaderNames extends AbstractSet<String> {

		@Override
		public Iterator<String> iterator() {
			return new HeaderNamesIterator(headers.getFieldNamesCollection().iterator());
		}

		@Override
		public int size() {
			return headers.getFieldNamesCollection().size();
		}
	}


	private final class HeaderNamesIterator implements Iterator<String> {

		private final Iterator<String> iterator;

		@Nullable
		private String currentName;

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
			HttpFields.Mutable mutableHttpFields = mutableFields();
			if (this.currentName == null) {
				throw new IllegalStateException("No current Header in iterator");
			}
			if (!headers.contains(this.currentName)) {
				throw new IllegalStateException("Header not present: " + this.currentName);
			}
			mutableHttpFields.remove(this.currentName);
		}
	}

}
