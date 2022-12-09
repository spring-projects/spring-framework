/*
 * Copyright 2004 The Apache Software Foundation
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

import org.springframework.asm.Type;

@SuppressWarnings({"rawtypes", "unchecked"})
class CallbackInfo
{
    public static Type[] determineTypes(Class[] callbackTypes) {
        return determineTypes(callbackTypes, true);
    }

    public static Type[] determineTypes(Class[] callbackTypes, boolean checkAll) {
        Type[] types = new Type[callbackTypes.length];
        for (int i = 0; i < types.length; i++) {
            types[i] = determineType(callbackTypes[i], checkAll);
        }
        return types;
    }

    public static Type[] determineTypes(Callback[] callbacks) {
        return determineTypes(callbacks, true);
    }

    public static Type[] determineTypes(Callback[] callbacks, boolean checkAll) {
        Type[] types = new Type[callbacks.length];
        for (int i = 0; i < types.length; i++) {
            types[i] = determineType(callbacks[i], checkAll);
        }
        return types;
    }

    public static CallbackGenerator[] getGenerators(Type[] callbackTypes) {
        CallbackGenerator[] generators = new CallbackGenerator[callbackTypes.length];
        for (int i = 0; i < generators.length; i++) {
            generators[i] = getGenerator(callbackTypes[i]);
        }
        return generators;
    }

    //////////////////// PRIVATE ////////////////////

    private Class cls;
    private CallbackGenerator generator;
    private Type type;

    private static final CallbackInfo[] CALLBACKS = {
        new CallbackInfo(NoOp.class, NoOpGenerator.INSTANCE),
        new CallbackInfo(MethodInterceptor.class, MethodInterceptorGenerator.INSTANCE),
        new CallbackInfo(InvocationHandler.class, InvocationHandlerGenerator.INSTANCE),
        new CallbackInfo(LazyLoader.class, LazyLoaderGenerator.INSTANCE),
        new CallbackInfo(Dispatcher.class, DispatcherGenerator.INSTANCE),
        new CallbackInfo(FixedValue.class, FixedValueGenerator.INSTANCE),
        new CallbackInfo(ProxyRefDispatcher.class, DispatcherGenerator.PROXY_REF_INSTANCE),
    };

    private CallbackInfo(Class cls, CallbackGenerator generator) {
        this.cls = cls;
        this.generator = generator;
        type = Type.getType(cls);
    }

    private static Type determineType(Callback callback, boolean checkAll) {
        if (callback == null) {
            throw new IllegalStateException("Callback is null");
        }
        return determineType(callback.getClass(), checkAll);
    }

    private static Type determineType(Class callbackType, boolean checkAll) {
        Class cur = null;
        Type type = null;
        for (CallbackInfo info : CALLBACKS) {
            if (info.cls.isAssignableFrom(callbackType)) {
                if (cur != null) {
                    throw new IllegalStateException("Callback implements both " + cur + " and " + info.cls);
                }
                cur = info.cls;
                type = info.type;
                if (!checkAll) {
                    break;
                }
            }
        }
        if (cur == null) {
            throw new IllegalStateException("Unknown callback type " + callbackType);
        }
        return type;
    }

    private static CallbackGenerator getGenerator(Type callbackType) {
        for (CallbackInfo info : CALLBACKS) {
            if (info.type.equals(callbackType)) {
                return info.generator;
            }
        }
        throw new IllegalStateException("Unknown callback type " + callbackType);
    }
}


