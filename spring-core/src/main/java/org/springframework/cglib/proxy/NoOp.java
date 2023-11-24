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
 * Methods using this {@link Enhancer} callback will delegate directly to the
 * default (super) implementation in the base class.
 */
public interface NoOp extends Callback
{
    /**
     * A thread-safe singleton instance of the <code>NoOp</code> callback.
     */
    public static final NoOp INSTANCE = new NoOp() { };
}
