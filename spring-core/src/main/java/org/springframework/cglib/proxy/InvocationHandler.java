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

/**
 * {@link java.lang.reflect.InvocationHandler} replacement (unavailable under JDK 1.2).
 * This callback type is primarily for use by the {@link Proxy} class but
 * may be used with {@link Enhancer} as well.
 * @author Neeme Praks <a href="mailto:neeme@apache.org">neeme@apache.org</a>
 * @version $Id: InvocationHandler.java,v 1.3 2004/06/24 21:15:20 herbyderby Exp $
 */
public interface InvocationHandler
extends Callback
{
    /**
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable;

}
