/*
 * Copyright 2010 the original author or authors.
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
package org.springframework.context.groovy;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Collection;
import java.util.List;

/**
 * A programmable runtime Spring configuration that allows a spring ApplicationContext
 * to be constructed at runtime
 *
 * Credit must go to Solomon Duskis and the
 * article: http://jroller.com/page/Solomon?entry=programmatic_configuration_in_spring
 *
 */
public interface RuntimeSpringConfiguration {

    /**
     * Adds a singleton bean definition
     *
     * @param name The name of the bean
     * @param clazz The class of the bean
     * @return A BeanConfiguration instance
     */
    public BeanConfiguration addSingletonBean(String name, Class clazz);
    
    public ApplicationContext getUnrefreshedApplicationContext();
    /**
     * Adds a prototype bean definition
     *
     * @param name The name of the bean
     * @param clazz The class of the bean
     * @return A BeanConfiguration instance
     */
    public BeanConfiguration addPrototypeBean(String name, Class clazz);

    /**
     * Retrieves the application context from the current state
     *
     * @return The ApplicationContext instance
     */
    ApplicationContext getApplicationContext();

    /**
     * Adds an empty singleton bean configuration
     * @param name The name of the singleton bean
     *
     * @return A BeanConfiguration instance
     */
    public BeanConfiguration addSingletonBean(String name);

    /**
     * Adds an empty prototype bean configuration
     *
     * @param name The name of the prototype bean
     * @return A BeanConfiguration instance
     */
    public BeanConfiguration addPrototypeBean(String name);

    /**
     * Creates a singleton bean configuration. Differs from addSingletonBean in that
     * it doesn't add the bean to the list of bean references. Hence should be used for
     * creating nested beans
     *
     * @param clazz
     * @return A BeanConfiguration instance
     */
    public BeanConfiguration createSingletonBean(Class clazz);

    /**
     * Creates a new singleton bean and adds it to the list of bean references
     *
     * @param name The name of the bean
     * @param clazz The class of the bean
     * @param args The constructor arguments of the bean
     * @return A BeanConfiguration instance
     */
    public BeanConfiguration addSingletonBean(String name, Class clazz, Collection args);

    /**
     * Creates a singleton bean configuration. Differs from addSingletonBean in that
     * it doesn't add the bean to the list of bean references. Hence should be used for
     * creating nested beans
     *
     * @param clazz The bean class
     * @param constructorArguments The constructor arguments
     * @return A BeanConfiguration instance
     */
    public BeanConfiguration createSingletonBean(Class clazz, Collection constructorArguments);


    /**
     * Creates a new prototype bean configuration. Differs from addPrototypeBean in that
     * it doesn't add the bean to the list of bean references to be created via the getApplicationContext()
     * method, hence can be used for creating nested beans
     *
     * @param name The bean name
     * @return A BeanConfiguration instance
     *
     */
    public BeanConfiguration createPrototypeBean(String name);

    /**
     * Creates a new singleton bean configuration. Differs from addSingletonBean in that
     * it doesn't add the bean to the list of bean references to be created via the getApplicationContext()
     * method, hence can be used for creating nested beans
     *
     * @param name The bean name
     * @return A BeanConfiguration instance
     *
     */
    public BeanConfiguration createSingletonBean(String name);

    /**
     * Adds a bean configuration to the list of beans to be created
     *
     * @param beanName The name of the bean in the context
     * @param beanConfiguration The BeanConfiguration instance
     */
    void addBeanConfiguration(String beanName, BeanConfiguration beanConfiguration);
    /**
     * Adds a Spring BeanDefinition. Differs from BeanConfiguration which is a factory class
     * for creating BeanDefinition instances
     * @param name The name of the bean
     * @param bd The BeanDefinition instance
     */
    public void addBeanDefinition(String name, BeanDefinition bd);

    /**
     * Returns whether the runtime spring config contains the specified bean
     *
     * @param name The bean name
     * @return True if it does
     */
    public boolean containsBean(String name);
    /**
     * Returns the BeanConfiguration for the specified name
     * @param name The name of the bean configuration
     * @return The BeanConfiguration
     */
    public BeanConfiguration getBeanConfig(String name);

    /**
     * Creates and returns the BeanDefinition that is regsitered within the given name or returns null
     *
     * @param name The name of the bean definition
     * @return A BeanDefinition
     */
    public AbstractBeanDefinition createBeanDefinition(String name);

    /**
     * Registers a bean factory post processor with the context
     *
     * @param processor The BeanFactoryPostProcessor instance
     */
    public void registerPostProcessor(BeanFactoryPostProcessor processor);

    List getBeanNames();

    /**
     * Registers the beans held within this RuntimeSpringConfiguration instance with the given ApplicationContext
     *
     * @param applicationContext The ApplicationContext instance
     */
    void registerBeansWithContext(GenericApplicationContext applicationContext);


    /**
     * Registers the beans held within this RuntimeSpringConfiguration instance with the given BeanDefinitionRegistry 
     *
     * @param registry The BeanDefinitionRegistry  instance
     */
    void registerBeansWithRegistry(BeanDefinitionRegistry registry);

    /**
     * Registers the beans held within this RuntimeSpringConfiguration instance with the given RuntimeSpringConfiguration
     *
     * @param targetSpringConfig The RuntimeSpringConfiguration  instance
     */
    void registerBeansWithConfig(RuntimeSpringConfiguration targetSpringConfig);

    /**
     * Adds an abstract bean definition to the bean factory and returns the BeanConfiguration object
     *
     * @param name The name of the bean
     * @return The BeanConfiguration object
     */
    BeanConfiguration addAbstractBean(String name);

    /**
     * Adds an alias to a given bean name
     *
     * @param alias The alias
     * @param beanName The bean
     */
    void addAlias(String alias, String beanName);

    /**
     * Obtains a BeanDefinition instance for the given beanName
     *
     * @param beanName The beanName
     * @return The BeanDefinition or null if it doesn't exit
     */
    BeanDefinition getBeanDefinition(String beanName);

    /**
     * Sets the BeanFactory implementation to use
     * @param beanFactory The BeanFactory implementation
     */
    void setBeanFactory(ListableBeanFactory beanFactory);
}
