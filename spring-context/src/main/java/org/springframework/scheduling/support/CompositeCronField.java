/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.support;

import java.time.temporal.Temporal;

import org.jspecify.annotations.Nullable;

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


	@Override
	public <T extends Temporal & Comparable<? super T>> @Nullable T nextOrSame(T temporal) {
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
	public boolean equals(@Nullable Object o) {
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
