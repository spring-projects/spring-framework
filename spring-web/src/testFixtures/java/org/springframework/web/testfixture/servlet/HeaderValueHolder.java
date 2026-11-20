/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.testfixture.servlet;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jspecify.annotations.Nullable;

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

	@Nullable Object getValue() {
		return (!this.values.isEmpty() ? this.values.get(0) : null);
	}

	@Nullable String getStringValue() {
		return (!this.values.isEmpty() ? String.valueOf(this.values.get(0)) : null);
	}

	@Override
	public String toString() {
		return this.values.toString();
	}

}
