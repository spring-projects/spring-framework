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

package org.springframework.cglib.reflect;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.asm.ClassVisitor;
import org.springframework.asm.Label;
import org.springframework.asm.Type;
import org.springframework.cglib.core.Block;
import org.springframework.cglib.core.ClassEmitter;
import org.springframework.cglib.core.CodeEmitter;
import org.springframework.cglib.core.CollectionUtils;
import org.springframework.cglib.core.Constants;
import org.springframework.cglib.core.DuplicatesPredicate;
import org.springframework.cglib.core.EmitUtils;
import org.springframework.cglib.core.MethodInfo;
import org.springframework.cglib.core.MethodInfoTransformer;
import org.springframework.cglib.core.ObjectSwitchCallback;
import org.springframework.cglib.core.ProcessSwitchCallback;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.cglib.core.Signature;
import org.springframework.cglib.core.TypeUtils;
import org.springframework.cglib.core.VisibilityPredicate;

@SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
class FastClassEmitter extends ClassEmitter {
    private static final Signature CSTRUCT_CLASS =
      TypeUtils.parseConstructor("Class");
    private static final Signature METHOD_GET_INDEX =
      TypeUtils.parseSignature("int getIndex(String, Class[])");
    private static final Signature SIGNATURE_GET_INDEX =
      new Signature("getIndex", Type.INT_TYPE, new Type[]{ Constants.TYPE_SIGNATURE });
    private static final Signature TO_STRING =
      TypeUtils.parseSignature("String toString()");
    private static final Signature CONSTRUCTOR_GET_INDEX =
      TypeUtils.parseSignature("int getIndex(Class[])");
    private static final Signature INVOKE =
      TypeUtils.parseSignature("Object invoke(int, Object, Object[])");
    private static final Signature NEW_INSTANCE =
      TypeUtils.parseSignature("Object newInstance(int, Object[])");
    private static final Signature GET_MAX_INDEX =
      TypeUtils.parseSignature("int getMaxIndex()");
    private static final Signature GET_SIGNATURE_WITHOUT_RETURN_TYPE =
      TypeUtils.parseSignature("String getSignatureWithoutReturnType(String, Class[])");
    private static final Type FAST_CLASS =
      TypeUtils.parseType("org.springframework.cglib.reflect.FastClass");
    private static final Type ILLEGAL_ARGUMENT_EXCEPTION =
      TypeUtils.parseType("IllegalArgumentException");
    private static final Type INVOCATION_TARGET_EXCEPTION =
      TypeUtils.parseType("java.lang.reflect.InvocationTargetException");
    private static final Type[] INVOCATION_TARGET_EXCEPTION_ARRAY = { INVOCATION_TARGET_EXCEPTION };

    public FastClassEmitter(ClassVisitor v, String className, Class type) {
        super(v);

        Type base = Type.getType(type);
        begin_class(Constants.V1_8, Constants.ACC_PUBLIC, className, FAST_CLASS, null, Constants.SOURCE_FILE);

        // constructor
        CodeEmitter e = begin_method(Constants.ACC_PUBLIC, CSTRUCT_CLASS, null);
        e.load_this();
        e.load_args();
        e.super_invoke_constructor(CSTRUCT_CLASS);
        e.return_value();
        e.end_method();

        VisibilityPredicate vp = new VisibilityPredicate(type, false);
        List methods = ReflectUtils.addAllMethods(type, new ArrayList());
        CollectionUtils.filter(methods, vp);
        CollectionUtils.filter(methods, new DuplicatesPredicate());
        List constructors = new ArrayList(Arrays.asList(type.getDeclaredConstructors()));
        CollectionUtils.filter(constructors, vp);

        // getIndex(String)
        emitIndexBySignature(methods);

        // getIndex(String, Class[])
        emitIndexByClassArray(methods);

        // getIndex(Class[])
        e = begin_method(Constants.ACC_PUBLIC, CONSTRUCTOR_GET_INDEX, null);
        e.load_args();
        List info = CollectionUtils.transform(constructors, MethodInfoTransformer.getInstance());
        EmitUtils.constructor_switch(e, info, new GetIndexCallback(e, info));
        e.end_method();

        // invoke(int, Object, Object[])
        e = begin_method(Constants.ACC_PUBLIC, INVOKE, INVOCATION_TARGET_EXCEPTION_ARRAY);
        e.load_arg(1);
        e.checkcast(base);
        e.load_arg(0);
        invokeSwitchHelper(e, methods, 2, base);
        e.end_method();

        // newInstance(int, Object[])
        e = begin_method(Constants.ACC_PUBLIC, NEW_INSTANCE, INVOCATION_TARGET_EXCEPTION_ARRAY);
        e.new_instance(base);
        e.dup();
        e.load_arg(0);
        invokeSwitchHelper(e, constructors, 1, base);
        e.end_method();

        // getMaxIndex()
        e = begin_method(Constants.ACC_PUBLIC, GET_MAX_INDEX, null);
        e.push(methods.size() - 1);
        e.return_value();
        e.end_method();

        end_class();
    }

