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
package org.springframework.cglib.beans;

import org.springframework.cglib.core.CodeGenerationException;

@SuppressWarnings({"rawtypes", "unchecked", "serial"})
public class BulkBeanException extends RuntimeException
{
    private int index;
    private Throwable cause;
    
    public BulkBeanException(String message, int index) {
        super(message);
        this.index = index;
    }

    public BulkBeanException(Throwable cause, int index) {
        super(cause.getMessage());
        this.index = index;
        this.cause = cause;
    }

    public int getIndex() {
        return index;
    }
    
    public Throwable getCause() {
        return cause;
    }
}
