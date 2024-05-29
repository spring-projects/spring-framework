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

package org.springframework.http;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/**
 * {@code HttpHeaders} object that can only be read, not written to.
 * <p>This caches the parsed representations of the "Accept" and "Content-Type" headers
 * and will get out of sync with the backing map it is mutated at runtime.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 5.1.1
 */
class ReadOnlyHttpHeaders extends HttpHeaders {

	private static final long serialVersionUID = -8578554704772377436L;

	@Nullable
	private MediaType cachedContentType;

	@Nullable
	@SuppressWarnings("serial")
	private List<MediaType> cachedAccept;


	ReadOnlyHttpHeaders(MultiValueMap<String, String> headers) {
		super(headers);
	}


	@Override
	@Nullable
	public MediaType getContentType() {
		if (this.cachedContentType != null) {
			return this.cachedContentType;
		}
		else {
			MediaType contentType = super.getContentType();
			this.cachedContentType = contentType;
			return contentType;
		}
	}

	@Override
	public List<MediaType> getAccept() {
		if (this.cachedAccept != null) {
			return this.cachedAccept;
		}
		else {
			List<MediaType> accept = super.getAccept();
			this.cachedAccept = accept;
			return accept;
		}
	}

	@Override
	public void clearContentHeaders() {
		// No-op.
	}

	@Override
	@Nullable
	public List<String> get(Object key) {
		List<String> values = this.headers.get(key);
		return (values != null ? Collections.unmodifiableList(values) : null);
	}

	@Override
	public void add(String headerName, @Nullable String headerValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addAll(String key, List<? extends String> values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addAll(MultiValueMap<String, String> values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void set(String headerName, @Nullable String headerValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setAll(Map<String, String> values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		return Collections.unmodifiableMap(this.headers.toSingleValueMap());
	}

	@Override
	public Map<String, String> asSingleValueMap() {
		return Collections.unmodifiableMap(this.headers.asSingleValueMap());
	}

	@Override
	public Set<String> keySet() {
		return Collections.unmodifiableSet(this.headers.keySet());
	}

	@Override
	public List<String> put(String key, List<String> value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<String> remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<List<String>> values() {
		return Collections.unmodifiableCollection(this.headers.values());
	}

	@Override
	public Set<Entry<String, List<String>>> entrySet() {
		return this.headers.entrySet().stream().map(SimpleImmutableEntry::new)
				.collect(Collectors.collectingAndThen(
						Collectors.toCollection(LinkedHashSet::new), // Retain original ordering of entries
						Collections::unmodifiableSet));
	}

	@Override
	public void forEach(BiConsumer<? super String, ? super List<String>> action) {
		this.headers.forEach((k, vs) -> action.accept(k, Collections.unmodifiableList(vs)));
	}

}
