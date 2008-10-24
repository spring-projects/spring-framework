/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.orm.hibernate3;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.engine.FilterDefinition;
import org.hibernate.type.TypeFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Convenient FactoryBean for defining Hibernate FilterDefinitions.
 * Exposes a corresponding Hibernate FilterDefinition object.
 *
 * <p>Typically defined as an inner bean within a LocalSessionFactoryBean
 * definition, as the list element for the "filterDefinitions" bean property.
 * For example:
 *
 * <pre>
 * &lt;bean id="sessionFactory" class="org.springframework.orm.hibernate3.LocalSessionFactoryBean"&gt;
 *   ...
 *   &lt;property name="filterDefinitions"&gt;
 *     &lt;list&gt;
 *       &lt;bean class="org.springframework.orm.hibernate3.FilterDefinitionFactoryBean"&gt;
 *         &lt;property name="filterName" value="myFilter"/&gt;
 *         &lt;property name="parameterTypes"&gt;
 *           &lt;props&gt;
 *             &lt;prop key="myParam"&gt;string&lt;/prop&gt;
 *             &lt;prop key="myOtherParam"&gt;long&lt;/prop&gt;
 *           &lt;/props&gt;
 *         &lt;/property&gt;
 *       &lt;/bean&gt;
 *     &lt;/list&gt;
 *   &lt;/property&gt;
 *   ...
 * &lt;/bean&gt;</pre>
 *
 * Alternatively, specify a bean id (or name) attribute for the inner bean,
 * instead of the "filterName" property.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see org.hibernate.engine.FilterDefinition
 * @see LocalSessionFactoryBean#setFilterDefinitions
 */
public class FilterDefinitionFactoryBean implements FactoryBean, BeanNameAware, InitializingBean {

	private String filterName;

	private Map parameterTypeMap = new HashMap();

	private String defaultFilterCondition;

	private FilterDefinition filterDefinition;


	/**
	 * Set the name of the filter.
	 */
	public void setFilterName(String filterName) {
		this.filterName = filterName;
	}

	/**
	 * Set the parameter types for the filter,
	 * with parameter names as keys and type names as values.
	 * @see org.hibernate.type.TypeFactory#heuristicType(String)
	 */
	public void setParameterTypes(Properties parameterTypes) {
		if (parameterTypes != null) {
			this.parameterTypeMap = new HashMap(parameterTypes.size());
			for (Enumeration names = parameterTypes.propertyNames(); names.hasMoreElements();) {
				String paramName = (String) names.nextElement();
				String typeName = parameterTypes.getProperty(paramName);
				this.parameterTypeMap.put(paramName, TypeFactory.heuristicType(typeName));
			}
		}
		else {
			this.parameterTypeMap = new HashMap();
		}
	}

	/**
	 * Specify a default filter condition for the filter, if any.
	 */
	public void setDefaultFilterCondition(String defaultFilterCondition) {
		this.defaultFilterCondition = defaultFilterCondition;
	}

	/**
	 * If no explicit filter name has been specified, the bean name of
	 * the FilterDefinitionFactoryBean will be used.
	 * @see #setFilterName
	 */
	public void setBeanName(String name) {
		if (this.filterName == null) {
			this.filterName = name;
		}
	}

	public void afterPropertiesSet() {
		this.filterDefinition =
				new FilterDefinition(this.filterName, this.defaultFilterCondition, this.parameterTypeMap);
	}


	public Object getObject() {
		return this.filterDefinition;
	}

	public Class getObjectType() {
		return FilterDefinition.class;
	}

	public boolean isSingleton() {
		return true;
	}

}
