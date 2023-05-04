/*
 * Copyright 2003,2004 The Apache Software Foundation
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

package org.springframework.cglib.core;

import java.util.Arrays;

import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Type;

/**
 * @author Juozas Baliuka, Chris Nokleberg
 */
@SuppressWarnings("fallthrough")
public class CodeEmitter extends LocalVariablesSorter {
    private static final Signature BOOLEAN_VALUE =
      TypeUtils.parseSignature("boolean booleanValue()");
    private static final Signature CHAR_VALUE =
      TypeUtils.parseSignature("char charValue()");
    private static final Signature LONG_VALUE =
      TypeUtils.parseSignature("long longValue()");
    private static final Signature DOUBLE_VALUE =
      TypeUtils.parseSignature("double doubleValue()");
    private static final Signature FLOAT_VALUE =
      TypeUtils.parseSignature("float floatValue()");
    private static final Signature INT_VALUE =
      TypeUtils.parseSignature("int intValue()");
    private static final Signature CSTRUCT_NULL =
      TypeUtils.parseConstructor("");
    private static final Signature CSTRUCT_STRING =
      TypeUtils.parseConstructor("String");

    public static final int ADD = Constants.IADD;
    public static final int MUL = Constants.IMUL;
    public static final int XOR = Constants.IXOR;
    public static final int USHR = Constants.IUSHR;
    public static final int SUB = Constants.ISUB;
    public static final int DIV = Constants.IDIV;
    public static final int NEG = Constants.INEG;
    public static final int REM = Constants.IREM;
    public static final int AND = Constants.IAND;
    public static final int OR = Constants.IOR;

    public static final int GT = Constants.IFGT;
    public static final int LT = Constants.IFLT;
    public static final int GE = Constants.IFGE;
    public static final int LE = Constants.IFLE;
    public static final int NE = Constants.IFNE;
    public static final int EQ = Constants.IFEQ;

    private ClassEmitter ce;
    private State state;

    private static class State
    extends MethodInfo
    {
        ClassInfo classInfo;
        int access;
        Signature sig;
        Type[] argumentTypes;
        int localOffset;
        Type[] exceptionTypes;

        State(ClassInfo classInfo, int access, Signature sig, Type[] exceptionTypes) {
            this.classInfo = classInfo;
            this.access = access;
            this.sig = sig;
            this.exceptionTypes = exceptionTypes;
            localOffset = TypeUtils.isStatic(access) ? 0 : 1;
            argumentTypes = sig.getArgumentTypes();
        }

        @Override
        public ClassInfo getClassInfo() {
            return classInfo;
        }

        @Override
        public int getModifiers() {
            return access;
        }

        @Override
        public Signature getSignature() {
            return sig;
        }

        @Override
        public Type[] getExceptionTypes() {
            return exceptionTypes;
        }

    }

    CodeEmitter(ClassEmitter ce, MethodVisitor mv, int access, Signature sig, Type[] exceptionTypes) {
        super(access, sig.getDescriptor(), mv);
        this.ce = ce;
        state = new State(ce.getClassInfo(), access, sig, exceptionTypes);
    }

    public CodeEmitter(CodeEmitter wrap) {
        super(wrap);
        this.ce = wrap.ce;
        this.state = wrap.state;
    }

    public boolean isStaticHook() {
        return false;
    }

    public Signature getSignature() {
        return state.sig;
    }

    public Type getReturnType() {
        return state.sig.getReturnType();
    }

    public MethodInfo getMethodInfo() {
        return state;
    }

    public ClassEmitter getClassEmitter() {
        return ce;
    }

    public void end_method() {
        visitMaxs(0, 0);
    }

    public Block begin_block() {
        return new Block(this);
    }

    public void catch_exception(Block block, Type exception) {
        if (block.getEnd() == null) {
            throw new IllegalStateException("end of block is unset");
        }
        mv.visitTryCatchBlock(block.getStart(),
                              block.getEnd(),
                              mark(),
                              exception.getInternalName());
    }

    public void goTo(Label label) { mv.visitJumpInsn(Constants.GOTO, label); }
    public void ifnull(Label label) { mv.visitJumpInsn(Constants.IFNULL, label); }
    public void ifnonnull(Label label) { mv.visitJumpInsn(Constants.IFNONNULL, label); }

