/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.groovy;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import groovy.lang.GroovyObjectSupport;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Internal wrapper for a Spring BeanDefinition, allowing for Groovy-style
 * property access within a {@link GroovyBeanDefinitionReader} closure.
 *
 * @author Jeff Brown
 * @author Juergen Hoeller
 * @since 4.0
 */
class GroovyBeanDefinitionWrapper extends GroovyObjectSupport {

	private static final String PARENT = "parent";
	private static final String AUTOWIRE = "autowire";
	private static final String CONSTRUCTOR_ARGS = "constructorArgs";
	private static final String FACTORY_BEAN = "factoryBean";
	private static final String FACTORY_METHOD = "factoryMethod";
	private static final String INIT_METHOD = "initMethod";
	private static final String DESTROY_METHOD = "destroyMethod";
	private static final String SINGLETON = "singleton";

	private static final Set<String> dynamicProperties = Set.of(PARENT, AUTOWIRE, CONSTRUCTOR_ARGS,
			FACTORY_BEAN, FACTORY_METHOD, INIT_METHOD, DESTROY_METHOD, SINGLETON);


	@Nullable
	private String beanName;

	@Nullable
	private final Class<?> clazz;

	@Nullable
	private final Collection<?> constructorArgs;

	@Nullable
	private AbstractBeanDefinition definition;

	@Nullable
	private BeanWrapper definitionWrapper;

	@Nullable
	private String parentName;


	GroovyBeanDefinitionWrapper(String beanName) {
		this(beanName, null);
	}

	GroovyBeanDefinitionWrapper(@Nullable String beanName, @Nullable Class<?> clazz) {
		this(beanName, clazz, null);
	}

	GroovyBeanDefinitionWrapper(@Nullable String beanName, @Nullable Class<?> clazz, @Nullable Collection<?> constructorArgs) {
		this.beanName = beanName;
		this.clazz = clazz;
		this.constructorArgs = constructorArgs;
	}


	@Nullable
	public String getBeanName() {
		return this.beanName;
	}

	void setBeanDefinition(AbstractBeanDefinition definition) {
		this.definition = definition;
	}

	AbstractBeanDefinition getBeanDefinition() {
		if (this.definition == null) {
			this.definition = createBeanDefinition();
		}
		return this.definition;
	}

	protected AbstractBeanDefinition createBeanDefinition() {
		AbstractBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClass(this.clazz);
		if (!CollectionUtils.isEmpty(this.constructorArgs)) {
			ConstructorArgumentValues cav = new ConstructorArgumentValues();
			for (Object constructorArg : this.constructorArgs) {
				cav.addGenericArgumentValue(constructorArg);
			}
			bd.setConstructorArgumentValues(cav);
		}
		if (this.parentName != null) {
			bd.setParentName(this.parentName);
		}
		this.definitionWrapper = new BeanWrapperImpl(bd);
		return bd;
	}

	void setBeanDefinitionHolder(BeanDefinitionHolder holder) {
		this.definition = (AbstractBeanDefinition) holder.getBeanDefinition();
		this.beanName = holder.getBeanName();
	}

	BeanDefinitionHolder getBeanDefinitionHolder() {
		Assert.state(this.beanName != null, "Bean name must be set");
		return new BeanDefinitionHolder(getBeanDefinition(), this.beanName);
	}

	void setParent(@Nullable Object obj) {
		Assert.notNull(obj, "Parent bean cannot be set to a null runtime bean reference");
		if (obj instanceof String name) {
			this.parentName = name;
		}
		else if (obj instanceof RuntimeBeanReference runtimeBeanReference) {
			this.parentName = runtimeBeanReference.getBeanName();
		}
		else if (obj instanceof GroovyBeanDefinitionWrapper wrapper) {
			this.parentName = wrapper.getBeanName();
		}
		getBeanDefinition().setParentName(this.parentName);
		getBeanDefinition().setAbstract(false);
	}

	GroovyBeanDefinitionWrapper addProperty(String propertyName, @Nullable Object propertyValue) {
		if (propertyValue instanceof GroovyBeanDefinitionWrapper wrapper) {
			propertyValue = wrapper.getBeanDefinition();
		}
		getBeanDefinition().getPropertyValues().add(propertyName, propertyValue);
		return this;
	}


	@Override
	@Nullable
	public Object getProperty(String property) {
		Assert.state(this.definitionWrapper != null, "BeanDefinition wrapper not initialized");
		if (this.definitionWrapper.isReadableProperty(property)) {
			return this.definitionWrapper.getPropertyValue(property);
		}
		else if (dynamicProperties.contains(property)) {
			return null;
		}
		return super.getProperty(property);
	}

	@Override
	public void setProperty(String property, @Nullable Object newValue) {
		if (PARENT.equals(property)) {
			setParent(newValue);
		}
		else {
			AbstractBeanDefinition bd = getBeanDefinition();
			Assert.state(this.definitionWrapper != null, "BeanDefinition wrapper not initialized");
			if (AUTOWIRE.equals(property)) {
				if ("byName".equals(newValue)) {
					bd.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_NAME);
				}
				else if ("byType".equals(newValue)) {
					bd.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
				}
				else if ("constructor".equals(newValue)) {
					bd.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
				}
				else if (Boolean.TRUE.equals(newValue)) {
					bd.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_NAME);
				}
			}
			// constructorArgs
			else if (CONSTRUCTOR_ARGS.equals(property) && newValue instanceof List<?> args) {
				ConstructorArgumentValues cav = new ConstructorArgumentValues();
				for (Object arg : args) {
					cav.addGenericArgumentValue(arg);
				}
				bd.setConstructorArgumentValues(cav);
			}
			// factoryBean
			else if (FACTORY_BEAN.equals(property)) {
				if (newValue != null) {
					bd.setFactoryBeanName(newValue.toString());
				}
			}
			// factoryMethod
			else if (FACTORY_METHOD.equals(property)) {
				if (newValue != null) {
					bd.setFactoryMethodName(newValue.toString());
				}
			}
			// initMethod
			else if (INIT_METHOD.equals(property)) {
				if (newValue != null) {
					bd.setInitMethodName(newValue.toString());
				}
			}
			// destroyMethod
			else if (DESTROY_METHOD.equals(property)) {
				if (newValue != null) {
					bd.setDestroyMethodName(newValue.toString());
				}
			}
			// singleton property
			else if (SINGLETON.equals(property)) {
				bd.setScope(Boolean.TRUE.equals(newValue) ?
						BeanDefinition.SCOPE_SINGLETON : BeanDefinition.SCOPE_PROTOTYPE);
			}
			else if (this.definitionWrapper.isWritableProperty(property)) {
				this.definitionWrapper.setPropertyValue(property, newValue);
			}
			else {
				super.setProperty(property, newValue);
			}
		}
	}

}
