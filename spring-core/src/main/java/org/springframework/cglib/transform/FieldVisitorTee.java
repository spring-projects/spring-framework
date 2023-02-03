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
import org.springframework.asm.Attribute;
import org.springframework.asm.FieldVisitor;
import org.springframework.asm.TypePath;
import org.springframework.cglib.core.Constants;

public class FieldVisitorTee extends FieldVisitor {
    private FieldVisitor fv1, fv2;

    public FieldVisitorTee(FieldVisitor fv1, FieldVisitor fv2) {
	super(Constants.ASM_API);
	this.fv1 = fv1;
        this.fv2 = fv2;
    }

    @Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return AnnotationVisitorTee.getInstance(fv1.visitAnnotation(desc, visible),
                                                fv2.visitAnnotation(desc, visible));
    }

    @Override
	public void visitAttribute(Attribute attr) {
        fv1.visitAttribute(attr);
        fv2.visitAttribute(attr);
    }

    @Override
	public void visitEnd() {
        fv1.visitEnd();
        fv2.visitEnd();
    }

    @Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        return AnnotationVisitorTee.getInstance(fv1.visitTypeAnnotation(typeRef, typePath, desc, visible),
                                                fv2.visitTypeAnnotation(typeRef, typePath, desc, visible));
    }
}