    // TODO: support constructor indices ("<init>")
    private void emitIndexBySignature(List methods) {
        CodeEmitter e = begin_method(Constants.ACC_PUBLIC, SIGNATURE_GET_INDEX, null);
        List signatures = CollectionUtils.transform(methods, obj -> ReflectUtils.getSignature((Method)obj).toString());
        e.load_arg(0);
        e.invoke_virtual(Constants.TYPE_OBJECT, TO_STRING);
        signatureSwitchHelper(e, signatures);
        e.end_method();
    }

    private static final int TOO_MANY_METHODS = 100; // TODO
    private void emitIndexByClassArray(List methods) {
        CodeEmitter e = begin_method(Constants.ACC_PUBLIC, METHOD_GET_INDEX, null);
        if (methods.size() > TOO_MANY_METHODS) {
            // hack for big classes
            List signatures = CollectionUtils.transform(methods, obj -> {
			    String s = ReflectUtils.getSignature((Method)obj).toString();
			    return s.substring(0, s.lastIndexOf(')') + 1);
			});
            e.load_args();
            e.invoke_static(FAST_CLASS, GET_SIGNATURE_WITHOUT_RETURN_TYPE);
            signatureSwitchHelper(e, signatures);
        } else {
            e.load_args();
            List info = CollectionUtils.transform(methods, MethodInfoTransformer.getInstance());
            EmitUtils.method_switch(e, info, new GetIndexCallback(e, info));
        }
        e.end_method();
    }

    private void signatureSwitchHelper(final CodeEmitter e, final List signatures) {
        ObjectSwitchCallback callback = new ObjectSwitchCallback() {
            @Override
			public void processCase(Object key, Label end) {
                // TODO: remove linear indexOf
                e.push(signatures.indexOf(key));
                e.return_value();
            }
            @Override
			public void processDefault() {
                e.push(-1);
                e.return_value();
            }
        };
        EmitUtils.string_switch(e,
                                (String[])signatures.toArray(new String[signatures.size()]),
                                Constants.SWITCH_STYLE_HASH,
                                callback);
    }

    private static void invokeSwitchHelper(final CodeEmitter e, List members, final int arg, final Type base) {
        final List info = CollectionUtils.transform(members, MethodInfoTransformer.getInstance());
        final Label illegalArg = e.make_label();
        Block block = e.begin_block();
        e.process_switch(getIntRange(info.size()), new ProcessSwitchCallback() {
            @Override
			public void processCase(int key, Label end) {
                MethodInfo method = (MethodInfo)info.get(key);
                Type[] types = method.getSignature().getArgumentTypes();
                for (int i = 0; i < types.length; i++) {
                    e.load_arg(arg);
                    e.aaload(i);
                    e.unbox(types[i]);
                }
                // TODO: change method lookup process so MethodInfo will already reference base
                // instead of superclass when superclass method is inaccessible
                e.invoke(method, base);
                if (!TypeUtils.isConstructor(method)) {
                    e.box(method.getSignature().getReturnType());
                }
                e.return_value();
            }
            @Override
			public void processDefault() {
                e.goTo(illegalArg);
            }
        });
        block.end();
        EmitUtils.wrap_throwable(block, INVOCATION_TARGET_EXCEPTION);
        e.mark(illegalArg);
        e.throw_exception(ILLEGAL_ARGUMENT_EXCEPTION, "Cannot find matching method/constructor");
    }

    private static class GetIndexCallback implements ObjectSwitchCallback {
        private CodeEmitter e;
        private Map indexes = new HashMap();

        public GetIndexCallback(CodeEmitter e, List methods) {
            this.e = e;
            int index = 0;
            for (Iterator it = methods.iterator(); it.hasNext();) {
                indexes.put(it.next(), index++);
            }
        }

        @Override
		public void processCase(Object key, Label end) {
            e.push(((Integer)indexes.get(key)));
            e.return_value();
        }

        @Override
		public void processDefault() {
            e.push(-1);
            e.return_value();
        }
    }

    private static int[] getIntRange(int length) {
        int[] range = new int[length];
        for (int i = 0; i < length; i++) {
            range[i] = i;
        }
        return range;
    }
}
