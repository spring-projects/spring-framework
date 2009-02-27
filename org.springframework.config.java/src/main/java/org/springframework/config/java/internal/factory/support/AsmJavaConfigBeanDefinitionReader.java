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
package org.springframework.config.java.internal.factory.support;

import static org.springframework.config.java.Util.*;

import java.util.ArrayList;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.config.java.annotation.Configuration;
import org.springframework.config.java.internal.parsing.ConfigurationParser;
import org.springframework.config.java.internal.parsing.asm.AsmConfigurationParser;
import org.springframework.config.java.internal.parsing.asm.AsmUtils;
import org.springframework.config.java.model.ConfigurationModel;
import org.springframework.config.java.model.MalformedJavaConfigurationException;
import org.springframework.config.java.model.UsageError;
import org.springframework.core.io.ClassPathResource;


/**
 * Uses ASM to parse {@link Configuration @Configuration} classes. Fashioned after the
 * {@link BeanDefinitionReader} hierarchy, but does not extend or implement any of its
 * types because differences were significant enough to merit the departure.
 *
 * @see     AsmConfigurationParser
 * 
 * @author  Chris Beams
 */
public class AsmJavaConfigBeanDefinitionReader {

    private final ConfigurationModelBeanDefinitionReader modelBeanDefinitionReader;

    /**
     * Creates a new {@link AsmJavaConfigBeanDefinitionReader}.
     * 
     * @param registry {@link BeanDefinitionRegistry} into which new bean definitions will be
     *            registered as they are read from Configuration classes.
     */
    public AsmJavaConfigBeanDefinitionReader(DefaultListableBeanFactory beanFactory) {
        this.modelBeanDefinitionReader = new ConfigurationModelBeanDefinitionReader(beanFactory);
    }

    /**
     * Parses each {@link Configuration} class specified by <var>configClassResources</var> and registers
     * individual bean definitions from those Configuration classes into the BeanDefinitionRegistry
     * supplied during construction.
     */
    public int loadBeanDefinitions(ConfigurationModel model, Map<String, ClassPathResource> configClassResources) throws BeanDefinitionStoreException {
        ConfigurationParser parser = new AsmConfigurationParser(model);
        
        for (String id : configClassResources.keySet()) {
            String resourcePath = configClassResources.get(id).getPath();
            ClassReader configClassReader = AsmUtils.newClassReader(getClassAsStream(resourcePath));
            parser.parse(configClassReader, id);
        }

        ArrayList<UsageError> errors = new ArrayList<UsageError>();
        model.validate(errors);
        if (errors.size() > 0)
            throw new MalformedJavaConfigurationException(errors.toArray(new UsageError[] { }));

        return modelBeanDefinitionReader.loadBeanDefinitions(model);
    }

}
