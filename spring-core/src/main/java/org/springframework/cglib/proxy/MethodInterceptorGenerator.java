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

package org.springframework.cglib.proxy;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.asm.Label;
import org.springframework.asm.Type;
import org.springframework.cglib.core.ClassEmitter;
import org.springframework.cglib.core.ClassInfo;
import org.springframework.cglib.core.CodeEmitter;
import org.springframework.cglib.core.CollectionUtils;
import org.springframework.cglib.core.Constants;
import org.springframework.cglib.core.EmitUtils;
import org.springframework.cglib.core.Local;
import org.springframework.cglib.core.MethodInfo;
import org.springframework.cglib.core.ObjectSwitchCallback;
import org.springframework.cglib.core.Signature;
import org.springframework.cglib.core.Transformer;
import org.springframework.cglib.core.TypeUtils;

@SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
class MethodInterceptorGenerator
implements CallbackGenerator
{
    public static final MethodInterceptorGenerator INSTANCE = new MethodInterceptorGenerator();

    static final String EMPTY_ARGS_NAME = "CGLIB$emptyArgs";
    static final String FIND_PROXY_NAME = "CGLIB$findMethodProxy";
    static final Class[] FIND_PROXY_TYPES = { Signature.class };

    private static final Type ABSTRACT_METHOD_ERROR =
      TypeUtils.parseType("AbstractMethodError");
    private static final Type METHOD =
      TypeUtils.parseType("java.lang.reflect.Method");
    private static final Type REFLECT_UTILS =
      TypeUtils.parseType("org.springframework.cglib.core.ReflectUtils");
    private static final Type METHOD_PROXY =
      TypeUtils.parseType("org.springframework.cglib.proxy.MethodProxy");
    private static final Type METHOD_INTERCEPTOR =
      TypeUtils.parseType("org.springframework.cglib.proxy.MethodInterceptor");
    private static final Signature GET_DECLARED_METHODS =
      TypeUtils.parseSignature("java.lang.reflect.Method[] getDeclaredMethods()");
    private static final Signature FIND_METHODS =
      TypeUtils.parseSignature("java.lang.reflect.Method[] findMethods(String[], java.lang.reflect.Method[])");
    private static final Signature MAKE_PROXY =
      new Signature("create", METHOD_PROXY, new Type[]{
          Constants.TYPE_CLASS,
          Constants.TYPE_CLASS,
          Constants.TYPE_STRING,
          Constants.TYPE_STRING,
          Constants.TYPE_STRING
      });
    private static final Signature INTERCEPT =
      new Signature("intercept", Constants.TYPE_OBJECT, new Type[]{
          Constants.TYPE_OBJECT,
          METHOD,
          Constants.TYPE_OBJECT_ARRAY,
          METHOD_PROXY
      });
    private static final Signature FIND_PROXY =
      new Signature(FIND_PROXY_NAME, METHOD_PROXY, new Type[]{ Constants.TYPE_SIGNATURE });
    private static final Signature TO_STRING =
      TypeUtils.parseSignature("String toString()");
    private static final Transformer METHOD_TO_CLASS = value -> ((MethodInfo)value).getClassInfo();

    private String getMethodField(Signature impl) {
        return impl.getName() + "$Method";
    }
    private String getMethodProxyField(Signature impl) {
        return impl.getName() + "$Proxy";
    }

    @Override
    public void generate(ClassEmitter ce, Context context, List methods) {
        Map sigMap = new HashMap();
        for (Iterator it = methods.iterator(); it.hasNext();) {
            MethodInfo method = (MethodInfo)it.next();
            Signature sig = method.getSignature();
            Signature impl = context.getImplSignature(method);

            String methodField = getMethodField(impl);
            String methodProxyField = getMethodProxyField(impl);

            sigMap.put(sig.toString(), methodProxyField);
            ce.declare_field(Constants.PRIVATE_FINAL_STATIC, methodField, METHOD, null);
            ce.declare_field(Constants.PRIVATE_FINAL_STATIC, methodProxyField, METHOD_PROXY, null);
            ce.declare_field(Constants.PRIVATE_FINAL_STATIC, EMPTY_ARGS_NAME, Constants.TYPE_OBJECT_ARRAY, null);
            CodeEmitter e;

            // access method
            e = ce.begin_method(Constants.ACC_FINAL,
                                impl,
                                method.getExceptionTypes());
            superHelper(e, method, context);
            e.return_value();
            e.end_method();

            // around method
            e = context.beginMethod(ce, method);
            Label nullInterceptor = e.make_label();
            context.emitCallback(e, context.getIndex(method));
            e.dup();
            e.ifnull(nullInterceptor);

            e.load_this();
            e.getfield(methodField);

            if (sig.getArgumentTypes().length == 0) {
                e.getfield(EMPTY_ARGS_NAME);
            } else {
                e.create_arg_array();
            }

            e.getfield(methodProxyField);
            e.invoke_interface(METHOD_INTERCEPTOR, INTERCEPT);
            e.unbox_or_zero(sig.getReturnType());
            e.return_value();

            e.mark(nullInterceptor);
            superHelper(e, method, context);
            e.return_value();
            e.end_method();
        }
        generateFindProxy(ce, sigMap);
    }

    private static void superHelper(CodeEmitter e, MethodInfo method, Context context)
    {
        if (TypeUtils.isAbstract(method.getModifiers())) {
            e.throw_exception(ABSTRACT_METHOD_ERROR, method.toString() + " is abstract" );
        } else {
            e.load_this();
            context.emitLoadArgsAndInvoke(e, method);
        }
    }

    @Override
    public void generateStatic(CodeEmitter e, Context context, List methods) throws Exception {
        /* generates:
           static {
             Class thisClass = Class.forName("NameOfThisClass");
             Class cls = Class.forName("java.lang.Object");
             String[] sigs = new String[]{ "toString", "()Ljava/lang/String;", ... };
             Method[] methods = cls.getDeclaredMethods();
             methods = ReflectUtils.findMethods(sigs, methods);
             METHOD_0 = methods[0];
             CGLIB$ACCESS_0 = MethodProxy.create(cls, thisClass, "()Ljava/lang/String;", "toString", "CGLIB$ACCESS_0");
             ...
           }
        */

        e.push(0);
        e.newarray();
        e.putfield(EMPTY_ARGS_NAME);

        Local thisclass = e.make_local();
        Local declaringclass = e.make_local();
        EmitUtils.load_class_this(e);
        e.store_local(thisclass);

        Map methodsByClass = CollectionUtils.bucket(methods, METHOD_TO_CLASS);
        for (Iterator i = methodsByClass.keySet().iterator(); i.hasNext();) {
            ClassInfo classInfo = (ClassInfo)i.next();

            List classMethods = (List)methodsByClass.get(classInfo);
            e.push(2 * classMethods.size());
            e.newarray(Constants.TYPE_STRING);
            for (int index = 0; index < classMethods.size(); index++) {
                MethodInfo method = (MethodInfo)classMethods.get(index);
                Signature sig = method.getSignature();
                e.dup();
                e.push(2 * index);
                e.push(sig.getName());
                e.aastore();
                e.dup();
                e.push(2 * index + 1);
                e.push(sig.getDescriptor());
                e.aastore();
            }

            EmitUtils.load_class(e, classInfo.getType());
            e.dup();
            e.store_local(declaringclass);
            e.invoke_virtual(Constants.TYPE_CLASS, GET_DECLARED_METHODS);
            e.invoke_static(REFLECT_UTILS, FIND_METHODS);

            for (int index = 0; index < classMethods.size(); index++) {
                MethodInfo method = (MethodInfo)classMethods.get(index);
                Signature sig = method.getSignature();
                Signature impl = context.getImplSignature(method);
                e.dup();
                e.push(index);
                e.array_load(METHOD);
                e.putfield(getMethodField(impl));

                e.load_local(declaringclass);
                e.load_local(thisclass);
                e.push(sig.getDescriptor());
                e.push(sig.getName());
                e.push(impl.getName());
                e.invoke_static(METHOD_PROXY, MAKE_PROXY);
                e.putfield(getMethodProxyField(impl));
            }
            e.pop();
        }
    }

    public void generateFindProxy(ClassEmitter ce, final Map sigMap) {
        final CodeEmitter e = ce.begin_method(Constants.ACC_PUBLIC | Constants.ACC_STATIC,
                                              FIND_PROXY,
                                              null);
        e.load_arg(0);
        e.invoke_virtual(Constants.TYPE_OBJECT, TO_STRING);
        ObjectSwitchCallback callback = new ObjectSwitchCallback() {
            @Override
            public void processCase(Object key, Label end) {
                e.getfield((String)sigMap.get(key));
                e.return_value();
            }
            @Override
            public void processDefault() {
                e.aconst_null();
                e.return_value();
            }
        };
        EmitUtils.string_switch(e,
                                (String[])sigMap.keySet().toArray(new String[0]),
                                Constants.SWITCH_STYLE_HASH,
                                callback);
        e.end_method();
    }
}
