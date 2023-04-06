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
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

import org.springframework.asm.ClassVisitor;
import org.springframework.asm.Type;
import org.springframework.cglib.core.AbstractClassGenerator;
import org.springframework.cglib.core.ClassEmitter;
import org.springframework.cglib.core.CodeEmitter;
import org.springframework.cglib.core.Constants;
import org.springframework.cglib.core.Converter;
import org.springframework.cglib.core.EmitUtils;
import org.springframework.cglib.core.KeyFactory;
import org.springframework.cglib.core.Local;
import org.springframework.cglib.core.MethodInfo;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.cglib.core.Signature;
import org.springframework.cglib.core.TypeUtils;

/**
 * @author Chris Nokleberg
 */
@SuppressWarnings({"rawtypes", "unchecked"})
abstract public class BeanCopier
{
    private static final BeanCopierKey KEY_FACTORY =
      (BeanCopierKey)KeyFactory.create(BeanCopierKey.class);
    private static final Type CONVERTER =
      TypeUtils.parseType("org.springframework.cglib.core.Converter");
    private static final Type BEAN_COPIER =
      TypeUtils.parseType("org.springframework.cglib.beans.BeanCopier");
    private static final Signature COPY =
      new Signature("copy", Type.VOID_TYPE, new Type[]{ Constants.TYPE_OBJECT, Constants.TYPE_OBJECT, CONVERTER });
    private static final Signature CONVERT =
      TypeUtils.parseSignature("Object convert(Object, Class, Object)");

    interface BeanCopierKey {
        public Object newInstance(String source, String target, boolean useConverter);
    }

    public static BeanCopier create(Class source, Class target, boolean useConverter) {
        Generator gen = new Generator();
        gen.setSource(source);
        gen.setTarget(target);
        gen.setUseConverter(useConverter);
        return gen.create();
    }

    abstract public void copy(Object from, Object to, Converter converter);

    public static class Generator extends AbstractClassGenerator {
        private static final Source SOURCE = new Source(BeanCopier.class.getName());
        private Class source;
        private Class target;
        private boolean useConverter;

        public Generator() {
            super(SOURCE);
        }

        public void setSource(Class source) {
            if(!Modifier.isPublic(source.getModifiers())){
               setNamePrefix(source.getName());
            }
            this.source = source;
        }

        public void setTarget(Class target) {
            if(!Modifier.isPublic(target.getModifiers())){
               setNamePrefix(target.getName());
            }
            this.target = target;
			// SPRING PATCH BEGIN
			setContextClass(target);
			// SPRING PATCH END
        }

        public void setUseConverter(boolean useConverter) {
            this.useConverter = useConverter;
        }

        @Override
		protected ClassLoader getDefaultClassLoader() {
            return source.getClassLoader();
        }

        @Override
		protected ProtectionDomain getProtectionDomain() {
        	return ReflectUtils.getProtectionDomain(source);
        }

        public BeanCopier create() {
            Object key = KEY_FACTORY.newInstance(source.getName(), target.getName(), useConverter);
            return (BeanCopier)super.create(key);
        }

        @Override
		public void generateClass(ClassVisitor v) {
            Type sourceType = Type.getType(source);
            Type targetType = Type.getType(target);
            ClassEmitter ce = new ClassEmitter(v);
            ce.begin_class(Constants.V1_8,
                           Constants.ACC_PUBLIC,
                           getClassName(),
                           BEAN_COPIER,
                           null,
                           Constants.SOURCE_FILE);

            EmitUtils.null_constructor(ce);
            CodeEmitter e = ce.begin_method(Constants.ACC_PUBLIC, COPY, null);
            PropertyDescriptor[] getters = ReflectUtils.getBeanGetters(source);
            PropertyDescriptor[] setters = ReflectUtils.getBeanSetters(target);

            Map names = new HashMap();
            for (PropertyDescriptor getter : getters) {
                names.put(getter.getName(), getter);
            }
            Local targetLocal = e.make_local();
            Local sourceLocal = e.make_local();
            if (useConverter) {
                e.load_arg(1);
                e.checkcast(targetType);
                e.store_local(targetLocal);
                e.load_arg(0);
                e.checkcast(sourceType);
                e.store_local(sourceLocal);
            } else {
                e.load_arg(1);
                e.checkcast(targetType);
                e.load_arg(0);
                e.checkcast(sourceType);
            }
            for (PropertyDescriptor setter : setters) {
                PropertyDescriptor getter = (PropertyDescriptor)names.get(setter.getName());
                if (getter != null) {
                    MethodInfo read = ReflectUtils.getMethodInfo(getter.getReadMethod());
                    MethodInfo write = ReflectUtils.getMethodInfo(setter.getWriteMethod());
                    if (useConverter) {
                        Type setterType = write.getSignature().getArgumentTypes()[0];
                        e.load_local(targetLocal);
                        e.load_arg(2);
                        e.load_local(sourceLocal);
                        e.invoke(read);
                        e.box(read.getSignature().getReturnType());
                        EmitUtils.load_class(e, setterType);
                        e.push(write.getSignature().getName());
                        e.invoke_interface(CONVERTER, CONVERT);
                        e.unbox_or_zero(setterType);
                        e.invoke(write);
                    } else if (compatible(getter, setter)) {
                        e.dup2();
                        e.invoke(read);
                        e.invoke(write);
                    }
                }
            }
            e.return_value();
            e.end_method();
            ce.end_class();
        }

        private static boolean compatible(PropertyDescriptor getter, PropertyDescriptor setter) {
            // TODO: allow automatic widening conversions?
            return setter.getPropertyType().isAssignableFrom(getter.getPropertyType());
        }

        @Override
		protected Object firstInstance(Class type) {
            return ReflectUtils.newInstance(type);
        }

        @Override
		protected Object nextInstance(Object instance) {
            return instance;
        }
    }
}
