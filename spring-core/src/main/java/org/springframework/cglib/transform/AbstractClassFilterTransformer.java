/*
 * Copyright 2004 The Apache Software Foundation
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

import org.springframework.asm.*;

abstract public class AbstractClassFilterTransformer extends AbstractClassTransformer {
    private ClassTransformer pass;
    private ClassVisitor target;

    public void setTarget(ClassVisitor target) {
        super.setTarget(target);
        pass.setTarget(target);
    }

    protected AbstractClassFilterTransformer(ClassTransformer pass) {
        this.pass = pass;
    }

    abstract protected boolean accept(int version, int access, String name, String signature, String superName, String[] interfaces);

    public void visit(int version,
                      int access,
                      String name,
                      String signature,
                      String superName,
                      String[] interfaces) {
        target = accept(version, access, name, signature, superName, interfaces) ? pass : cv;
        target.visit(version, access, name, signature, superName, interfaces);
    }
    
    public void visitSource(String source, String debug) {
        target.visitSource(source, debug);
    }
    
    public void visitOuterClass(String owner, String name, String desc) {
        target.visitOuterClass(owner, name, desc);
    }
    
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return target.visitAnnotation(desc, visible);
    }
    
    public void visitAttribute(Attribute attr) {
        target.visitAttribute(attr);
    }
    
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        target.visitInnerClass(name, outerName, innerName, access);
    }

    public FieldVisitor visitField(int access,
                                   String name,
                                   String desc,
                                   String signature,
                                   Object value) {
        return target.visitField(access, name, desc, signature, value);
    }
    
    public MethodVisitor visitMethod(int access,
                                     String name,
                                     String desc,
                                     String signature,
                                     String[] exceptions) {
        return target.visitMethod(access, name, desc, signature, exceptions);
    }

    public void visitEnd() {
        target.visitEnd();
        target = null; // just to be safe
    }
}
