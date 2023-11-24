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
package org.springframework.cglib.transform.impl;

import java.lang.reflect.Method;

import org.springframework.asm.Type;
import org.springframework.cglib.core.CodeEmitter;
import org.springframework.cglib.core.Constants;
import org.springframework.cglib.core.EmitUtils;
import org.springframework.cglib.core.MethodInfo;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.cglib.core.TypeUtils;
import org.springframework.cglib.transform.ClassEmitterTransformer;

/**
 * @author Juozas Baliuka, Chris Nokleberg
 */
public class AddStaticInitTransformer extends ClassEmitterTransformer {
    private MethodInfo info;

    public AddStaticInitTransformer(Method classInit) {
        info = ReflectUtils.getMethodInfo(classInit);
        if (!TypeUtils.isStatic(info.getModifiers())) {
            throw new IllegalArgumentException(classInit + " is not static");
        }
        Type[] types = info.getSignature().getArgumentTypes();
        if (types.length != 1 ||
            !types[0].equals(Constants.TYPE_CLASS) ||
            !info.getSignature().getReturnType().equals(Type.VOID_TYPE)) {
            throw new IllegalArgumentException(classInit + " illegal signature");
        }
    }

    @Override
	protected void init() {
        if (!TypeUtils.isInterface(getAccess())) {
            CodeEmitter e = getStaticHook();
            EmitUtils.load_class_this(e);
            e.invoke(info);
        }
    }
}
