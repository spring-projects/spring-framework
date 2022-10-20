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
 * {@link Enhancer} callback that simply returns the value to return
 * from the proxied method. No information about what method
 * is being called is available to the callback, and the type of
 * the returned object must be compatible with the return type of
 * the proxied method. This makes this callback primarily useful
 * for forcing a particular method (through the use of a {@link CallbackFilter}
 * to return a fixed value with little overhead.
 */
public interface FixedValue extends Callback {
    /**
     * Return the object which the original method invocation should
     * return. This method is called for <b>every</b> method invocation.
     * @return an object matching the type of the return value for every
     * method this callback is mapped to
     */
    Object loadObject() throws Exception;
}
