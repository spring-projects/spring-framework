
/*
 * Copyright 2002-2014 the original author or authors.
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
package org.springframework.jdbc.core.metadata;

import org.springframework.jdbc.core.SqlParameter;

/**
 * Name-specific class to manage context metadata used for the configuration and execution of the call.
 * Implementing binding like: {call some_package.some_procedure(p_parameter_1 => ?, p_parameter_2 => ?, p_parameter_3 =>?)}
 *
 * @author Kiril Nugmanov
 */
public class NamedCallMetaDataContext extends CallMetaDataContext {

    @Override
    protected String constructParameterBinding(SqlParameter parameter) {
        return parameter.getName() + " => ?";
    }
}
