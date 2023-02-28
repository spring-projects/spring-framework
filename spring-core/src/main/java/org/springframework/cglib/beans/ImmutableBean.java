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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import org.springframework.asm.ClassVisitor;
import org.springframework.asm.Type;
import org.springframework.cglib.core.AbstractClassGenerator;
import org.springframework.cglib.core.ClassEmitter;
import org.springframework.cglib.core.CodeEmitter;
import org.springframework.cglib.core.Constants;
import org.springframework.cglib.core.EmitUtils;
import org.springframework.cglib.core.MethodInfo;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.cglib.core.Signature;
import org.springframework.cglib.core.TypeUtils;
/**
 * @author Chris Nokleberg
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ImmutableBean
{
    private static final Type ILLEGAL_STATE_EXCEPTION =
      TypeUtils.parseType("IllegalStateException");
    private static final Signature CSTRUCT_OBJECT =
      TypeUtils.parseConstructor("Object");
    private static final Class[] OBJECT_CLASSES = { Object.class };
    private static final String FIELD_NAME = "CGLIB$RWBean";

    private ImmutableBean() {
    }

    public static Object create(Object bean) {
        Generator gen = new Generator();
        gen.setBean(bean);
        return gen.create();
    }

    public static class Generator extends AbstractClassGenerator {
        private static final Source SOURCE = new Source(ImmutableBean.class.getName());
        private Object bean;
        private Class target;

        public Generator() {
            super(SOURCE);
        }

        public void setBean(Object bean) {
            this.bean = bean;
            target = bean.getClass();
			// SPRING PATCH BEGIN
			setContextClass(target);
			// SPRING PATCH END
        }

        @Override
		protected ClassLoader getDefaultClassLoader() {
            return target.getClassLoader();
        }

        @Override
		protected ProtectionDomain getProtectionDomain() {
        	return ReflectUtils.getProtectionDomain(target);
        }

        public Object create() {
            String name = target.getName();
            setNamePrefix(name);
            return super.create(name);
        }

        @Override
		public void generateClass(ClassVisitor v) {
            Type targetType = Type.getType(target);
            ClassEmitter ce = new ClassEmitter(v);
            ce.begin_class(Constants.V1_8,
                           Constants.ACC_PUBLIC,
                           getClassName(),
                           targetType,
                           null,
                           Constants.SOURCE_FILE);

            ce.declare_field(Constants.ACC_FINAL | Constants.ACC_PRIVATE, FIELD_NAME, targetType, null);

            CodeEmitter e = ce.begin_method(Constants.ACC_PUBLIC, CSTRUCT_OBJECT, null);
            e.load_this();
            e.super_invoke_constructor();
            e.load_this();
            e.load_arg(0);
            e.checkcast(targetType);
            e.putfield(FIELD_NAME);
            e.return_value();
            e.end_method();

            PropertyDescriptor[] descriptors = ReflectUtils.getBeanProperties(target);
            Method[] getters = ReflectUtils.getPropertyMethods(descriptors, true, false);
            Method[] setters = ReflectUtils.getPropertyMethods(descriptors, false, true);

            for (Method getter2 : getters) {
                MethodInfo getter = ReflectUtils.getMethodInfo(getter2);
                e = EmitUtils.begin_method(ce, getter, Constants.ACC_PUBLIC);
                e.load_this();
                e.getfield(FIELD_NAME);
                e.invoke(getter);
                e.return_value();
                e.end_method();
            }

            for (Method setter2 : setters) {
                MethodInfo setter = ReflectUtils.getMethodInfo(setter2);
                e = EmitUtils.begin_method(ce, setter, Constants.ACC_PUBLIC);
                e.throw_exception(ILLEGAL_STATE_EXCEPTION, "Bean is immutable");
                e.end_method();
            }

            ce.end_class();
        }

        @Override
		protected Object firstInstance(Class type) {
            return ReflectUtils.newInstance(type, OBJECT_CLASSES, new Object[]{ bean });
        }

        // TODO: optimize
        @Override
		protected Object nextInstance(Object instance) {
            return firstInstance(instance.getClass());
        }
    }
}
