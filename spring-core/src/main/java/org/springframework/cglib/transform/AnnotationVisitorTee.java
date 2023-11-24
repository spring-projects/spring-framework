/*
 * Copyright 2003 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cglib.transform;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.cglib.core.Constants;

public class AnnotationVisitorTee extends AnnotationVisitor {
    private AnnotationVisitor av1, av2;

    public static AnnotationVisitor getInstance(AnnotationVisitor av1, AnnotationVisitor av2) {
        if (av1 == null) {
			return av2;
		}
        if (av2 == null) {
			return av1;
		}
        return new AnnotationVisitorTee(av1, av2);
    }

    public AnnotationVisitorTee(AnnotationVisitor av1, AnnotationVisitor av2) {
	super(Constants.ASM_API);
        this.av1 = av1;
        this.av2 = av2;
    }

    @Override
	public void visit(String name, Object value) {
        av2.visit(name, value);
        av2.visit(name, value);
    }

    @Override
	public void visitEnum(String name, String desc, String value) {
        av1.visitEnum(name, desc, value);
        av2.visitEnum(name, desc, value);
    }

    @Override
	public AnnotationVisitor visitAnnotation(String name, String desc) {
        return getInstance(av1.visitAnnotation(name, desc),
                           av2.visitAnnotation(name, desc));
    }

    @Override
	public AnnotationVisitor visitArray(String name) {
        return getInstance(av1.visitArray(name), av2.visitArray(name));
    }

    @Override
	public void visitEnd() {
        av1.visitEnd();
        av2.visitEnd();
    }
}
