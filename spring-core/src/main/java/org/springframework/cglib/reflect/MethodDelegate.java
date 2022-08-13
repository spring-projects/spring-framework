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
import java.security.ProtectionDomain;

import org.springframework.asm.ClassVisitor;
import org.springframework.asm.Type;
import org.springframework.cglib.core.AbstractClassGenerator;
import org.springframework.cglib.core.ClassEmitter;
import org.springframework.cglib.core.CodeEmitter;
import org.springframework.cglib.core.Constants;
import org.springframework.cglib.core.EmitUtils;
import org.springframework.cglib.core.KeyFactory;
import org.springframework.cglib.core.MethodInfo;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.cglib.core.Signature;
import org.springframework.cglib.core.TypeUtils;

// TODO: don't require exact match for return type

/**
 * <b>DOCUMENTATION FROM APACHE AVALON DELEGATE CLASS</b>
 *
 * <p>
 * Delegates are a typesafe pointer to another method.  Since Java does not
 * have language support for such a construct, this utility will construct
 * a proxy that forwards method calls to any method with the same signature.
 * This utility is inspired in part by the C# delegate mechanism.  We
 * implemented it in a Java-centric manner.
 * </p>
 *
 * <h2>Delegate</h2>
 * <p>
 *   Any interface with one method can become the interface for a delegate.
 *   Consider the example below:
 * </p>
 *
 * <pre>
 *   public interface MainDelegate {
 *       int main(String[] args);
 *   }
 * </pre>
 *
 * <p>
 *   The interface above is an example of an interface that can become a
 *   delegate.  It has only one method, and the interface is public.  In
 *   order to create a delegate for that method, all we have to do is
 *   call <code>MethodDelegate.create(this, "alternateMain", MainDelegate.class)</code>.
 *   The following program will show how to use it:
 * </p>
 *
 * <pre>
 *   public class Main {
 *       public static int main( String[] args ) {
 *           Main newMain = new Main();
 *           MainDelegate start = (MainDelegate)
 *               MethodDelegate.create(newMain, "alternateMain", MainDelegate.class);
 *           return start.main( args );
 *       }
 *
 *       public int alternateMain( String[] args ) {
 *           for (int i = 0; i &lt; args.length; i++) {
 *               System.out.println( args[i] );
 *           }
 *           return args.length;
 *       }
 *   }
 * </pre>
 *
 * <p>
 *   By themselves, delegates don't do much.  Their true power lies in the fact that
 *   they can be treated like objects, and passed to other methods.  In fact that is
 *   one of the key building blocks of building Intelligent Agents which in tern are
 *   the foundation of artificial intelligence.  In the above program, we could have
 *   easily created the delegate to match the static <code>main</code> method by
 *   substituting the delegate creation call with this:
 *   <code>MethodDelegate.createStatic(getClass(), "main", MainDelegate.class)</code>.
 * </p>
 * <p>
 *   Another key use for Delegates is to register event listeners.  It is much easier
 *   to have all the code for your events separated out into methods instead of individual
 *   classes.  One of the ways Java gets around that is to create anonymous classes.
 *   They are particularly troublesome because many Debuggers do not know what to do
 *   with them.  Anonymous classes tend to duplicate alot of code as well.  We can
 *   use any interface with one declared method to forward events to any method that
 *   matches the signature (although the method name can be different).
 * </p>
 *
 * <h3>Equality</h3>
 *  The criteria that we use to test if two delegates are equal are:
 *   <ul>
 *     <li>
 *       They both refer to the same instance.  That is, the <code>instance</code>
 *       parameter passed to the newDelegate method was the same for both. The
 *       instances are compared with the identity equality operator, <code>==</code>.
 *     </li>
 *     <li>They refer to the same method as resolved by <code>Method.equals</code>.</li>
 *   </ul>
 *
 * @version $Id: MethodDelegate.java,v 1.25 2006/03/05 02:43:19 herbyderby Exp $
 */
@SuppressWarnings({"rawtypes", "unchecked"})
abstract public class MethodDelegate {
    private static final MethodDelegateKey KEY_FACTORY =
      (MethodDelegateKey)KeyFactory.create(MethodDelegateKey.class, KeyFactory.CLASS_BY_NAME);

    protected Object target;
    protected String eqMethod;

    interface MethodDelegateKey {
        Object newInstance(Class delegateClass, String methodName, Class iface);
    }

    public static MethodDelegate createStatic(Class targetClass, String methodName, Class iface) {
        Generator gen = new Generator();
        gen.setTargetClass(targetClass);
        gen.setMethodName(methodName);
        gen.setInterface(iface);
        return gen.create();
    }

