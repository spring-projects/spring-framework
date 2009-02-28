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



import org.objectweb.asm.ClassReader;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.config.java.Configuration;
import org.springframework.config.java.ConfigurationClass;
import org.springframework.config.java.ConfigurationModel;
import org.springframework.config.java.Util;
import org.springframework.core.io.ClassPathResource;


/**
 * Parses a {@link Configuration} class definition, populating a {@link ConfigurationModel}.
 * This ASM-based implementation avoids reflection and eager classloading in order to
 * interoperate effectively with tooling (Spring IDE) and OSGi environments.
 * <p>
 * This class helps separate the concern of parsing the structure of a Configuration class 
 * from the concern of registering {@link BeanDefinition} objects based on the content of
 * that model.
 * 
 * @see org.springframework.config.java.ConfigurationModel
 * @see org.springframework.config.java.internal.factory.support.ConfigurationModelBeanDefinitionReader
 *
 * @author Chris Beams
 */
public class ConfigurationParser {

    /**
     * Model to be populated during calls to {@link #parse(Object, String)}
     */
    private final ConfigurationModel model;

    /**
     * Creates a new parser instance that will be used to populate <var>model</var>.
     *
     * @param model   model to be populated by each successive call to {@link #parse(Object, String)}
     */
    public ConfigurationParser(ConfigurationModel model) {
        this.model = model;
    }

    /**
     * Parse the {@link Configuration @Configuration} class encapsulated by
     * <var>configurationSource</var>.
     *
     * @param configurationSource reader for Configuration class being parsed
     * @param configurationId may be null, but if populated represents the bean id
     * (assumes that this configuration class was configured via XML)
     */
    public void parse(ClassPathResource resource, String configurationId) {
    	
        String resourcePath = resource.getPath();
        ClassReader configClassReader = AsmUtils.newClassReader(Util.getClassAsStream(resourcePath));
            
        ConfigurationClass configClass = new ConfigurationClass();
        configClass.setBeanName(configurationId);

        configClassReader.accept(new ConfigurationClassVisitor(configClass, model), false);
		model.add(configClass);
    }

}