    public void if_jump(int mode, Label label) {
        mv.visitJumpInsn(mode, label);
    }

    public void if_icmp(int mode, Label label) {
        if_cmp(Type.INT_TYPE, mode, label);
    }

    public void if_cmp(Type type, int mode, Label label) {
        int intOp = -1;
        int jumpmode = mode;
        switch (mode) {
        case GE: jumpmode = LT; break;
        case LE: jumpmode = GT; break;
        }
        switch (type.getSort()) {
        case Type.LONG:
            mv.visitInsn(Constants.LCMP);
            break;
        case Type.DOUBLE:
            mv.visitInsn(Constants.DCMPG);
            break;
        case Type.FLOAT:
            mv.visitInsn(Constants.FCMPG);
            break;
        case Type.ARRAY:
        case Type.OBJECT:
            switch (mode) {
            case EQ:
                mv.visitJumpInsn(Constants.IF_ACMPEQ, label);
                return;
            case NE:
                mv.visitJumpInsn(Constants.IF_ACMPNE, label);
                return;
            }
            throw new IllegalArgumentException("Bad comparison for type " + type);
        default:
            switch (mode) {
            case EQ: intOp = Constants.IF_ICMPEQ; break;
            case NE: intOp = Constants.IF_ICMPNE; break;
            case GE: swap(); /* fall through */
            case LT: intOp = Constants.IF_ICMPLT; break;
            case LE: swap(); /* fall through */
            case GT: intOp = Constants.IF_ICMPGT; break;
            }
            mv.visitJumpInsn(intOp, label);
            return;
        }
        if_jump(jumpmode, label);
    }

    public void pop() { mv.visitInsn(Constants.POP); }
    public void pop2() { mv.visitInsn(Constants.POP2); }
    public void dup() { mv.visitInsn(Constants.DUP); }
    public void dup2() { mv.visitInsn(Constants.DUP2); }
    public void dup_x1() { mv.visitInsn(Constants.DUP_X1); }
    public void dup_x2() { mv.visitInsn(Constants.DUP_X2); }
    public void dup2_x1() { mv.visitInsn(Constants.DUP2_X1); }
    public void dup2_x2() { mv.visitInsn(Constants.DUP2_X2); }
    public void swap() { mv.visitInsn(Constants.SWAP); }
    public void aconst_null() { mv.visitInsn(Constants.ACONST_NULL); }

    public void swap(Type prev, Type type) {
        if (type.getSize() == 1) {
            if (prev.getSize() == 1) {
                swap(); // same as dup_x1(), pop();
            } else {
                dup_x2();
                pop();
            }
        } else {
            if (prev.getSize() == 1) {
                dup2_x1();
                pop2();
            } else {
                dup2_x2();
                pop2();
            }
        }
    }

    public void monitorenter() { mv.visitInsn(Constants.MONITORENTER); }
    public void monitorexit() { mv.visitInsn(Constants.MONITOREXIT); }

    public void math(int op, Type type) { mv.visitInsn(type.getOpcode(op)); }

    public void array_load(Type type) { mv.visitInsn(type.getOpcode(Constants.IALOAD)); }
    public void array_store(Type type) { mv.visitInsn(type.getOpcode(Constants.IASTORE)); }

    /**
     * Casts from one primitive numeric type to another
     */
    public void cast_numeric(Type from, Type to) {
        if (from != to) {
            if (from == Type.DOUBLE_TYPE) {
                if (to == Type.FLOAT_TYPE) {
                    mv.visitInsn(Constants.D2F);
                } else if (to == Type.LONG_TYPE) {
                    mv.visitInsn(Constants.D2L);
                } else {
                    mv.visitInsn(Constants.D2I);
                    cast_numeric(Type.INT_TYPE, to);
                }
            } else if (from == Type.FLOAT_TYPE) {
                if (to == Type.DOUBLE_TYPE) {
                    mv.visitInsn(Constants.F2D);
                } else if (to == Type.LONG_TYPE) {
                    mv.visitInsn(Constants.F2L);
                } else {
                    mv.visitInsn(Constants.F2I);
                    cast_numeric(Type.INT_TYPE, to);
                }
            } else if (from == Type.LONG_TYPE) {
                if (to == Type.DOUBLE_TYPE) {
                    mv.visitInsn(Constants.L2D);
                } else if (to == Type.FLOAT_TYPE) {
                    mv.visitInsn(Constants.L2F);
                } else {
                    mv.visitInsn(Constants.L2I);
                    cast_numeric(Type.INT_TYPE, to);
                }
            } else {
                if (to == Type.BYTE_TYPE) {
                    mv.visitInsn(Constants.I2B);
                } else if (to == Type.CHAR_TYPE) {
                    mv.visitInsn(Constants.I2C);
                } else if (to == Type.DOUBLE_TYPE) {
                    mv.visitInsn(Constants.I2D);
                } else if (to == Type.FLOAT_TYPE) {
                    mv.visitInsn(Constants.I2F);
                } else if (to == Type.LONG_TYPE) {
                    mv.visitInsn(Constants.I2L);
                } else if (to == Type.SHORT_TYPE) {
                    mv.visitInsn(Constants.I2S);
                }
            }
        }
    }

