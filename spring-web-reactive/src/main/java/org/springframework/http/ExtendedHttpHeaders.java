/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.http;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Variant of HttpHeaders (to be merged into HttpHeaders) that supports the
 * registration of {@link HeaderChangeListener}s.
 *
 * <p>For use with HTTP server response implementations that wish to propagate
 * header header changes to the underlying runtime as they occur.
 *
 * @author Rossen Stoyanchev
 */
public class ExtendedHttpHeaders extends HttpHeaders {

	private final List<HeaderChangeListener> listeners = new ArrayList<>(1);


	public ExtendedHttpHeaders() {
	}

	public ExtendedHttpHeaders(HeaderChangeListener listener) {
		this.listeners.add(listener);
	}


	@Override
	public void add(String name, String value) {
		for (HeaderChangeListener listener : this.listeners) {
			listener.headerAdded(name, value);
		}
		super.add(name, value);
	}

	@Override
	public void set(String name, String value) {
		List<String> values = new LinkedList<>();
		values.add(value);
		put(name, values);
	}

	@Override
	public List<String> put(String key, List<String> values) {
		for (HeaderChangeListener listener : this.listeners) {
			listener.headerPut(key, values);
		}
		return super.put(key, values);
	}

	@Override
	public List<String> remove(Object key) {
		for (HeaderChangeListener listener : this.listeners) {
			listener.headerRemoved((String) key);
		}
		return super.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		for (Entry<? extends String, ? extends List<String>> entry : map.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
		super.putAll(map);
	}

	@Override
	public void clear() {
		for (Entry<? extends String, ? extends List<String>> entry : super.entrySet()) {
			remove(entry.getKey(), entry.getValue());
		}
		super.clear();
	}


	public interface HeaderChangeListener {

		void headerAdded(String name, String value);

		void headerPut(String key, List<String> values);

		void headerRemoved(String key);

	}

}
