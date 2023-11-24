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
package org.springframework.cglib.proxy;

import java.lang.reflect.Method;

import org.springframework.asm.ClassVisitor;
import org.springframework.cglib.core.ReflectUtils;

/**
 * @author Chris Nokleberg
 * @version $Id: MixinBeanEmitter.java,v 1.2 2004/06/24 21:15:20 herbyderby Exp $
 */
@SuppressWarnings({"rawtypes", "unchecked"})
class MixinBeanEmitter extends MixinEmitter {
    public MixinBeanEmitter(ClassVisitor v, String className, Class[] classes) {
        super(v, className, classes, null);
    }

    @Override
	protected Class[] getInterfaces(Class[] classes) {
        return null;
    }

    @Override
	protected Method[] getMethods(Class type) {
        return ReflectUtils.getPropertyMethods(ReflectUtils.getBeanProperties(type), true, true);
    }
}
