/*
 * Copyright 2002-2008 the original author or authors.
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

/**
 * {@link ClassMetadata} implementation that uses standard reflection
 * to introspect a given <code>Class</code>.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
public class StandardClassMetadata implements ClassMetadata {

	private final Class introspectedClass;


	public StandardClassMetadata(Class introspectedClass) {
		this.introspectedClass = introspectedClass;
	}

	public final Class getIntrospectedClass() {
		return this.introspectedClass;
	}


	public String getClassName() {
		return getIntrospectedClass().getName();
	}

	public boolean isInterface() {
		return getIntrospectedClass().isInterface();
	}

	public boolean isAbstract() {
		return Modifier.isAbstract(getIntrospectedClass().getModifiers());
	}

	public boolean isConcrete() {
		return !(isInterface() || isAbstract());
	}

	public boolean isIndependent() {
		return (!hasEnclosingClass() ||
				(getIntrospectedClass().getDeclaringClass() != null &&
						Modifier.isStatic(getIntrospectedClass().getModifiers())));
	}

	public boolean hasEnclosingClass() {
		return (getIntrospectedClass().getEnclosingClass() != null);
	}

	public String getEnclosingClassName() {
		Class enclosingClass = getIntrospectedClass().getEnclosingClass();
		return (enclosingClass != null ? enclosingClass.getName() : null);
	}

	public boolean hasSuperClass() {
		return (getIntrospectedClass().getSuperclass() != null);
	}

	public String getSuperClassName() {
		Class superClass = getIntrospectedClass().getSuperclass();
		return (superClass != null ? superClass.getName() : null);
	}

	public String[] getInterfaceNames() {
		Class[] ifcs = getIntrospectedClass().getInterfaces();
		String[] ifcNames = new String[ifcs.length];
		for (int i = 0; i < ifcs.length; i++) {
			ifcNames[i] = ifcs[i].getName();
		}
		return ifcNames;
	}

}
