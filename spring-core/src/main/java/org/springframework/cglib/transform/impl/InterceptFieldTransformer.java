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
package org.springframework.cglib.transform.impl;

import org.springframework.asm.Label;
import org.springframework.asm.Type;
import org.springframework.cglib.core.CodeEmitter;
import org.springframework.cglib.core.Constants;
import org.springframework.cglib.core.Local;
import org.springframework.cglib.core.Signature;
import org.springframework.cglib.core.TypeUtils;
import org.springframework.cglib.transform.ClassEmitterTransformer;

/**
 * @author Juozas Baliuka, Chris Nokleberg
 */
public class InterceptFieldTransformer extends ClassEmitterTransformer {
    private static final String CALLBACK_FIELD = "$CGLIB_READ_WRITE_CALLBACK";
    private static final Type CALLBACK =
      TypeUtils.parseType("org.springframework.cglib.transform.impl.InterceptFieldCallback");
    private static final Type ENABLED =
      TypeUtils.parseType("org.springframework.cglib.transform.impl.InterceptFieldEnabled");
    private static final Signature ENABLED_SET =
      new Signature("setInterceptFieldCallback", Type.VOID_TYPE, new Type[]{ CALLBACK });
    private static final Signature ENABLED_GET =
      new Signature("getInterceptFieldCallback", CALLBACK, new Type[0]);

    private InterceptFieldFilter filter;

    public InterceptFieldTransformer(InterceptFieldFilter filter) {
        this.filter = filter;
    }

    @Override
	public void begin_class(int version, int access, String className, Type superType, Type[] interfaces, String sourceFile) {
        if (!TypeUtils.isInterface(access)) {
            super.begin_class(version, access, className, superType, TypeUtils.add(interfaces, ENABLED), sourceFile);

            super.declare_field(Constants.ACC_PRIVATE | Constants.ACC_TRANSIENT,
                                CALLBACK_FIELD,
                                CALLBACK,
                                null);

            CodeEmitter e;
            e = super.begin_method(Constants.ACC_PUBLIC, ENABLED_GET, null);
            e.load_this();
            e.getfield(CALLBACK_FIELD);
            e.return_value();
            e.end_method();

            e = super.begin_method(Constants.ACC_PUBLIC, ENABLED_SET, null);
            e.load_this();
            e.load_arg(0);
            e.putfield(CALLBACK_FIELD);
            e.return_value();
            e.end_method();
        } else {
            super.begin_class(version, access, className, superType, interfaces, sourceFile);
        }
    }

    @Override
	public void declare_field(int access, String name, Type type, Object value) {
        super.declare_field(access, name, type, value);
        if (!TypeUtils.isStatic(access)) {
            if (filter.acceptRead(getClassType(), name)) {
                addReadMethod(name, type);
            }
            if (filter.acceptWrite(getClassType(), name)) {
                addWriteMethod(name, type);
            }
        }
    }

    private void addReadMethod(String name, Type type) {
        CodeEmitter e = super.begin_method(Constants.ACC_PUBLIC,
                                           readMethodSig(name, type.getDescriptor()),
                                           null);
        e.load_this();
        e.getfield(name);
        e.load_this();
        e.invoke_interface(ENABLED,ENABLED_GET);
        Label intercept = e.make_label();
        e.ifnonnull(intercept);
        e.return_value();

        e.mark(intercept);
        Local result = e.make_local(type);
        e.store_local(result);
        e.load_this();
        e.invoke_interface(ENABLED,ENABLED_GET);
        e.load_this();
        e.push(name);
        e.load_local(result);
        e.invoke_interface(CALLBACK, readCallbackSig(type));
        if (!TypeUtils.isPrimitive(type)) {
            e.checkcast(type);
        }
        e.return_value();
        e.end_method();
    }

    private void addWriteMethod(String name, Type type) {
        CodeEmitter e = super.begin_method(Constants.ACC_PUBLIC,
                                           writeMethodSig(name, type.getDescriptor()),
                                           null);
        e.load_this();
        e.dup();
        e.invoke_interface(ENABLED,ENABLED_GET);
        Label skip = e.make_label();
        e.ifnull(skip);

        e.load_this();
        e.invoke_interface(ENABLED,ENABLED_GET);
        e.load_this();
        e.push(name);
        e.load_this();
        e.getfield(name);
        e.load_arg(0);
        e.invoke_interface(CALLBACK, writeCallbackSig(type));
        if (!TypeUtils.isPrimitive(type)) {
            e.checkcast(type);
        }
        Label go = e.make_label();
        e.goTo(go);
        e.mark(skip);
        e.load_arg(0);
        e.mark(go);
        e.putfield(name);
        e.return_value();
        e.end_method();
    }

    @Override
	public CodeEmitter begin_method(int access, Signature sig, Type[] exceptions) {
        return new CodeEmitter(super.begin_method(access, sig, exceptions)) {
            @Override
			public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                Type towner = TypeUtils.fromInternalName(owner);
                switch (opcode) {
                case Constants.GETFIELD:
                    if (filter.acceptRead(towner, name)) {
                        helper(towner, readMethodSig(name, desc));
                        return;
                    }
                    break;
                case Constants.PUTFIELD:
                    if (filter.acceptWrite(towner, name)) {
                        helper(towner, writeMethodSig(name, desc));
                        return;
                    }
                    break;
                }
                super.visitFieldInsn(opcode, owner, name, desc);
            }

            private void helper(Type owner, Signature sig) {
                invoke_virtual(owner, sig);
            }
        };
    }

    private static Signature readMethodSig(String name, String desc) {
        return new Signature("$cglib_read_" + name, "()" + desc);
    }

    private static Signature writeMethodSig(String name, String desc) {
        return new Signature("$cglib_write_" + name, "(" + desc + ")V");
    }

    private static Signature readCallbackSig(Type type) {
        Type remap = remap(type);
        return new Signature("read" + callbackName(remap),
                             remap,
                             new Type[]{ Constants.TYPE_OBJECT,
                                         Constants.TYPE_STRING,
                                         remap });
    }

    private static Signature writeCallbackSig(Type type) {
        Type remap = remap(type);
        return new Signature("write" + callbackName(remap),
                             remap,
                             new Type[]{ Constants.TYPE_OBJECT,
                                         Constants.TYPE_STRING,
                                         remap,
                                         remap });
    }

    private static Type remap(Type type) {
        switch (type.getSort()) {
        case Type.OBJECT:
        case Type.ARRAY:
            return Constants.TYPE_OBJECT;
        default:
            return type;
        }
    }

    private static String callbackName(Type type) {
        return (type == Constants.TYPE_OBJECT) ?
            "Object" :
            TypeUtils.upperFirst(TypeUtils.getClassName(type));
    }
}
