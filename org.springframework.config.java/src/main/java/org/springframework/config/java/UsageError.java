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



/**
 * Represents an invalid usage of JavaConfig constructs, e.g. a {@link Configuration} that declares
 * no {@link Bean @Bean} methods, or declaring both {@link Bean @Bean} and
 * {@link ExternalBean @ExternalBean} on a single method. Explore the type hierarchy to discover all
 * possible usage errors.
 *
 * @author  Chris Beams
 * @see     MalformedJavaConfigurationException
 */
public abstract class UsageError {

    private final ModelClass clazz;
    private final int lineNumber;

    /**
     * Create a new usage error, providing information about where the error was detected.
     *
     * @param  modelClass  class in which this error was detected. Null value indicates that the
     *                     error was not local to a single class.
     * @param lineNumber   line number on which this error was detected (useful for tooling integration)
     * 
     * @see ModelClass#getSource()
     */
    public UsageError(ModelClass modelClass, int lineNumber) {
        this.clazz = modelClass;
        this.lineNumber = lineNumber;
    }

    /**
     * Human-readable description of this error suitable for console output or IDE tooling.
     */
    public abstract String getDescription();

    /**
     * Same as {@link #getDescription()} but attributed with class and line number information. If
     * modelClass constructor parameter was null, class and line number information will be omitted.
     */
    public final String getAttributedDescription() {
        if (clazz == null)
            return getDescription();

        return String.format("%s:%d: %s", clazz.getSource(), lineNumber, getDescription());
    }

    /**
     * Delegates directly to {@link #getAttributedDescription()}.
     */
    @Override
    public String toString() {
        return getAttributedDescription();
    }

}
