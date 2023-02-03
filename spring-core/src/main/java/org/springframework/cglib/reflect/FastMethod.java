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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.asm.Type;
import org.springframework.cglib.core.Signature;

@SuppressWarnings({"rawtypes", "unchecked"})
public class FastMethod extends FastMember
{
    FastMethod(FastClass fc, Method method) {
        super(fc, method, helper(fc, method));
    }

    private static int helper(FastClass fc, Method method) {
        int index = fc.getIndex(new Signature(method.getName(), Type.getMethodDescriptor(method)));
        if (index < 0) {
            Class[] types = method.getParameterTypes();
            System.err.println("hash=" + method.getName().hashCode() + " size=" + types.length);
            for (int i = 0; i < types.length; i++) {
                System.err.println("  types[" + i + "]=" + types[i].getName());
            }
            throw new IllegalArgumentException("Cannot find method " + method);
        }
        return index;
    }

    public Class getReturnType() {
        return ((Method)member).getReturnType();
    }

    @Override
	public Class[] getParameterTypes() {
        return ((Method)member).getParameterTypes();
    }

    @Override
	public Class[] getExceptionTypes() {
        return ((Method)member).getExceptionTypes();
    }

    public Object invoke(Object obj, Object[] args) throws InvocationTargetException {
        return fc.invoke(index, obj, args);
    }

    public Method getJavaMethod() {
        return (Method)member;
    }
}
