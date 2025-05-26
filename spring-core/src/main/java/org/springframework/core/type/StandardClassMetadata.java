/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.core.type;

import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link ClassMetadata} implementation that uses standard reflection
 * to introspect a given {@code Class}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.5
 */
public class StandardClassMetadata implements ClassMetadata {

	private final Class<?> introspectedClass;


	/**
	 * Create a new StandardClassMetadata wrapper for the given Class.
	 * @param introspectedClass the Class to introspect
	 * @deprecated since 5.2 in favor of {@link StandardAnnotationMetadata}
	 */
	@Deprecated(since = "5.2")
	public StandardClassMetadata(Class<?> introspectedClass) {
		Assert.notNull(introspectedClass, "Class must not be null");
		this.introspectedClass = introspectedClass;
	}

	/**
	 * Return the underlying Class.
	 */
	public final Class<?> getIntrospectedClass() {
		return this.introspectedClass;
	}


	@Override
	public String getClassName() {
		return this.introspectedClass.getName();
	}

	@Override
	public boolean isInterface() {
		return this.introspectedClass.isInterface();
	}

	@Override
	public boolean isAnnotation() {
		return this.introspectedClass.isAnnotation();
	}

	@Override
	public boolean isAbstract() {
		return Modifier.isAbstract(this.introspectedClass.getModifiers());
	}

	@Override
	public boolean isFinal() {
		return Modifier.isFinal(this.introspectedClass.getModifiers());
	}

	@Override
	public boolean isIndependent() {
		return (!hasEnclosingClass() ||
				(this.introspectedClass.getDeclaringClass() != null &&
						Modifier.isStatic(this.introspectedClass.getModifiers())));
	}

	@Override
	public @Nullable String getEnclosingClassName() {
		Class<?> enclosingClass = this.introspectedClass.getEnclosingClass();
		return (enclosingClass != null ? enclosingClass.getName() : null);
	}

	@Override
	public @Nullable String getSuperClassName() {
		Class<?> superClass = this.introspectedClass.getSuperclass();
		return (superClass != null ? superClass.getName() : null);
	}

	@Override
	public String[] getInterfaceNames() {
		Class<?>[] ifcs = this.introspectedClass.getInterfaces();
		String[] ifcNames = new String[ifcs.length];
		for (int i = 0; i < ifcs.length; i++) {
			ifcNames[i] = ifcs[i].getName();
		}
		return ifcNames;
	}

	@Override
	public String[] getMemberClassNames() {
		LinkedHashSet<String> memberClassNames = new LinkedHashSet<>(4);
		for (Class<?> nestedClass : this.introspectedClass.getDeclaredClasses()) {
			memberClassNames.add(nestedClass.getName());
		}
		return StringUtils.toStringArray(memberClassNames);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof StandardClassMetadata that &&
				getIntrospectedClass().equals(that.getIntrospectedClass())));
	}

	@Override
	public int hashCode() {
		return getIntrospectedClass().hashCode();
	}

	@Override
	public String toString() {
		return getClassName();
	}

}
