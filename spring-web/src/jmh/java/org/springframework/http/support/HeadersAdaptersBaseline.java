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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpMessage;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

class HeadersAdaptersBaseline {

	static final class HttpComponents implements MultiValueMap<String, String> {

		private final HttpMessage message;


		/**
		 * Create a new {@code HttpComponentsHeadersAdapter} based on the given
		 * {@code HttpMessage}.
		 */
		public HttpComponents(HttpMessage message) {
			Assert.notNull(message, "Message must not be null");
			this.message = message;
		}


		@Override
		@Nullable
		public String getFirst(String key) {
			Header header = this.message.getFirstHeader(key);
			return (header != null ? header.getValue() : null);
		}

		@Override
		public void add(String key, @Nullable String value) {
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
			this.message.setHeader(key, value);
		}

		@Override
		public void setAll(Map<String, String> values) {
			values.forEach(this::set);
		}

		@Override
		public Map<String, String> toSingleValueMap() {
			Map<String, String> map = CollectionUtils.newLinkedHashMap(size());
			this.message.headerIterator().forEachRemaining(h -> map.putIfAbsent(h.getName(), h.getValue()));
			return map;
		}

		@Override
		public int size() {
			return this.message.getHeaders().length;
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

		@Nullable
		@Override
		public List<String> get(Object key) {
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

		@Nullable
		@Override
		public List<String> put(String key, List<String> values) {
			List<String> oldValues = remove(key);
			values.forEach(value -> add(key, value));
			return oldValues;
		}

		@Nullable
		@Override
		public List<String> remove(Object key) {
			if (key instanceof String headerName) {
				List<String> oldValues = get(key);
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
			this.message.setHeaders();
		}

		@Override
		public Set<String> keySet() {
			Set<String> keys = new LinkedHashSet<>(size());
			for (Header header : this.message.getHeaders()) {
				keys.add(header.getName());
			}
			return keys;
		}

		@Override
		public Collection<List<String>> values() {
			Collection<List<String>> values = new ArrayList<>(size());
			for (Header header : this.message.getHeaders()) {
				values.add(get(header.getName()));
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
					return HttpComponents.this.size();
				}
			};
		}


		@Override
		public String toString() {
			return HttpHeaders.formatHeaders(this);
		}


		private class EntryIterator implements Iterator<Entry<String, List<String>>> {

			private final Iterator<Header> iterator = message.headerIterator();

			@Override
			public boolean hasNext() {
				return this.iterator.hasNext();
			}

			@Override
			public Entry<String, List<String>> next() {
				return new HeaderEntry(this.iterator.next().getName());
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
				List<String> values = HttpComponents.this.get(this.key);
				return values != null ? values : Collections.emptyList();
			}

			@Override
			public List<String> setValue(List<String> value) {
				List<String> previousValues = getValue();
				HttpComponents.this.put(this.key, value);
				return previousValues;
			}
		}

	}

	static final class Jetty implements MultiValueMap<String, String> {

		private final HttpFields headers;

		@Nullable
		private final HttpFields.Mutable mutable;


		/**
		 * Creates a new {@code JettyHeadersAdapter} based on the given
		 * {@code HttpFields} instance.
		 * @param headers the {@code HttpFields} to base this adapter on
		 */
		public Jetty(HttpFields headers) {
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

	static final class Netty4 implements MultiValueMap<String, String> {

		private final io.netty.handler.codec.http.HttpHeaders headers;

		/**
		 * Creates a new {@code Netty4HeadersAdapter} based on the given
		 * {@code HttpHeaders}.
		 */
		public Netty4(io.netty.handler.codec.http.HttpHeaders headers) {
			Assert.notNull(headers, "Headers must not be null");
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
			return (key instanceof String headerName && this.headers.contains(headerName));
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
			return HttpHeaders.formatHeaders(this);
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

	static final class Netty5 implements MultiValueMap<String, String> {

		private final io.netty5.handler.codec.http.headers.HttpHeaders headers;


		/**
		 * Create a new {@code Netty5HeadersAdapter} based on the given
		 * {@code HttpHeaders}.
		 */
		public Netty5(io.netty5.handler.codec.http.headers.HttpHeaders headers) {
			Assert.notNull(headers, "Headers must not be null");
			this.headers = headers;
		}


		@Override
		@Nullable
		public String getFirst(String key) {
			CharSequence value = this.headers.get(key);
			return (value != null ? value.toString() : null);
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
			this.headers.forEach(entry -> singleValueMap.putIfAbsent(
					entry.getKey().toString(), entry.getValue().toString()));
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
					StreamSupport.stream(this.headers.spliterator(), false)
							.anyMatch(entry -> value.equals(entry.getValue())));
		}

		@Override
		@Nullable
		public List<String> get(Object key) {
			Iterator<CharSequence> iterator = this.headers.valuesIterator((CharSequence) key);
			if (iterator.hasNext()) {
				List<String> result = new ArrayList<>();
				iterator.forEachRemaining(value -> result.add(value.toString()));
				return result;
			}
			return null;
		}

		@Nullable
		@Override
		public List<String> put(String key, @Nullable List<String> value) {
			List<String> previousValues = get(key);
			this.headers.set(key, value);
			return previousValues;
		}

		@Nullable
		@Override
		public List<String> remove(Object key) {
			if (key instanceof String headerName) {
				List<String> previousValues = get(headerName);
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
			List<List<String>> result = new ArrayList<>(this.headers.size());
			forEach((key, value) -> result.add(value));
			return result;
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

			private final Iterator<CharSequence> names = headers.names().iterator();

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

			private final CharSequence key;

			HeaderEntry(CharSequence key) {
				this.key = key;
			}

			@Override
			public String getKey() {
				return this.key.toString();
			}

			@Override
			public List<String> getValue() {
				List<String> values = get(this.key);
				return (values != null ? values : Collections.emptyList());
			}

			@Override
			public List<String> setValue(List<String> value) {
				List<String> previousValues = getValue();
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

			private final Iterator<CharSequence> iterator;

			@Nullable
			private CharSequence currentName;

			private HeaderNamesIterator(Iterator<CharSequence> iterator) {
				this.iterator = iterator;
			}

			@Override
			public boolean hasNext() {
				return this.iterator.hasNext();
			}

			@Override
			public String next() {
				this.currentName = this.iterator.next();
				return this.currentName.toString();
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
}