    public void push(int i) {
        if (i < -1) {
            mv.visitLdcInsn(i);
        } else if (i <= 5) {
            mv.visitInsn(TypeUtils.ICONST(i));
        } else if (i <= Byte.MAX_VALUE) {
            mv.visitIntInsn(Constants.BIPUSH, i);
        } else if (i <= Short.MAX_VALUE) {
            mv.visitIntInsn(Constants.SIPUSH, i);
        } else {
            mv.visitLdcInsn(i);
        }
    }

    public void push(long value) {
        if (value == 0L || value == 1L) {
            mv.visitInsn(TypeUtils.LCONST(value));
        } else {
            mv.visitLdcInsn(value);
        }
    }

    public void push(float value) {
        if (value == 0f || value == 1f || value == 2f) {
            mv.visitInsn(TypeUtils.FCONST(value));
        } else {
            mv.visitLdcInsn(value);
        }
    }
    public void push(double value) {
        if (value == 0d || value == 1d) {
            mv.visitInsn(TypeUtils.DCONST(value));
        } else {
            mv.visitLdcInsn(value);
        }
    }

    public void push(String value) {
        mv.visitLdcInsn(value);
    }

    public void newarray() {
        newarray(Constants.TYPE_OBJECT);
    }

    public void newarray(Type type) {
        if (TypeUtils.isPrimitive(type)) {
            mv.visitIntInsn(Constants.NEWARRAY, TypeUtils.NEWARRAY(type));
        } else {
            emit_type(Constants.ANEWARRAY, type);
        }
    }

    public void arraylength() {
        mv.visitInsn(Constants.ARRAYLENGTH);
    }

    public void load_this() {
        if (TypeUtils.isStatic(state.access)) {
            throw new IllegalStateException("no 'this' pointer within static method");
        }
        mv.visitVarInsn(Constants.ALOAD, 0);
    }

    /**
     * Pushes all of the arguments of the current method onto the stack.
     */
    public void load_args() {
        load_args(0, state.argumentTypes.length);
    }

    /**
     * Pushes the specified argument of the current method onto the stack.
     * @param index the zero-based index into the argument list
     */
    public void load_arg(int index) {
        load_local(state.argumentTypes[index],
                   state.localOffset + skipArgs(index));
    }

    // zero-based (see load_this)
    public void load_args(int fromArg, int count) {
        int pos = state.localOffset + skipArgs(fromArg);
        for (int i = 0; i < count; i++) {
            Type t = state.argumentTypes[fromArg + i];
            load_local(t, pos);
            pos += t.getSize();
        }
    }

    private int skipArgs(int numArgs) {
        int amount = 0;
        for (int i = 0; i < numArgs; i++) {
            amount += state.argumentTypes[i].getSize();
        }
        return amount;
    }

    private void load_local(Type t, int pos) {
        // TODO: make t == null ok?
        mv.visitVarInsn(t.getOpcode(Constants.ILOAD), pos);
    }

    private void store_local(Type t, int pos) {
        // TODO: make t == null ok?
        mv.visitVarInsn(t.getOpcode(Constants.ISTORE), pos);
    }

    public void iinc(Local local, int amount) {
        mv.visitIincInsn(local.getIndex(), amount);
    }

    public void store_local(Local local) {
        store_local(local.getType(), local.getIndex());
    }

