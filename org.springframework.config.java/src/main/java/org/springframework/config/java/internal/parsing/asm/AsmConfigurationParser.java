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
package org.springframework.config.java.internal.parsing.asm;



import org.objectweb.asm.ClassReader;
import org.springframework.config.java.annotation.Configuration;
import org.springframework.config.java.internal.parsing.ConfigurationParser;
import org.springframework.config.java.model.ConfigurationClass;
import org.springframework.config.java.model.ConfigurationModel;
import org.springframework.util.Assert;


/**
 * ASM-based implementation of {@link ConfigurationParser}. Avoids reflection and eager classloading
 * in order to interoperate effectively with tooling (Spring IDE).
 *
 * @see  org.springframework.config.java.model.AsmConfigurationParserTests

 * @author Chris Beams
 */
public class AsmConfigurationParser implements ConfigurationParser {

    /**
     * Model to be populated during calls to {@link #parse(Object)}
     */
    private final ConfigurationModel model;

    /**
     * Creates a new parser instance that will be used to populate <var>model</var>.
     *
     * @param model   model to be populated by each successive call to {@link #parse(Object)}
     */
    public AsmConfigurationParser(ConfigurationModel model) {
        this.model = model;
    }

    /**
     * Convenience implementation, delegates to {@link #parse(Object, String)},
     * passing in {@code null} for the configurationId.
     *
     * @param configurationSource   must be an ASM {@link ClassReader}
     */
    public void parse(Object configurationSource) {
        parse(configurationSource, null);
    }

    /**
     * Parse the {@link Configuration @Configuration} class encapsulated by
     * <var>configurationSource</var>.
     *
     * @param configurationSource must be an ASM {@link ClassReader}
     * @param configurationId may be null, but if populated represents the bean id
     * (assumes that this configuration class was configured via XML)
     */
    public void parse(Object configurationSource, String configurationId) {
        Assert.isInstanceOf(ClassReader.class, configurationSource,
                            "configurationSource must be an ASM ClassReader");

        ConfigurationClass configClass = new ConfigurationClass();
        configClass.setBeanName(configurationId);

        parse((ClassReader) configurationSource, configClass);
    }

    /**
     * Kicks off visiting <var>configClass</var> with {@link ConfigurationClassVisitor}
     */
    private void parse(ClassReader reader, ConfigurationClass configClass) {
        reader.accept(new ConfigurationClassVisitor(configClass, model), false);
        model.add(configClass);
    }

}
