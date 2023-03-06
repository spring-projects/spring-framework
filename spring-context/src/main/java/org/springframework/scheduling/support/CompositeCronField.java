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

package org.springframework.scheduling.support;

import java.time.temporal.Temporal;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Extension of {@link CronField} that wraps an array of cron fields.
 *
 * @author Arjen Poutsma
 * @since 5.3.3
 */
final class CompositeCronField extends CronField {

	private final CronField[] fields;

	private final String value;


	private CompositeCronField(Type type, CronField[] fields, String value) {
		super(type);
		this.fields = fields;
		this.value = value;
	}

	/**
	 * Composes the given fields into a {@link CronField}.
	 */
	public static CronField compose(CronField[] fields, Type type, String value) {
		Assert.notEmpty(fields, "Fields must not be empty");
		Assert.hasLength(value, "Value must not be empty");

		if (fields.length == 1) {
			return fields[0];
		}
		else {
			return new CompositeCronField(type, fields, value);
		}
	}


	@Nullable
	@Override
	public <T extends Temporal & Comparable<? super T>> T nextOrSame(T temporal) {
		T result = null;
		for (CronField field : this.fields) {
			T candidate = field.nextOrSame(temporal);
			if (result == null ||
					candidate != null && candidate.compareTo(result) < 0) {
				result = candidate;
			}
		}
		return result;
	}


	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CompositeCronField other)) {
			return false;
		}
		return type() == other.type() &&
				this.value.equals(other.value);
	}

	@Override
	public String toString() {
		return type() + " '" + this.value + "'";

	}
}
