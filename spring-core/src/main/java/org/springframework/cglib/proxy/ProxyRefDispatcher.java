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

/**
 * Dispatching {@link Enhancer} callback. This is the same as the
 * {@link Dispatcher} except for the addition of an argument
 * which references the proxy object.
 */
public interface ProxyRefDispatcher extends Callback {
    /**
     * Return the object which the original method invocation should
     * be dispatched. This method is called for <b>every</b> method invocation.
     * @param proxy a reference to the proxy (generated) object
     * @return an object that can invoke the method
     */
    Object loadObject(Object proxy) throws Exception;
}
