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
package org.springframework.config.java.internal.parsing;

import org.springframework.config.java.annotation.Configuration;
import org.springframework.config.java.model.ConfigurationModel;


/**
 * Parses a {@link Configuration} class definition, usually into a {@link ConfigurationModel}.
 * <p>
 * This interface aids in separating the process of reading a class file (via reflection, ASM, etc)
 * from the process of registering bean definitions based on the content of that class.
 * 
 * @see org.springframework.config.java.internal.parsing.asm.AsmConfigurationParser
 * @see org.springframework.config.java.model.ConfigurationModel
 * @see org.springframework.config.java.internal.factory.support.ConfigurationModelBeanDefinitionReader
 * 
 * @author Chris Beams
 */ 
public interface ConfigurationParser {

    /**
     * Parse the Configuration object represented by <var>configurationSource.</var>
     *
     * @param  configurationSource  representation of a Configuration class, may be java.lang.Class,
     *                              ASM representation or otherwise
     *
     * @see    org.springframework.config.java.annotation.Configuration
     */
    void parse(Object configurationSource);

    /**
     * Optionally propagate a custom name for this <var>configurationSource</var>. Usually this id
     * corresponds to the name of a Configuration bean as declared in a beans XML.
     *
     * @param  configurationSource  representation of a Configuration class, may be java.lang.Class,
     *                              ASM representation or otherwise
     * @param  configurationId      name of this configuration class, probably corresponding to a
     *                              bean id
     *
     * @see    org.springframework.config.java.annotation.Configuration
     * @see    org.springframework.config.java.process.ConfigurationPostProcessor
     */
    void parse(Object configurationSource, String configurationId);

}
