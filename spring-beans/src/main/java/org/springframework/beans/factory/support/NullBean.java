/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.support;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.FactoryBean;

/**
 * Internal representation of a null bean instance, for example, for a {@code null} value
 * returned from {@link FactoryBean#getObject()} or from a factory method.
 *
 * <p>Each such null bean is represented by a dedicated {@code NullBean} instance
 * which are not equal to each other, uniquely differentiating each bean as returned
 * from all variants of {@link org.springframework.beans.factory.BeanFactory#getBean}.
 * However, each such instance will return {@code true} for {@code #equals(null)}
 * and returns "null" from {@code #toString()}, which is how they can be tested
 * externally (since this class itself is not public).
 *
 * @author Juergen Hoeller
 * @since 5.0
 */
final class NullBean {

	NullBean() {
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || other == null);
	}

	@Override
	public int hashCode() {
		return NullBean.class.hashCode();
	}

	@Override
	public String toString() {
		return "null";
	}

}
