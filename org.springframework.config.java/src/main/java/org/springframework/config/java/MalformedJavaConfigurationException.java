/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.config.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * TODO: rename to UsageException / move outside .internal?
 *
 * @author  Chris Beams
 */
@SuppressWarnings("serial")
public class MalformedJavaConfigurationException extends RuntimeException {

    private final List<? extends UsageError> errors;

    public MalformedJavaConfigurationException(String message) {
        super(message);
        this.errors = new ArrayList<UsageError>();
    }

    public MalformedJavaConfigurationException(UsageError... errors) {
        super(toString(errors));
        this.errors = Arrays.asList(errors);
    }

    public boolean containsError(Class<? extends UsageError> errorType) {
        for (UsageError error : errors)
            if (error.getClass().isAssignableFrom(errorType))
                return true;

        return false;
    }

    /**
     * Render a list of syntax errors as output suitable for diagnosis via System.err.
     */
    private static String toString(UsageError... errors) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n");

        if (errors.length == 1)
            sb.append("A usage error has ");
        else
            sb.append(errors.length + " usage errors have ");

        sb.append("been detected:\n");

        for (int i = 0; i < errors.length; i++) {
            sb.append(errors[i].toString());
            if ((i + 1) < errors.length)
                sb.append('\n');
        }

        return sb.toString();
    }

}
