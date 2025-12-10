/*
 * Copyright 2003 The Apache Software Foundation
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

@SuppressWarnings({"rawtypes", "unchecked"})
public class FastConstructor extends FastMember
{
    FastConstructor(FastClass fc, Constructor constructor) {
        super(fc, constructor, fc.getIndex(constructor.getParameterTypes()));
    }

    @Override
	public Class[] getParameterTypes() {
        return ((Constructor)member).getParameterTypes();
    }

    @Override
	public Class[] getExceptionTypes() {
        return ((Constructor)member).getExceptionTypes();
    }

    public Object newInstance() throws InvocationTargetException {
        return fc.newInstance(index, null);
    }

    public Object newInstance(Object[] args) throws InvocationTargetException {
        return fc.newInstance(index, args);
    }

    public Constructor getJavaConstructor() {
        return (Constructor)member;
    }
}
