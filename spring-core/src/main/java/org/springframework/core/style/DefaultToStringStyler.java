/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.style;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Spring's default {@code toString()} styler.
 *
 * <p>This class is used by {@link ToStringCreator} to style {@code toString()}
 * output in a consistent manner according to Spring conventions.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 1.2.2
 */
public class DefaultToStringStyler implements ToStringStyler {

	private final ValueStyler valueStyler;


	/**
	 * Create a new DefaultToStringStyler.
	 * @param valueStyler the ValueStyler to use
	 */
	public DefaultToStringStyler(ValueStyler valueStyler) {
		Assert.notNull(valueStyler, "ValueStyler must not be null");
		this.valueStyler = valueStyler;
	}

	/**
	 * Return the ValueStyler used by this ToStringStyler.
	 */
	protected final ValueStyler getValueStyler() {
		return this.valueStyler;
	}


	@Override
	public void styleStart(StringBuilder buffer, Object obj) {
		if (!obj.getClass().isArray()) {
			buffer.append('[').append(ClassUtils.getShortName(obj.getClass()));
			styleIdentityHashCode(buffer, obj);
		}
		else {
			buffer.append('[');
			styleIdentityHashCode(buffer, obj);
			buffer.append(' ');
			styleValue(buffer, obj);
		}
	}

	private void styleIdentityHashCode(StringBuilder buffer, Object obj) {
		buffer.append('@');
		buffer.append(ObjectUtils.getIdentityHexString(obj));
	}

	@Override
	public void styleEnd(StringBuilder buffer, Object o) {
		buffer.append(']');
	}

	@Override
	public void styleField(StringBuilder buffer, String fieldName, @Nullable Object value) {
		styleFieldStart(buffer, fieldName);
		styleValue(buffer, value);
		styleFieldEnd(buffer, fieldName);
	}

	protected void styleFieldStart(StringBuilder buffer, String fieldName) {
		buffer.append(' ').append(fieldName).append(" = ");
	}

	protected void styleFieldEnd(StringBuilder buffer, String fieldName) {
	}

	@Override
	public void styleValue(StringBuilder buffer, @Nullable Object value) {
		buffer.append(this.valueStyler.style(value));
	}

	@Override
	public void styleFieldSeparator(StringBuilder buffer) {
		buffer.append(',');
	}

}
