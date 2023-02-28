/*
 * Copyright 2002,2003 The Apache Software Foundation
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

import org.springframework.cglib.core.CodeGenerationException;

/**
 * Used by {@link Proxy} as a replacement for <code>java.lang.reflect.UndeclaredThrowableException</code>.
 * @author Juozas Baliuka
 */
@SuppressWarnings("serial")
public class UndeclaredThrowableException extends CodeGenerationException {
    /**
     * Creates a new instance of <code>UndeclaredThrowableException</code> without detail message.
     */
    public UndeclaredThrowableException(Throwable t) {
        super(t);
    }

    public Throwable getUndeclaredThrowable() {
        return getCause();
    }
}
