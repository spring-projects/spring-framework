/*
 * Copyright 2002-2005 the original author or authors.
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

import java.util.Properties;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;

/**
 * Bean that encapsulates a Hibernate type definition.
 *
 * <p>Typically defined as inner bean within a LocalSessionFactoryBean
 * definition, as list element for the "typeDefinitions" bean property.
 * For example:
 *
 * <pre>
 * &lt;bean id="sessionFactory" class="org.springframework.orm.hibernate3.LocalSessionFactoryBean"&gt;
 *   ...
 *   &lt;property name="typeDefinitions"&gt;
 *     &lt;list&gt;
 *       &lt;bean class="org.springframework.orm.hibernate3.TypeDefinitionBean"&gt;
 *         &lt;property name="typeName" value="myType"/&gt;
 *         &lt;property name="typeClass" value="mypackage.MyTypeClass"/&gt;
 *       &lt;/bean&gt;
 *     &lt;/list&gt;
 *   &lt;/property&gt;
 *   ...
 * &lt;/bean&gt;</pre>
 *
 * Alternatively, specify a bean id (or name) attribute for the inner bean,
 * instead of the "typeName" property.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see LocalSessionFactoryBean#setTypeDefinitions(TypeDefinitionBean[])
 */
public class TypeDefinitionBean implements BeanNameAware, InitializingBean {

	private String typeName;

	private String typeClass;

	private Properties parameters = new Properties();


	/**
	 * Set the name of the type.
	 * @see org.hibernate.cfg.Mappings#addTypeDef(String, String, java.util.Properties)
	 */
	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	/**
	 * Return the name of the type.
	 */
	public String getTypeName() {
		return typeName;
	}

	/**
	 * Set the type implementation class.
	 * @see org.hibernate.cfg.Mappings#addTypeDef(String, String, java.util.Properties)
	 */
	public void setTypeClass(String typeClass) {
		this.typeClass = typeClass;
	}

	/**
	 * Return the type implementation class.
	 */
	public String getTypeClass() {
		return typeClass;
	}

	/**
	 * Specify default parameters for the type.
	 * This only applies to parameterized types.
	 * @see org.hibernate.cfg.Mappings#addTypeDef(String, String, java.util.Properties)
	 * @see org.hibernate.usertype.ParameterizedType
	 */
	public void setParameters(Properties parameters) {
		this.parameters = parameters;
	}

	/**
	 * Return the default parameters for the type.
	 */
	public Properties getParameters() {
		return parameters;
	}


	/**
	 * If no explicit type name has been specified, the bean name of
	 * the TypeDefinitionBean will be used.
	 * @see #setTypeName
	 */
	@Override
	public void setBeanName(String name) {
		if (this.typeName == null) {
			this.typeName = name;
		}
	}

	@Override
	public void afterPropertiesSet() {
		if (this.typeName == null) {
			throw new IllegalArgumentException("typeName is required");
		}
		if (this.typeClass == null) {
			throw new IllegalArgumentException("typeClass is required");
		}
	}

}
