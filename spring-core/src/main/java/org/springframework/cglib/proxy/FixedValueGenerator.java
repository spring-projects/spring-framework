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

import java.util.Iterator;
import java.util.List;

import org.springframework.asm.Type;
import org.springframework.cglib.core.ClassEmitter;
import org.springframework.cglib.core.CodeEmitter;
import org.springframework.cglib.core.MethodInfo;
import org.springframework.cglib.core.Signature;
import org.springframework.cglib.core.TypeUtils;

@SuppressWarnings({"rawtypes", "unchecked"})
class FixedValueGenerator implements CallbackGenerator {
    public static final FixedValueGenerator INSTANCE = new FixedValueGenerator();
    private static final Type FIXED_VALUE =
      TypeUtils.parseType("org.springframework.cglib.proxy.FixedValue");
    private static final Signature LOAD_OBJECT =
      TypeUtils.parseSignature("Object loadObject()");

    @Override
	public void generate(ClassEmitter ce, Context context, List methods) {
        for (Iterator it = methods.iterator(); it.hasNext();) {
            MethodInfo method = (MethodInfo)it.next();
            CodeEmitter e = context.beginMethod(ce, method);
            context.emitCallback(e, context.getIndex(method));
            e.invoke_interface(FIXED_VALUE, LOAD_OBJECT);
            e.unbox_or_zero(e.getReturnType());
            e.return_value();
            e.end_method();
        }
    }

    @Override
	public void generateStatic(CodeEmitter e, Context context, List methods) { }
}
