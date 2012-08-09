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

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;

/**
 * An interface that represent a runtime bean configuration 
 * 
 * Credit must go to Solomon Duskis and the
 * article: http://jroller.com/page/Solomon?entry=programmatic_configuration_in_spring
 * 
 */

public interface BeanConfiguration {
    String AUTOWIRE_BY_TYPE = "byType";
    String AUTOWIRE_BY_NAME = "byName";

    /**
     *
     * @return The name of the bean
     */
    String getName();
	
	/**
	 * 
	 * @return True if the bean is singleton
	 */
	boolean isSingleton();
	
	/**
	 * 
	 * @return The Spring bean definition instance
	 */
	AbstractBeanDefinition getBeanDefinition();

	/**
	 * Adds a property value to this bean
	 * @param propertyName The name of the property
	 * @param propertyValue The value of the property
	 * 
	 * @return Returns this bean configuration
	 */
	BeanConfiguration addProperty(String propertyName, Object propertyValue);

	/**
	 * Sets the name of the method to call when destroying the bean
	 * 
	 * @param methodName The method name
	 * @return This bean configuration
	 */
	BeanConfiguration setDestroyMethod(String methodName);

	/**
	 * Sets the names of the beans this bean configuration depends on
	 * 
	 * @param dependsOn Bean names it depends on
	 * @return This bean configuration
	 */
	BeanConfiguration setDependsOn(String[] dependsOn);

	/**
	 * 
	 * @param beanName
	 * @return This BeanConfiguration
	 */
	BeanConfiguration setFactoryBean(String beanName);

	/**
	 * 
	 * @param methodName
	 * @return This BeanConfiguration
	 */
	BeanConfiguration setFactoryMethod(String methodName);

	/**
	 * Sets the autowire type, either "byType" or "byName"
	 * 
	 * @param type The type
	 * @return This BeanConfiguration
	 */
	BeanConfiguration setAutowire(String type);


    /**
     * Sets the name of the bean in the app ctx
     * @param beanName The bean name
     */
    void setName(String beanName);

    /**
     * Returns true if the bean config has the name property set
     * @param name The name of the property
     * @return True if it does have a property with the given name
     */
	boolean hasProperty(String name);

	/**
	 * Returns the value of the given property or throws a MissingPropertyException
	 * 
	 * @param name The name of the property
	 * @return The value of the property
	 */
	Object getPropertyValue(String name);

	/**
	 * Sets a property value on the bean configuration
	 * 
	 * @param property The name of the property
	 * @param newValue The value
	 */
	void setPropertyValue(String property, Object newValue);

    /**
     * Sets the BeanConfiguration as an Abstract bean definition
     * @param isAbstract Whether its abstract or not
     * @return This BeanConfiguration object
     */
    BeanConfiguration setAbstract(boolean isAbstract);

    /**
     * Sets the name of the parent bean
     *
      * @param name Either a string which is the name of the bean, a RuntimeBeanReference or a BeanConfiguration
     */
    void setParent(Object name);

    void setBeanDefinition(BeanDefinition definition);
}
