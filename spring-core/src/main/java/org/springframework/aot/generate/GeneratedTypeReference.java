/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.generate;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.AbstractTypeReference;
import org.springframework.aot.hint.TypeReference;
import org.springframework.javapoet.ClassName;
import org.springframework.util.Assert;

/**
 * A {@link TypeReference} for a generated {@linkplain ClassName type}.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public final class GeneratedTypeReference extends AbstractTypeReference {

	private final ClassName className;

	private GeneratedTypeReference(ClassName className) {
		super(className.packageName(), className.simpleName(), safeCreate(className.enclosingClassName()));
		this.className = className;
	}

	private static @Nullable GeneratedTypeReference safeCreate(@Nullable ClassName className) {
		return (className != null ? new GeneratedTypeReference(className) : null);
	}

	public static GeneratedTypeReference of(ClassName className) {
		Assert.notNull(className, "ClassName must not be null");
		return new GeneratedTypeReference(className);
	}

	@Override
	public String getCanonicalName() {
		return this.className.canonicalName();
	}

	@Override
	protected boolean isPrimitive() {
		return this.className.isPrimitive();
	}

}