    public void load_local(Local local) {
        load_local(local.getType(), local.getIndex());
    }

    public void return_value() {
        mv.visitInsn(state.sig.getReturnType().getOpcode(Constants.IRETURN));
    }

    public void getfield(String name) {
        ClassEmitter.FieldInfo info = ce.getFieldInfo(name);
        int opcode = TypeUtils.isStatic(info.access) ? Constants.GETSTATIC : Constants.GETFIELD;
        emit_field(opcode, ce.getClassType(), name, info.type);
    }

    public void putfield(String name) {
        ClassEmitter.FieldInfo info = ce.getFieldInfo(name);
        int opcode = TypeUtils.isStatic(info.access) ? Constants.PUTSTATIC : Constants.PUTFIELD;
        emit_field(opcode, ce.getClassType(), name, info.type);
    }

    public void super_getfield(String name, Type type) {
        emit_field(Constants.GETFIELD, ce.getSuperType(), name, type);
    }

    public void super_putfield(String name, Type type) {
        emit_field(Constants.PUTFIELD, ce.getSuperType(), name, type);
    }

    public void super_getstatic(String name, Type type) {
        emit_field(Constants.GETSTATIC, ce.getSuperType(), name, type);
    }

    public void super_putstatic(String name, Type type) {
        emit_field(Constants.PUTSTATIC, ce.getSuperType(), name, type);
    }

    public void getfield(Type owner, String name, Type type) {
        emit_field(Constants.GETFIELD, owner, name, type);
    }

    public void putfield(Type owner, String name, Type type) {
        emit_field(Constants.PUTFIELD, owner, name, type);
    }

    public void getstatic(Type owner, String name, Type type) {
        emit_field(Constants.GETSTATIC, owner, name, type);
    }

    public void putstatic(Type owner, String name, Type type) {
        emit_field(Constants.PUTSTATIC, owner, name, type);
    }

    // package-protected for EmitUtils, try to fix
    void emit_field(int opcode, Type ctype, String name, Type ftype) {
        mv.visitFieldInsn(opcode,
                          ctype.getInternalName(),
                          name,
                          ftype.getDescriptor());
    }

    public void super_invoke() {
        super_invoke(state.sig);
    }

    public void super_invoke(Signature sig) {
        emit_invoke(Constants.INVOKESPECIAL, ce.getSuperType(), sig, false);
    }

    public void invoke_constructor(Type type) {
        invoke_constructor(type, CSTRUCT_NULL);
    }

    public void super_invoke_constructor() {
        invoke_constructor(ce.getSuperType());
    }

    public void invoke_constructor_this() {
        invoke_constructor(ce.getClassType());
    }

    private void emit_invoke(int opcode, Type type, Signature sig, boolean isInterface) {
        if (sig.getName().equals(Constants.CONSTRUCTOR_NAME) &&
            ((opcode == Constants.INVOKEVIRTUAL) ||
             (opcode == Constants.INVOKESTATIC))) {
            // TODO: error
        }
        mv.visitMethodInsn(opcode,
                           type.getInternalName(),
                           sig.getName(),
                           sig.getDescriptor(),
                           isInterface);
    }

    public void invoke_interface(Type owner, Signature sig) {
        emit_invoke(Constants.INVOKEINTERFACE, owner, sig, true);
    }

    public void invoke_virtual(Type owner, Signature sig) {
        emit_invoke(Constants.INVOKEVIRTUAL, owner, sig, false);
    }

    @Deprecated
    public void invoke_static(Type owner, Signature sig) {
        invoke_static(owner, sig, false);
    }

    public void invoke_static(Type owner, Signature sig, boolean isInterface) {
        emit_invoke(Constants.INVOKESTATIC, owner, sig, isInterface);
    }

    public void invoke_virtual_this(Signature sig) {
        invoke_virtual(ce.getClassType(), sig);
    }

    public void invoke_static_this(Signature sig) {
        invoke_static(ce.getClassType(), sig);
    }

    public void invoke_constructor(Type type, Signature sig) {
        emit_invoke(Constants.INVOKESPECIAL, type, sig, false);
    }

    public void invoke_constructor_this(Signature sig) {
        invoke_constructor(ce.getClassType(), sig);
    }

    public void super_invoke_constructor(Signature sig) {
        invoke_constructor(ce.getSuperType(), sig);
    }

