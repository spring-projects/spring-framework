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

import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.asm.ClassVisitor;
import org.springframework.asm.Type;
import org.springframework.cglib.core.AbstractClassGenerator;
import org.springframework.cglib.core.ClassEmitter;
import org.springframework.cglib.core.CodeEmitter;
import org.springframework.cglib.core.Constants;
import org.springframework.cglib.core.EmitUtils;
import org.springframework.cglib.core.Local;
import org.springframework.cglib.core.MethodInfo;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.cglib.core.Signature;
import org.springframework.cglib.core.TypeUtils;

@SuppressWarnings({"rawtypes", "unchecked"})
abstract public class MulticastDelegate implements Cloneable {
    protected Object[] targets = {};

    protected MulticastDelegate() {
    }

    public List getTargets() {
        return new ArrayList(Arrays.asList(targets));
    }

    abstract public MulticastDelegate add(Object target);

    protected MulticastDelegate addHelper(Object target) {
        MulticastDelegate copy = newInstance();
        copy.targets = new Object[targets.length + 1];
        System.arraycopy(targets, 0, copy.targets, 0, targets.length);
        copy.targets[targets.length] = target;
        return copy;
    }

    public MulticastDelegate remove(Object target) {
        for (int i = targets.length - 1; i >= 0; i--) {
            if (targets[i].equals(target)) {
                MulticastDelegate copy = newInstance();
                copy.targets = new Object[targets.length - 1];
                System.arraycopy(targets, 0, copy.targets, 0, i);
                System.arraycopy(targets, i + 1, copy.targets, i, targets.length - i - 1);
                return copy;
            }
        }
        return this;
    }

    abstract public MulticastDelegate newInstance();

    public static MulticastDelegate create(Class iface) {
        Generator gen = new Generator();
        gen.setInterface(iface);
        return gen.create();
    }

    public static class Generator extends AbstractClassGenerator {
        private static final Source SOURCE = new Source(MulticastDelegate.class.getName());
        private static final Type MULTICAST_DELEGATE =
          TypeUtils.parseType("org.springframework.cglib.reflect.MulticastDelegate");
        private static final Signature NEW_INSTANCE =
          new Signature("newInstance", MULTICAST_DELEGATE, new Type[0]);
        private static final Signature ADD_DELEGATE =
          new Signature("add", MULTICAST_DELEGATE, new Type[]{ Constants.TYPE_OBJECT });
        private static final Signature ADD_HELPER =
          new Signature("addHelper", MULTICAST_DELEGATE, new Type[]{ Constants.TYPE_OBJECT });

        private Class iface;

        public Generator() {
            super(SOURCE);
        }

        @Override
        protected ClassLoader getDefaultClassLoader() {
            return iface.getClassLoader();
        }

        @Override
        protected ProtectionDomain getProtectionDomain() {
            return ReflectUtils.getProtectionDomain(iface);
        }

        public void setInterface(Class iface) {
            this.iface = iface;
        }

        public MulticastDelegate create() {
            setNamePrefix(MulticastDelegate.class.getName());
            return (MulticastDelegate)super.create(iface.getName());
        }

        @Override
        public void generateClass(ClassVisitor cv) {
            final MethodInfo method = ReflectUtils.getMethodInfo(ReflectUtils.findInterfaceMethod(iface));

            ClassEmitter ce = new ClassEmitter(cv);
            ce.begin_class(Constants.V1_8,
                           Constants.ACC_PUBLIC,
                           getClassName(),
                           MULTICAST_DELEGATE,
                           new Type[]{ Type.getType(iface) },
                           Constants.SOURCE_FILE);
            EmitUtils.null_constructor(ce);

            // generate proxied method
            emitProxy(ce, method);

            // newInstance
            CodeEmitter e = ce.begin_method(Constants.ACC_PUBLIC, NEW_INSTANCE, null);
            e.new_instance_this();
            e.dup();
            e.invoke_constructor_this();
            e.return_value();
            e.end_method();

            // add
            e = ce.begin_method(Constants.ACC_PUBLIC, ADD_DELEGATE, null);
            e.load_this();
            e.load_arg(0);
            e.checkcast(Type.getType(iface));
            e.invoke_virtual_this(ADD_HELPER);
            e.return_value();
            e.end_method();

            ce.end_class();
        }

        private void emitProxy(ClassEmitter ce, final MethodInfo method) {
            int modifiers = Constants.ACC_PUBLIC;
            if ((method.getModifiers() & Constants.ACC_VARARGS) == Constants.ACC_VARARGS) {
                modifiers |= Constants.ACC_VARARGS;
            }
            final CodeEmitter e = EmitUtils.begin_method(ce, method, modifiers);
            Type returnType = method.getSignature().getReturnType();
            final boolean returns = returnType != Type.VOID_TYPE;
            Local result = null;
            if (returns) {
                result = e.make_local(returnType);
                e.zero_or_null(returnType);
                e.store_local(result);
            }
            e.load_this();
            e.super_getfield("targets", Constants.TYPE_OBJECT_ARRAY);
            final Local result2 = result;
            EmitUtils.process_array(e, Constants.TYPE_OBJECT_ARRAY, type -> {
			    e.checkcast(Type.getType(iface));
			    e.load_args();
			    e.invoke(method);
			    if (returns) {
			        e.store_local(result2);
			    }
			});
            if (returns) {
                e.load_local(result);
            }
            e.return_value();
            e.end_method();
        }

        @Override
        protected Object firstInstance(Class type) {
            // make a new instance in case first object is used with a long list of targets
            return ((MulticastDelegate)ReflectUtils.newInstance(type)).newInstance();
        }

        @Override
        protected Object nextInstance(Object instance) {
            return ((MulticastDelegate)instance).newInstance();
        }
    }
}
