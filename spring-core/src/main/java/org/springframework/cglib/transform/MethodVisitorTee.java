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

import org.springframework.cglib.core.Constants;
import org.springframework.asm.*;

@SuppressWarnings("deprecation")
public class MethodVisitorTee extends MethodVisitor {
    private final MethodVisitor mv1;
    private final MethodVisitor mv2;
    
    public MethodVisitorTee(MethodVisitor mv1, MethodVisitor mv2) {
	super(Constants.ASM_API);
	this.mv1 = mv1;
        this.mv2 = mv2;
    }

    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        mv1.visitFrame(type, nLocal, local, nStack, stack);
        mv2.visitFrame(type, nLocal, local, nStack, stack);
    }

    public AnnotationVisitor visitAnnotationDefault() {
        return AnnotationVisitorTee.getInstance(mv1.visitAnnotationDefault(),
                                                mv2.visitAnnotationDefault());
    }
    
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return AnnotationVisitorTee.getInstance(mv1.visitAnnotation(desc, visible),
                                                mv2.visitAnnotation(desc, visible));
    }
    
    public AnnotationVisitor visitParameterAnnotation(int parameter,
                                                      String desc,
                                                      boolean visible) {
        return AnnotationVisitorTee.getInstance(mv1.visitParameterAnnotation(parameter, desc, visible),
                                                mv2.visitParameterAnnotation(parameter, desc, visible));
    }

    public void visitAttribute(Attribute attr) {
        mv1.visitAttribute(attr);
        mv2.visitAttribute(attr);
    }
    
    public void visitCode() {
        mv1.visitCode();
        mv2.visitCode();
    }
    
    public void visitInsn(int opcode) {
        mv1.visitInsn(opcode);
        mv2.visitInsn(opcode);
    }
    
    public void visitIntInsn(int opcode, int operand) {
        mv1.visitIntInsn(opcode, operand);
        mv2.visitIntInsn(opcode, operand);
    }
    
    public void visitVarInsn(int opcode, int var) {
        mv1.visitVarInsn(opcode, var);
        mv2.visitVarInsn(opcode, var);
    }
    
    public void visitTypeInsn(int opcode, String desc) {
        mv1.visitTypeInsn(opcode, desc);
        mv2.visitTypeInsn(opcode, desc);
    }
    
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        mv1.visitFieldInsn(opcode, owner, name, desc);
        mv2.visitFieldInsn(opcode, owner, name, desc);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        mv1.visitMethodInsn(opcode, owner, name, desc);
        mv2.visitMethodInsn(opcode, owner, name, desc);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        mv1.visitMethodInsn(opcode, owner, name, desc, itf);
        mv2.visitMethodInsn(opcode, owner, name, desc, itf);
    }
    
    public void visitJumpInsn(int opcode, Label label) {
        mv1.visitJumpInsn(opcode, label);
        mv2.visitJumpInsn(opcode, label);
    }
    
    public void visitLabel(Label label) {
        mv1.visitLabel(label);
        mv2.visitLabel(label);
    }
    
    public void visitLdcInsn(Object cst) {
        mv1.visitLdcInsn(cst);
        mv2.visitLdcInsn(cst);
    }
    
    public void visitIincInsn(int var, int increment) {
        mv1.visitIincInsn(var, increment);
        mv2.visitIincInsn(var, increment);
    }
    
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        mv1.visitTableSwitchInsn(min, max, dflt, labels);
        mv2.visitTableSwitchInsn(min, max, dflt, labels);
    }
    
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        mv1.visitLookupSwitchInsn(dflt, keys, labels);
        mv2.visitLookupSwitchInsn(dflt, keys, labels);
    }
    
    public void visitMultiANewArrayInsn(String desc, int dims) {
        mv1.visitMultiANewArrayInsn(desc, dims);
        mv2.visitMultiANewArrayInsn(desc, dims);
    }
    
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        mv1.visitTryCatchBlock(start, end, handler, type);
        mv2.visitTryCatchBlock(start, end, handler, type);
    }
    
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        mv1.visitLocalVariable(name, desc, signature, start, end, index);
        mv2.visitLocalVariable(name, desc, signature, start, end, index);
    }
    
    public void visitLineNumber(int line, Label start) {
        mv1.visitLineNumber(line, start);
        mv2.visitLineNumber(line, start);
    }
    
    public void visitMaxs(int maxStack, int maxLocals) {
        mv1.visitMaxs(maxStack, maxLocals);
        mv2.visitMaxs(maxStack, maxLocals);
    }
    
    public void visitEnd() {
        mv1.visitEnd();
        mv2.visitEnd();
    }

    public void visitParameter(String name, int access) {
        mv1.visitParameter(name, access);
        mv2.visitParameter(name, access);
    }

    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        return AnnotationVisitorTee.getInstance(mv1.visitTypeAnnotation(typeRef, typePath, desc, visible),
                                                mv2.visitTypeAnnotation(typeRef, typePath, desc, visible));
    }

    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        mv1.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        mv2.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    }

    public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        return AnnotationVisitorTee.getInstance(mv1.visitInsnAnnotation(typeRef, typePath, desc, visible),
                                                mv2.visitInsnAnnotation(typeRef, typePath, desc, visible));
    }

    public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        return AnnotationVisitorTee.getInstance(mv1.visitTryCatchAnnotation(typeRef, typePath, desc, visible),
                                                mv2.visitTryCatchAnnotation(typeRef, typePath, desc, visible));
    }

    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String desc, boolean visible) {
        return AnnotationVisitorTee.getInstance(mv1.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, desc, visible),
                                                mv2.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, desc, visible));
    }
}

