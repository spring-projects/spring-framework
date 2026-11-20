/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.support;

import java.io.Serializable;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.ClassFilter;
import org.springframework.util.Assert;

/**
 * Simple ClassFilter implementation that passes classes (and optionally subclasses).
 *
 * @author Rod Johnson
 * @author Sam Brannen
 */
@SuppressWarnings("serial")
public class RootClassFilter implements ClassFilter, Serializable {

	private final Class<?> clazz;


	public RootClassFilter(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		this.clazz = clazz;
	}


	@Override
	public boolean matches(Class<?> candidate) {
		return this.clazz.isAssignableFrom(candidate);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof RootClassFilter that &&
				this.clazz.equals(that.clazz)));
	}

	@Override
	public int hashCode() {
		return this.clazz.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + this.clazz.getName();
	}

}
