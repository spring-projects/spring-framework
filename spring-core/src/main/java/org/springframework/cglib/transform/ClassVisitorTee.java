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
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.FieldVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.TypePath;
import org.springframework.cglib.core.Constants;

public class ClassVisitorTee extends ClassVisitor {
    private ClassVisitor cv1, cv2;
    
    public ClassVisitorTee(ClassVisitor cv1, ClassVisitor cv2) {
	super(Constants.ASM_API);
	this.cv1 = cv1;
        this.cv2 = cv2;
    }

    public void visit(int version,
                      int access,
                      String name,
                      String signature,
                      String superName,
                      String[] interfaces) {
        cv1.visit(version, access, name, signature, superName, interfaces);
        cv2.visit(version, access, name, signature, superName, interfaces);
    }

    public void visitEnd() {
        cv1.visitEnd();
        cv2.visitEnd();
        cv1 = cv2 = null;
    }

    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        cv1.visitInnerClass(name, outerName, innerName, access);
        cv2.visitInnerClass(name, outerName, innerName, access);
    }

    public FieldVisitor visitField(int access,
                                   String name,
                                   String desc,
                                   String signature,
                                   Object value) {
        FieldVisitor fv1 = cv1.visitField(access, name, desc, signature, value);
        FieldVisitor fv2 = cv2.visitField(access, name, desc, signature, value);
        if (fv1 == null)
            return fv2;
        if (fv2 == null)
            return fv1;
        return new FieldVisitorTee(fv1, fv2);
    }


    public MethodVisitor visitMethod(int access,
                                     String name,
                                     String desc,
                                     String signature,
                                     String[] exceptions) {
        MethodVisitor mv1 = cv1.visitMethod(access, name, desc, signature, exceptions);
        MethodVisitor mv2 = cv2.visitMethod(access, name, desc, signature, exceptions);
        if (mv1 == null)
            return mv2;
        if (mv2 == null)
            return mv1;
        return new MethodVisitorTee(mv1, mv2);
    }

    public void visitSource(String source, String debug) {
        cv1.visitSource(source, debug);
        cv2.visitSource(source, debug);
    }

    public void visitOuterClass(String owner, String name, String desc) {
        cv1.visitOuterClass(owner, name, desc);
        cv2.visitOuterClass(owner, name, desc);
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return AnnotationVisitorTee.getInstance(cv1.visitAnnotation(desc, visible),
                                                cv2.visitAnnotation(desc, visible));
    }
    
    public void visitAttribute(Attribute attrs) {
        cv1.visitAttribute(attrs);
        cv2.visitAttribute(attrs);
    }

    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        return AnnotationVisitorTee.getInstance(cv1.visitTypeAnnotation(typeRef, typePath, desc, visible),
                                                cv2.visitTypeAnnotation(typeRef, typePath, desc, visible));
    }
}
