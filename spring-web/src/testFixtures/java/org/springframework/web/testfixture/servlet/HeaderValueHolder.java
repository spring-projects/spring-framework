/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.testfixture.servlet;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * Internal helper class that serves as a value holder for request headers.
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 * @since 2.0.1
 */
class HeaderValueHolder {

	private final List<Object> values = new LinkedList<>();


	void setValue(@Nullable Object value) {
		this.values.clear();
		if (value != null) {
			this.values.add(value);
		}
	}

	void addValue(Object value) {
		this.values.add(value);
	}

	void addValues(Collection<?> values) {
		this.values.addAll(values);
	}

	void addValueArray(Object values) {
		CollectionUtils.mergeArrayIntoCollection(values, this.values);
	}

	List<Object> getValues() {
		return Collections.unmodifiableList(this.values);
	}

	List<String> getStringValues() {
		return this.values.stream().map(Object::toString).toList();
	}

	@Nullable
	Object getValue() {
		return (!this.values.isEmpty() ? this.values.get(0) : null);
	}

	@Nullable
	String getStringValue() {
		return (!this.values.isEmpty() ? String.valueOf(this.values.get(0)) : null);
	}

	@Override
	public String toString() {
		return this.values.toString();
	}

}
