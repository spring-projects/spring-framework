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
 * Lazy-loading {@link Enhancer} callback.
 */
public interface LazyLoader extends Callback {
    /**
     * Return the object which the original method invocation should be
     * dispatched. Called as soon as the first lazily-loaded method in
     * the enhanced instance is invoked. The same object is then used
     * for every future method call to the proxy instance.
     * @return an object that can invoke the method
     */
    Object loadObject() throws Exception;
}
