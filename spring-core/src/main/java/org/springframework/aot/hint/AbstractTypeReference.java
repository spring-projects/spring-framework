/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Base {@link TypeReference} implementation that ensures consistent behavior
 * for {@code equals()}, {@code hashCode()}, and {@code toString()} based on
 * the {@linkplain #getCanonicalName() canonical name}.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public abstract class AbstractTypeReference implements TypeReference {

	private final String packageName;

	private final String simpleName;

	private final @Nullable TypeReference enclosingType;


	protected AbstractTypeReference(String packageName, String simpleName, @Nullable TypeReference enclosingType) {
		this.packageName = packageName;
		this.simpleName = simpleName;
		this.enclosingType = enclosingType;
	}


	@Override
	public String getName() {
		TypeReference enclosingType = getEnclosingType();
		String simpleName = getSimpleName();
		return (enclosingType != null ? (enclosingType.getName() + '$' + simpleName) :
				addPackageIfNecessary(simpleName));
	}

	@Override
	public String getPackageName() {
		return this.packageName;
	}

	@Override
	public String getSimpleName() {
		return this.simpleName;
	}

	@Override
	public @Nullable TypeReference getEnclosingType() {
		return this.enclosingType;
	}

	protected String addPackageIfNecessary(String part) {
		if (this.packageName.isEmpty() ||
				(this.packageName.equals("java.lang") && isPrimitive())) {
			return part;
		}
		return this.packageName + '.' + part;
	}

	protected abstract boolean isPrimitive();

	@Override
	public int compareTo(TypeReference other) {
		return getCanonicalName().compareToIgnoreCase(other.getCanonicalName());
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof TypeReference that &&
				getCanonicalName().equals(that.getCanonicalName())));
	}

	@Override
	public int hashCode() {
		return Objects.hash(getCanonicalName());
	}

	@Override
	public String toString() {
		return getCanonicalName();
	}

}
