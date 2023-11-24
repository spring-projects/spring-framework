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
package org.springframework.cglib.core;

/**
 * @version $Id: CodeGenerationException.java,v 1.3 2004/06/24 21:15:21 herbyderby Exp $
 */
@SuppressWarnings("serial")
public class CodeGenerationException extends RuntimeException {
    private Throwable cause;

    public CodeGenerationException(Throwable cause) {
        super(cause.getClass().getName() + "-->" + cause.getMessage());
        this.cause = cause;
    }

    @Override
	public Throwable getCause() {
        return cause;
    }
}
