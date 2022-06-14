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
package org.springframework.cglib.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import org.springframework.cglib.core.*;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.Type;

@SuppressWarnings({"rawtypes", "unchecked"})
class BulkBeanEmitter extends ClassEmitter {
    private static final Signature GET_PROPERTY_VALUES =
      TypeUtils.parseSignature("void getPropertyValues(Object, Object[])");
    private static final Signature SET_PROPERTY_VALUES =
      TypeUtils.parseSignature("void setPropertyValues(Object, Object[])");
    private static final Signature CSTRUCT_EXCEPTION =
      TypeUtils.parseConstructor("Throwable, int");
    private static final Type BULK_BEAN =
      TypeUtils.parseType("org.springframework.cglib.beans.BulkBean");
    private static final Type BULK_BEAN_EXCEPTION =
      TypeUtils.parseType("org.springframework.cglib.beans.BulkBeanException");
        
    public BulkBeanEmitter(ClassVisitor v,
                           String className,
                           Class target,
                           String[] getterNames,
                           String[] setterNames,
                           Class[] types) {
        super(v);

        Method[] getters = new Method[getterNames.length];
        Method[] setters = new Method[setterNames.length];
        validate(target, getterNames, setterNames, types, getters, setters);

        begin_class(Constants.V1_8, Constants.ACC_PUBLIC, className, BULK_BEAN, null, Constants.SOURCE_FILE);
        EmitUtils.null_constructor(this);
        generateGet(target, getters);
        generateSet(target, setters);
        end_class();
    }

    private void generateGet(final Class target, final Method[] getters) {
        CodeEmitter e = begin_method(Constants.ACC_PUBLIC, GET_PROPERTY_VALUES, null);
        if (getters.length > 0) {
            e.load_arg(0);
            e.checkcast(Type.getType(target));
            Local bean = e.make_local();
            e.store_local(bean);
            for (int i = 0; i < getters.length; i++) {
                if (getters[i] != null) {
                    MethodInfo getter = ReflectUtils.getMethodInfo(getters[i]);
                    e.load_arg(1);
                    e.push(i);
                    e.load_local(bean);
                    e.invoke(getter);
                    e.box(getter.getSignature().getReturnType());
                    e.aastore();
                }
            }
        }
        e.return_value();
        e.end_method();
    }

    private void generateSet(final Class target, final Method[] setters) {
        // setPropertyValues
        CodeEmitter e = begin_method(Constants.ACC_PUBLIC, SET_PROPERTY_VALUES, null);
        if (setters.length > 0) {
            Local index = e.make_local(Type.INT_TYPE);
            e.push(0);
            e.store_local(index);
            e.load_arg(0);
            e.checkcast(Type.getType(target));
            e.load_arg(1);
            Block handler = e.begin_block();
            int lastIndex = 0;
            for (int i = 0; i < setters.length; i++) {
                if (setters[i] != null) {
                    MethodInfo setter = ReflectUtils.getMethodInfo(setters[i]);
                    int diff = i - lastIndex;
                    if (diff > 0) {
                        e.iinc(index, diff);
                        lastIndex = i;
                    }
                    e.dup2();
                    e.aaload(i);
                    e.unbox(setter.getSignature().getArgumentTypes()[0]);
                    e.invoke(setter);
                }
            }
            handler.end();
            e.return_value();
            e.catch_exception(handler, Constants.TYPE_THROWABLE);
            e.new_instance(BULK_BEAN_EXCEPTION);
            e.dup_x1();
            e.swap();
            e.load_local(index);
            e.invoke_constructor(BULK_BEAN_EXCEPTION, CSTRUCT_EXCEPTION);
            e.athrow();
        } else {
            e.return_value();
        }
        e.end_method();
    }
    
    private static void validate(Class target,
                                 String[] getters,
                                 String[] setters,
                                 Class[] types,
                                 Method[] getters_out,
                                 Method[] setters_out) {
        int i = -1;
        if (setters.length != types.length || getters.length != types.length) {
            throw new BulkBeanException("accessor array length must be equal type array length", i);
        }
        try {
            for (i = 0; i < types.length; i++) {
                if (getters[i] != null) {
                    Method method = ReflectUtils.findDeclaredMethod(target, getters[i], null);
                    if (method.getReturnType() != types[i]) {
                        throw new BulkBeanException("Specified type " + types[i] +
                                                    " does not match declared type " + method.getReturnType(), i);
                    }
                    if (Modifier.isPrivate(method.getModifiers())) {
                        throw new BulkBeanException("Property is private", i);
                    }
                    getters_out[i] = method;
                }
                if (setters[i] != null) {
                    Method method = ReflectUtils.findDeclaredMethod(target, setters[i], new Class[]{ types[i] });
                    if (Modifier.isPrivate(method.getModifiers()) ){
                        throw new BulkBeanException("Property is private", i);
                    }
                    setters_out[i] = method;
                }
            }
        } catch (NoSuchMethodException e) {
            throw new BulkBeanException("Cannot find specified property", i);
        }
    }
}