    public void new_instance_this() {
        new_instance(ce.getClassType());
    }

    public void new_instance(Type type) {
        emit_type(Constants.NEW, type);
    }

    private void emit_type(int opcode, Type type) {
        String desc;
        if (TypeUtils.isArray(type)) {
            desc = type.getDescriptor();
        } else {
            desc = type.getInternalName();
        }
        mv.visitTypeInsn(opcode, desc);
    }

    public void aaload(int index) {
        push(index);
        aaload();
    }

    public void aaload() { mv.visitInsn(Constants.AALOAD); }
    public void aastore() { mv.visitInsn(Constants.AASTORE); }
    public void athrow() { mv.visitInsn(Constants.ATHROW); }

    public Label make_label() {
        return new Label();
    }

    public Local make_local() {
        return make_local(Constants.TYPE_OBJECT);
    }

    public Local make_local(Type type) {
        return new Local(newLocal(type.getSize()), type);
    }

    public void checkcast_this() {
        checkcast(ce.getClassType());
    }

    public void checkcast(Type type) {
        if (!type.equals(Constants.TYPE_OBJECT)) {
            emit_type(Constants.CHECKCAST, type);
        }
    }

    public void instance_of(Type type) {
        emit_type(Constants.INSTANCEOF, type);
    }

    public void instance_of_this() {
        instance_of(ce.getClassType());
    }

    public void process_switch(int[] keys, ProcessSwitchCallback callback) {
        float density;
        if (keys.length == 0) {
            density = 0;
        } else {
            density = (float)keys.length / (keys[keys.length - 1] - keys[0] + 1);
        }
        process_switch(keys, callback, density >= 0.5f);
    }

    public void process_switch(int[] keys, ProcessSwitchCallback callback, boolean useTable) {
        if (!isSorted(keys)) {
			throw new IllegalArgumentException("keys to switch must be sorted ascending");
		}
        Label def = make_label();
        Label end = make_label();

        try {
            if (keys.length > 0) {
                int len = keys.length;
                int min = keys[0];
                int max = keys[len - 1];
                int range = max - min + 1;

                if (useTable) {
                    Label[] labels = new Label[range];
                    Arrays.fill(labels, def);
                    for (int i = 0; i < len; i++) {
                        labels[keys[i] - min] = make_label();
                    }
                    mv.visitTableSwitchInsn(min, max, def, labels);
                    for (int i = 0; i < range; i++) {
                        Label label = labels[i];
                        if (label != def) {
                            mark(label);
                            callback.processCase(i + min, end);
                        }
                    }
                } else {
                    Label[] labels = new Label[len];
                    for (int i = 0; i < len; i++) {
                        labels[i] = make_label();
                    }
                    mv.visitLookupSwitchInsn(def, keys, labels);
                    for (int i = 0; i < len; i++) {
                        mark(labels[i]);
                        callback.processCase(keys[i], end);
                    }
                }
            }

            mark(def);
            callback.processDefault();
            mark(end);

        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Exception e) {
            throw new CodeGenerationException(e);
        }
    }

    private static boolean isSorted(int[] keys) {
        for (int i = 1; i < keys.length; i++) {
            if (keys[i] < keys[i - 1]) {
				return false;
			}
        }
        return true;
    }

    public void mark(Label label) {
        mv.visitLabel(label);
    }

    Label mark() {
        Label label = make_label();
        mv.visitLabel(label);
        return label;
    }

    public void push(boolean value) {
        push(value ? 1 : 0);
    }

    /**
     * Toggles the integer on the top of the stack from 1 to 0 or vice versa
     */
    public void not() {
        push(1);
        math(XOR, Type.INT_TYPE);
    }

    public void throw_exception(Type type, String msg) {
        new_instance(type);
        dup();
        push(msg);
        invoke_constructor(type, CSTRUCT_STRING);
        athrow();
    }

