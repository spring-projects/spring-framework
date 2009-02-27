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
package org.springframework.config.java.process;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.config.java.annotation.Configuration;
import org.springframework.config.java.internal.process.InternalConfigurationPostProcessor;
import org.springframework.core.Ordered;


/**
 * {@link BeanFactoryPostProcessor} used for bootstrapping {@link Configuration @Configuration}
 * beans from Spring XML files.
 */
// TODO: This class now just delegates to InternalConfigurationPostProcessor.  Eliminate?
public class ConfigurationPostProcessor implements Ordered, BeanFactoryPostProcessor {

    /**
     * Iterates through <var>beanFactory</var>, detecting and processing any {@link Configuration}
     * bean definitions.
     */
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        new InternalConfigurationPostProcessor().postProcessBeanFactory(beanFactory);
    }

    /**
     * Returns the order in which this {@link BeanPostProcessor} will be executed.
     * Returns {@link Ordered#HIGHEST_PRECEDENCE}.
     */
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
