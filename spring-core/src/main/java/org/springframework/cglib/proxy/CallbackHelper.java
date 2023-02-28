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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cglib.core.ReflectUtils;

/**
 * @version $Id: CallbackHelper.java,v 1.2 2004/06/24 21:15:20 herbyderby Exp $
 */
@SuppressWarnings({"rawtypes", "unchecked"})
abstract public class CallbackHelper
implements CallbackFilter
{
    private Map methodMap = new HashMap();
    private List callbacks = new ArrayList();

    public CallbackHelper(Class superclass, Class[] interfaces) {
        List methods = new ArrayList();
        Enhancer.getMethods(superclass, interfaces, methods);
        Map indexes = new HashMap();
        for (int i = 0, size = methods.size(); i < size; i++) {
            Method method = (Method)methods.get(i);
            Object callback = getCallback(method);
            if (callback == null) {
                throw new IllegalStateException("getCallback cannot return null");
            }
            boolean isCallback = callback instanceof Callback;
            if (!(isCallback || (callback instanceof Class))) {
                throw new IllegalStateException("getCallback must return a Callback or a Class");
            }
            if (i > 0 && ((callbacks.get(i - 1) instanceof Callback) ^ isCallback)) {
                throw new IllegalStateException("getCallback must return a Callback or a Class consistently for every Method");
            }
            Integer index = (Integer)indexes.get(callback);
            if (index == null) {
                index = callbacks.size();
                indexes.put(callback, index);
            }
            methodMap.put(method, index);
            callbacks.add(callback);
        }
    }

    abstract protected Object getCallback(Method method);

    public Callback[] getCallbacks() {
        if (callbacks.size() == 0) {
            return new Callback[0];
        }
        if (callbacks.get(0) instanceof Callback) {
            return (Callback[])callbacks.toArray(new Callback[callbacks.size()]);
        }
        else {
            throw new IllegalStateException("getCallback returned classes, not callbacks; call getCallbackTypes instead");
        }
    }

    public Class[] getCallbackTypes() {
        if (callbacks.size() == 0) {
            return new Class[0];
        }
        if (callbacks.get(0) instanceof Callback) {
            return ReflectUtils.getClasses(getCallbacks());
        }
        else {
            return (Class[])callbacks.toArray(new Class[callbacks.size()]);
        }
    }

    @Override
    public int accept(Method method) {
        return ((Integer)methodMap.get(method)).intValue();
    }

    @Override
    public int hashCode() {
        return methodMap.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof CallbackHelper other)) {
            return false;
        }
        return this.methodMap.equals(other.methodMap);
    }

}
