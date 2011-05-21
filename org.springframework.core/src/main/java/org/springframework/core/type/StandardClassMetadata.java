/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.util.Assert;

/**
 * {@link ClassMetadata} implementation that uses standard reflection
 * to introspect a given <code>Class</code>.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
public class StandardClassMetadata implements ClassMetadata {

	private final Class introspectedClass;


	/**
	 * Create a new StandardClassMetadata wrapper for the given Class.
	 * @param introspectedClass the Class to introspect
	 */
	public StandardClassMetadata(Class introspectedClass) {
		Assert.notNull(introspectedClass, "Class must not be null");
		this.introspectedClass = introspectedClass;
	}

	/**
	 * Return the underlying Class.
	 */
	public final Class getIntrospectedClass() {
		return this.introspectedClass;
	}


	public String getClassName() {
		return this.introspectedClass.getName();
	}

	public boolean isInterface() {
		return this.introspectedClass.isInterface();
	}

	public boolean isAbstract() {
		return Modifier.isAbstract(this.introspectedClass.getModifiers());
	}

	public boolean isConcrete() {
		return !(isInterface() || isAbstract());
	}

	public boolean isFinal() {
		return Modifier.isFinal(this.introspectedClass.getModifiers());
	}

	public boolean isIndependent() {
		return (!hasEnclosingClass() ||
				(this.introspectedClass.getDeclaringClass() != null &&
						Modifier.isStatic(this.introspectedClass.getModifiers())));
	}

	public boolean hasEnclosingClass() {
		return (this.introspectedClass.getEnclosingClass() != null);
	}

	public String getEnclosingClassName() {
		Class enclosingClass = this.introspectedClass.getEnclosingClass();
		return (enclosingClass != null ? enclosingClass.getName() : null);
	}

	public boolean hasSuperClass() {
		return (this.introspectedClass.getSuperclass() != null);
	}

	public String getSuperClassName() {
		Class superClass = this.introspectedClass.getSuperclass();
		return (superClass != null ? superClass.getName() : null);
	}

	public String[] getInterfaceNames() {
		Class[] ifcs = this.introspectedClass.getInterfaces();
		String[] ifcNames = new String[ifcs.length];
		for (int i = 0; i < ifcs.length; i++) {
			ifcNames[i] = ifcs[i].getName();
		}
		return ifcNames;
	}

	public String[] getMemberClassNames() {
		LinkedHashSet<String> memberClassNames = new LinkedHashSet<String>();
		for (Class<?> nestedClass : this.introspectedClass.getDeclaredClasses()) {
			memberClassNames.add(nestedClass.getName());
		}
		return memberClassNames.toArray(new String[memberClassNames.size()]);
	}

}
