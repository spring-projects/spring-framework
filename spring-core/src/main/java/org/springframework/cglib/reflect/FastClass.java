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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import org.springframework.asm.ClassVisitor;
import org.springframework.asm.Type;
import org.springframework.cglib.core.AbstractClassGenerator;
import org.springframework.cglib.core.Constants;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.cglib.core.Signature;

@SuppressWarnings({"rawtypes", "unchecked"})
abstract public class FastClass
{
    private Class type;

    protected FastClass() {
        throw new Error("Using the FastClass empty constructor--please report to the cglib-devel mailing list");
    }

    protected FastClass(Class type) {
        this.type = type;
    }

    public static FastClass create(Class type) {

        return create(type.getClassLoader(),type);

    }
    public static FastClass create(ClassLoader loader, Class type) {
        Generator gen = new Generator();
        gen.setType(type);
        gen.setClassLoader(loader);
        return gen.create();
    }

    public static class Generator extends AbstractClassGenerator
    {
        private static final Source SOURCE = new Source(FastClass.class.getName());
        private Class type;

        public Generator() {
            super(SOURCE);
        }

        public void setType(Class type) {
            this.type = type;
        }

        public FastClass create() {
            setNamePrefix(type.getName());
            return (FastClass)super.create(type.getName());
        }

        @Override
		protected ClassLoader getDefaultClassLoader() {
            return type.getClassLoader();
        }

        @Override
		protected ProtectionDomain getProtectionDomain() {
        	return ReflectUtils.getProtectionDomain(type);
        }

        @Override
		public void generateClass(ClassVisitor v) throws Exception {
            new FastClassEmitter(v, getClassName(), type);
        }

        @Override
		protected Object firstInstance(Class type) {
            return ReflectUtils.newInstance(type,
                                            new Class[]{ Class.class },
                                            new Object[]{ this.type });
        }

        @Override
		protected Object nextInstance(Object instance) {
            return instance;
        }
    }

    public Object invoke(String name, Class[] parameterTypes, Object obj, Object[] args) throws InvocationTargetException {
        return invoke(getIndex(name, parameterTypes), obj, args);
    }

    public Object newInstance() throws InvocationTargetException {
        return newInstance(getIndex(Constants.EMPTY_CLASS_ARRAY), null);
    }

    public Object newInstance(Class[] parameterTypes, Object[] args) throws InvocationTargetException {
        return newInstance(getIndex(parameterTypes), args);
    }

    public FastMethod getMethod(Method method) {
        return new FastMethod(this, method);
    }

    public FastConstructor getConstructor(Constructor constructor) {
        return new FastConstructor(this, constructor);
    }

    public FastMethod getMethod(String name, Class[] parameterTypes) {
        try {
            return getMethod(type.getMethod(name, parameterTypes));
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        }
    }

    public FastConstructor getConstructor(Class[] parameterTypes) {
        try {
            return getConstructor(type.getConstructor(parameterTypes));
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        }
    }

    public String getName() {
        return type.getName();
    }

    public Class getJavaClass() {
        return type;
    }

    @Override
	public String toString() {
        return type.toString();
    }

    @Override
	public int hashCode() {
        return type.hashCode();
    }

    @Override
	public boolean equals(Object o) {
        if (o == null || !(o instanceof FastClass that)) {
            return false;
        }
        return type.equals(that.type);
    }

    /**
     * Return the index of the matching method. The index may be used
     * later to invoke the method with less overhead. If more than one
     * method matches (i.e. they differ by return type only), one is
     * chosen arbitrarily.
     * @see #invoke(int, Object, Object[])
     * @param name the method name
     * @param parameterTypes the parameter array
     * @return the index, or <code>-1</code> if none is found.
     */
    abstract public int getIndex(String name, Class[] parameterTypes);

    /**
     * Return the index of the matching constructor. The index may be used
     * later to create a new instance with less overhead.
     * @see #newInstance(int, Object[])
     * @param parameterTypes the parameter array
     * @return the constructor index, or <code>-1</code> if none is found.
     */
    abstract public int getIndex(Class[] parameterTypes);

    /**
     * Invoke the method with the specified index.
     * @see getIndex(name, Class[])
     * @param index the method index
     * @param obj the object the underlying method is invoked from
     * @param args the arguments used for the method call
     * @throws java.lang.reflect.InvocationTargetException if the underlying method throws an exception
     */
    abstract public Object invoke(int index, Object obj, Object[] args) throws InvocationTargetException;

    /**
     * Create a new instance using the specified constructor index and arguments.
     * @see getIndex(Class[])
     * @param index the constructor index
     * @param args the arguments passed to the constructor
     * @throws java.lang.reflect.InvocationTargetException if the constructor throws an exception
     */
    abstract public Object newInstance(int index, Object[] args) throws InvocationTargetException;

    abstract public int getIndex(Signature sig);

    /**
     * Returns the maximum method index for this class.
     */
    abstract public int getMaxIndex();

    protected static String getSignatureWithoutReturnType(String name, Class[] parameterTypes) {
		StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append('(');
        for (Class parameterType : parameterTypes) {
            sb.append(Type.getDescriptor(parameterType));
        }
        sb.append(')');
        return sb.toString();
    }
}
