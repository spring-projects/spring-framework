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

package org.springframework.http;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.util.CollectionUtils;
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

	private @Nullable MediaType cachedContentType;

	@SuppressWarnings("serial")
	private @Nullable List<MediaType> cachedAccept;

	ReadOnlyHttpHeaders(MultiValueMap<String, String> headers) {
		super(headers);
	}


	@Override
	public @Nullable MediaType getContentType() {
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
	public @Nullable List<String> get(String headerName) {
		List<String> values = this.headers.get(headerName);
		return (values != null ? Collections.unmodifiableList(values) : null);
	}

	@Override
	public void add(String headerName, @Nullable String headerValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addAll(String key, List<? extends String> headerValues) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addAll(HttpHeaders values) {
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

	@SuppressWarnings("removal")
	@Override
	@Deprecated(since = "7.0", forRemoval = true)
	public Map<String, String> asSingleValueMap() {
		return Collections.unmodifiableMap(this.headers.asSingleValueMap());
	}

	@SuppressWarnings("removal")
	@Override
	@Deprecated(since = "7.0", forRemoval = true)
	public MultiValueMap<String, String> asMultiValueMap() {
		return CollectionUtils.unmodifiableMultiValueMap(this.headers);
	}

	@Override
	public Set<String> headerNames() {
		return Collections.unmodifiableSet(super.headerNames());
	}


	@Override
	public List<String> put(String key, List<String> value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public @Nullable List<String> putIfAbsent(String headerName, List<String> headerValues) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(@Nullable HttpHeaders values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> headers) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<String> remove(String key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Entry<String, List<String>>> headerSet() {
		return super.headerSet().stream().map(SimpleImmutableEntry::new)
				.collect(Collectors.collectingAndThen(
						Collectors.toCollection(LinkedHashSet::new), // Retain original ordering of entries
						Collections::unmodifiableSet));
	}

	@Override
	public void forEach(BiConsumer<? super String, ? super List<String>> action) {
		this.headers.forEach((k, vs) -> action.accept(k, Collections.unmodifiableList(vs)));
	}

}