    public static MethodDelegate create(Object target, String methodName, Class iface) {
        Generator gen = new Generator();
        gen.setTarget(target);
        gen.setMethodName(methodName);
        gen.setInterface(iface);
        return gen.create();
    }

    @Override
    public boolean equals(Object obj) {
        MethodDelegate other = (MethodDelegate)obj;
        return (other != null && target == other.target) && eqMethod.equals(other.eqMethod);
    }

    @Override
    public int hashCode() {
        return target.hashCode() ^ eqMethod.hashCode();
    }

    public Object getTarget() {
        return target;
    }

    abstract public MethodDelegate newInstance(Object target);

    public static class Generator extends AbstractClassGenerator {
        private static final Source SOURCE = new Source(MethodDelegate.class.getName());
        private static final Type METHOD_DELEGATE =
          TypeUtils.parseType("org.springframework.cglib.reflect.MethodDelegate");
        private static final Signature NEW_INSTANCE =
          new Signature("newInstance", METHOD_DELEGATE, new Type[]{ Constants.TYPE_OBJECT });

        private Object target;
        private Class targetClass;
        private String methodName;
        private Class iface;

        public Generator() {
            super(SOURCE);
        }

        public void setTarget(Object target) {
            this.target = target;
            this.targetClass = target.getClass();
        }

        public void setTargetClass(Class targetClass) {
            this.targetClass = targetClass;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public void setInterface(Class iface) {
            this.iface = iface;
        }

        @Override
        protected ClassLoader getDefaultClassLoader() {
            return targetClass.getClassLoader();
        }

        @Override
        protected ProtectionDomain getProtectionDomain() {
            return ReflectUtils.getProtectionDomain(targetClass);
        }

        public MethodDelegate create() {
            setNamePrefix(targetClass.getName());
            Object key = KEY_FACTORY.newInstance(targetClass, methodName, iface);
            return (MethodDelegate)super.create(key);
        }

        @Override
        protected Object firstInstance(Class type) {
            return ((MethodDelegate)ReflectUtils.newInstance(type)).newInstance(target);
        }

        @Override
        protected Object nextInstance(Object instance) {
            return ((MethodDelegate)instance).newInstance(target);
        }

        @Override
        public void generateClass(ClassVisitor v) throws NoSuchMethodException {
            Method proxy = ReflectUtils.findInterfaceMethod(iface);
            final Method method = targetClass.getMethod(methodName, proxy.getParameterTypes());
            if (!proxy.getReturnType().isAssignableFrom(method.getReturnType())) {
                throw new IllegalArgumentException("incompatible return types");
            }

            MethodInfo methodInfo = ReflectUtils.getMethodInfo(method);

            boolean isStatic = TypeUtils.isStatic(methodInfo.getModifiers());
            if ((target == null) ^ isStatic) {
                throw new IllegalArgumentException("Static method " + (isStatic ? "not " : "") + "expected");
            }

            ClassEmitter ce = new ClassEmitter(v);
            CodeEmitter e;
            ce.begin_class(Constants.V1_8,
                           Constants.ACC_PUBLIC,
                           getClassName(),
                           METHOD_DELEGATE,
                           new Type[]{ Type.getType(iface) },
                           Constants.SOURCE_FILE);
            ce.declare_field(Constants.PRIVATE_FINAL_STATIC, "eqMethod", Constants.TYPE_STRING, null);
            EmitUtils.null_constructor(ce);

            // generate proxied method
            MethodInfo proxied = ReflectUtils.getMethodInfo(iface.getDeclaredMethods()[0]);
            int modifiers = Constants.ACC_PUBLIC;
            if ((proxied.getModifiers() & Constants.ACC_VARARGS) == Constants.ACC_VARARGS) {
                modifiers |= Constants.ACC_VARARGS;
            }
            e = EmitUtils.begin_method(ce, proxied, modifiers);
            e.load_this();
            e.super_getfield("target", Constants.TYPE_OBJECT);
            e.checkcast(methodInfo.getClassInfo().getType());
            e.load_args();
            e.invoke(methodInfo);
            e.return_value();
            e.end_method();

            // newInstance
            e = ce.begin_method(Constants.ACC_PUBLIC, NEW_INSTANCE, null);
            e.new_instance_this();
            e.dup();
            e.dup2();
            e.invoke_constructor_this();
            e.getfield("eqMethod");
            e.super_putfield("eqMethod", Constants.TYPE_STRING);
            e.load_arg(0);
            e.super_putfield("target", Constants.TYPE_OBJECT);
            e.return_value();
            e.end_method();

            // static initializer
            e = ce.begin_static();
            e.push(methodInfo.getSignature().toString());
            e.putfield("eqMethod");
            e.return_value();
            e.end_method();

            ce.end_class();
        }
    }
}
