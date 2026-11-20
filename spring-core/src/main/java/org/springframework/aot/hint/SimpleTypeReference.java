/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint;

import java.util.List;

import javax.lang.model.SourceVersion;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * A {@link TypeReference} based on fully qualified name.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
final class SimpleTypeReference extends AbstractTypeReference {

	private static final List<String> PRIMITIVE_NAMES = List.of("boolean", "byte",
			"short", "int", "long", "char", "float", "double", "void");

	private @Nullable String canonicalName;

	SimpleTypeReference(String packageName, String simpleName, @Nullable TypeReference enclosingType) {
		super(packageName, simpleName, enclosingType);
	}

	static SimpleTypeReference of(String className) {
		Assert.notNull(className, "'className' must not be null");
		if (!isValidClassName(className)) {
			throw new IllegalStateException("Invalid class name '" + className + "'");
		}
		if (!className.contains("$")) {
			return createTypeReference(className);
		}
		String[] elements = className.split("(?<!\\$)\\$(?!\\$)");
		SimpleTypeReference typeReference = createTypeReference(elements[0]);
		for (int i = 1; i < elements.length; i++) {
			typeReference = new SimpleTypeReference(typeReference.getPackageName(), elements[i], typeReference);
		}
		return typeReference;
	}

	private static boolean isValidClassName(String className) {
		for (String s : className.split("\\.", -1)) {
			String candidate = s.replace("[", "").replace("]", "");
			if (!SourceVersion.isIdentifier(candidate)) {
				return false;
			}
		}
		return true;
	}

	private static SimpleTypeReference createTypeReference(String className) {
		int i = className.lastIndexOf('.');
		if (i != -1) {
			return new SimpleTypeReference(className.substring(0, i), className.substring(i + 1), null);
		}
		else {
			String packageName = (isPrimitive(className) ? "java.lang" : "");
			return new SimpleTypeReference(packageName, className, null);
		}
	}

	@Override
	public String getCanonicalName() {
		if (this.canonicalName == null) {
			StringBuilder names = new StringBuilder();
			buildName(this, names);
			this.canonicalName = addPackageIfNecessary(names.toString());
		}
		return this.canonicalName;
	}

	@Override
	protected boolean isPrimitive() {
		return isPrimitive(getSimpleName());
	}

	private static boolean isPrimitive(String name) {
		return PRIMITIVE_NAMES.stream().anyMatch(name::startsWith);
	}

	private static void buildName(@Nullable TypeReference type, StringBuilder sb) {
		if (type == null) {
			return;
		}
		String typeName = (type.getEnclosingType() != null ? "." + type.getSimpleName() : type.getSimpleName());
		sb.insert(0, typeName);
		buildName(type.getEnclosingType(), sb);
	}

}