    /**
     * If the argument is a primitive class, replaces the primitive value
     * on the top of the stack with the wrapped (Object) equivalent. For
     * example, char -> Character.
     * If the class is Void, a null is pushed onto the stack instead.
     * @param type the class indicating the current type of the top stack value
     */
    public void box(Type type) {
        if (TypeUtils.isPrimitive(type)) {
            if (type == Type.VOID_TYPE) {
                aconst_null();
            } else {
                Type boxed = TypeUtils.getBoxedType(type);
                new_instance(boxed);
                if (type.getSize() == 2) {
                    // Pp -> Ppo -> oPpo -> ooPpo -> ooPp -> o
                    dup_x2();
                    dup_x2();
                    pop();
                } else {
                    // p -> po -> opo -> oop -> o
                    dup_x1();
                    swap();
                }
                invoke_constructor(boxed, new Signature(Constants.CONSTRUCTOR_NAME, Type.VOID_TYPE, new Type[]{ type }));
            }
        }
    }

    /**
     * If the argument is a primitive class, replaces the object
     * on the top of the stack with the unwrapped (primitive)
     * equivalent. For example, Character -> char.
     * @param type the class indicating the desired type of the top stack value
     */
    public void unbox(Type type) {
        Type t = Constants.TYPE_NUMBER;
        Signature sig = null;
        switch (type.getSort()) {
        case Type.VOID:
            return;
        case Type.CHAR:
            t = Constants.TYPE_CHARACTER;
            sig = CHAR_VALUE;
            break;
        case Type.BOOLEAN:
            t = Constants.TYPE_BOOLEAN;
            sig = BOOLEAN_VALUE;
            break;
        case Type.DOUBLE:
            sig = DOUBLE_VALUE;
            break;
        case Type.FLOAT:
            sig = FLOAT_VALUE;
            break;
        case Type.LONG:
            sig = LONG_VALUE;
            break;
        case Type.INT:
        case Type.SHORT:
        case Type.BYTE:
            sig = INT_VALUE;
        }

        if (sig == null) {
            checkcast(type);
        } else {
            checkcast(t);
            invoke_virtual(t, sig);
        }
    }

    /**
     * Allocates and fills an Object[] array with the arguments to the
     * current method. Primitive values are inserted as their boxed
     * (Object) equivalents.
     */
    public void create_arg_array() {
        /* generates:
           Object[] args = new Object[]{ arg1, new Integer(arg2) };
         */

        push(state.argumentTypes.length);
        newarray();
        for (int i = 0; i < state.argumentTypes.length; i++) {
            dup();
            push(i);
            load_arg(i);
            box(state.argumentTypes[i]);
            aastore();
        }
    }


    /**
     * Pushes a zero onto the stack if the argument is a primitive class, or a null otherwise.
     */
    public void zero_or_null(Type type) {
        if (TypeUtils.isPrimitive(type)) {
            switch (type.getSort()) {
            case Type.DOUBLE:
                push(0d);
                break;
            case Type.LONG:
                push(0L);
                break;
            case Type.FLOAT:
                push(0f);
                break;
            case Type.VOID:
                aconst_null();
            default:
                push(0);
            }
        } else {
            aconst_null();
        }
    }

    /**
     * Unboxes the object on the top of the stack. If the object is null, the
     * unboxed primitive value becomes zero.
     */
    public void unbox_or_zero(Type type) {
        if (TypeUtils.isPrimitive(type)) {
            if (type != Type.VOID_TYPE) {
                Label nonNull = make_label();
                Label end = make_label();
                dup();
                ifnonnull(nonNull);
                pop();
                zero_or_null(type);
                goTo(end);
                mark(nonNull);
                unbox(type);
                mark(end);
            }
        } else {
            checkcast(type);
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        if (!TypeUtils.isAbstract(state.access)) {
            mv.visitMaxs(0, 0);
        }
    }

    public void invoke(MethodInfo method, Type virtualType) {
        ClassInfo classInfo = method.getClassInfo();
        Type type = classInfo.getType();
        Signature sig = method.getSignature();
        if (sig.getName().equals(Constants.CONSTRUCTOR_NAME)) {
            invoke_constructor(type, sig);
        } else if (TypeUtils.isStatic(method.getModifiers())) {
            invoke_static(type, sig, TypeUtils.isInterface(classInfo.getModifiers()));
        } else if (TypeUtils.isInterface(classInfo.getModifiers())) {
            invoke_interface(type, sig);
        } else {
            invoke_virtual(virtualType, sig);
        }
    }

    public void invoke(MethodInfo method) {
        invoke(method, method.getClassInfo().getType());
    }
}
