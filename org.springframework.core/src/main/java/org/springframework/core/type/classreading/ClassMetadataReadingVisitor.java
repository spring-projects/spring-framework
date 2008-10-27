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

package org.springframework.core.type.classreading;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.EmptyVisitor;

import org.springframework.core.type.ClassMetadata;
import org.springframework.util.ClassUtils;

/**
 * ASM class visitor which looks only for the class name and implemented types,
 * exposing them through the {@link org.springframework.core.type.ClassMetadata}
 * interface.
 *
 * @author Rod Johnson
 * @author Costin Leau
 * @author Mark Fisher
 * @author Ramnivas Laddad
 * @since 2.5
 */
class ClassMetadataReadingVisitor extends EmptyVisitor implements ClassMetadata {

	private String className;

	private boolean isInterface;

	private boolean isAbstract;

	private String enclosingClassName;

	private boolean independentInnerClass;

	private String superClassName;

	private String[] interfaces;


	public void visit(int version, int access, String name, String signature, String supername, String[] interfaces) {
		this.className = ClassUtils.convertResourcePathToClassName(name);
		this.isInterface = ((access & Opcodes.ACC_INTERFACE) != 0);
		this.isAbstract = ((access & Opcodes.ACC_ABSTRACT) != 0);
		if (supername != null) {
			this.superClassName = ClassUtils.convertResourcePathToClassName(supername);
		}
		this.interfaces = new String[interfaces.length];
		for (int i = 0; i < interfaces.length; i++) {
			this.interfaces[i] = ClassUtils.convertResourcePathToClassName(interfaces[i]);
		}
	}

	public void visitOuterClass(String owner, String name, String desc) {
		this.enclosingClassName = ClassUtils.convertResourcePathToClassName(owner);
	}

	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		if (outerName != null && this.className.equals(ClassUtils.convertResourcePathToClassName(name))) {
			this.enclosingClassName = ClassUtils.convertResourcePathToClassName(outerName);
			this.independentInnerClass = ((access & Opcodes.ACC_STATIC) != 0);
		}
	}


	public String getClassName() {
		return this.className;
	}

	public boolean isInterface() {
		return this.isInterface;
	}

	public boolean isAbstract() {
		return this.isAbstract;
	}

	public boolean isConcrete() {
		return !(this.isInterface || this.isAbstract);
	}

	public boolean isIndependent() {
		return (this.enclosingClassName == null || this.independentInnerClass);
	}

	public boolean hasEnclosingClass() {
		return (this.enclosingClassName != null);
	}

	public String getEnclosingClassName() {
		return this.enclosingClassName;
	}

	public boolean hasSuperClass() {
		return (this.superClassName != null);
	}

	public String getSuperClassName() {
		return this.superClassName;
	}

	public String[] getInterfaceNames() {
		return this.interfaces;
	}

}
