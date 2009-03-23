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

package org.springframework.context.annotation.support;

import org.springframework.asm.AnnotationVisitor;


/**
 * An empty {@link AnnotationVisitor} that delegates to another AnnotationVisitor. This
 * class can be used as a super class to quickly implement useful annotation adapter
 * classes, just by overriding the necessary methods. Note that for some reason, ASM
 * doesn't provide this class (it does provide MethodAdapter and ClassAdapter), thus
 * we're following the general pattern and adding our own here.
 * 
 * @author Chris Beams
 */
class AnnotationAdapter implements AnnotationVisitor {

	private AnnotationVisitor delegate;

	/**
	 * Creates a new AnnotationAdapter instance that will delegate all its calls to
	 * <var>delegate</var>.
	 * 
	 * @param delegate In most cases, the delegate will simply be
	 *        {@link AsmUtils#ASM_EMPTY_VISITOR}
	 */
	public AnnotationAdapter(AnnotationVisitor delegate) {
		this.delegate = delegate;
	}

	public void visit(String arg0, Object arg1) {
		delegate.visit(arg0, arg1);
	}

	public AnnotationVisitor visitAnnotation(String arg0, String arg1) {
		return delegate.visitAnnotation(arg0, arg1);
	}

	public AnnotationVisitor visitArray(String arg0) {
		return delegate.visitArray(arg0);
	}

	public void visitEnum(String arg0, String arg1, String arg2) {
		delegate.visitEnum(arg0, arg1, arg2);
	}

	public void visitEnd() {
		delegate.visitEnd();
	}

}
