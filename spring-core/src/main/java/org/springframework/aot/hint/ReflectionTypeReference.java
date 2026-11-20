/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * A {@link TypeReference} based on a {@link Class}.
 *
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 * @since 6.0
 */
final class ReflectionTypeReference extends AbstractTypeReference {

	private final Class<?> type;

	private ReflectionTypeReference(Class<?> type) {
		super(type.getPackageName(), type.getSimpleName(), getEnclosingClass(type));
		this.type = type;
	}

	private static @Nullable TypeReference getEnclosingClass(Class<?> type) {
		Class<?> candidate = (type.isArray() ? type.componentType().getEnclosingClass() :
				type.getEnclosingClass());
		return (candidate != null ? new ReflectionTypeReference(candidate) : null);
	}

	static ReflectionTypeReference of(Class<?> type) {
		Assert.notNull(type, "'type' must not be null");
		Assert.notNull(type.getCanonicalName(), "'type.getCanonicalName()' must not be null");
		return new ReflectionTypeReference(type);
	}

	@Override
	public String getCanonicalName() {
		return this.type.getCanonicalName();
	}

	@Override
	protected boolean isPrimitive() {
		return this.type.isPrimitive() ||
				(this.type.isArray() && this.type.componentType().isPrimitive());
	}

}
