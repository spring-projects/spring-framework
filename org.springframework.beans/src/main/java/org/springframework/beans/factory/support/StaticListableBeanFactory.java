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

package org.springframework.beans.factory.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanIsNotAFactoryException;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.util.StringUtils;

/**
 * Static {@link org.springframework.beans.factory.BeanFactory} implementation
 * which allows to register existing singleton instances programmatically.
 * Does not have support for prototype beans or aliases.
 *
 * <p>Serves as example for a simple implementation of the
 * {@link org.springframework.beans.factory.ListableBeanFactory} interface,
 * managing existing bean instances rather than creating new ones based on bean
 * definitions, and not implementing any extended SPI interfaces (such as
 * {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}).
 *
 * <p>For a full-fledged factory based on bean definitions, have a look
 * at {@link DefaultListableBeanFactory}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 06.01.2003
 * @see DefaultListableBeanFactory
 */
public class StaticListableBeanFactory implements ListableBeanFactory {

	/** Map from bean name to bean instance */
	private final Map beans = new HashMap();


	/**
	 * Add a new singleton bean.
	 * Will overwrite any existing instance for the given name.
	 * @param name the name of the bean
	 * @param bean the bean instance
	 */
	public void addBean(String name, Object bean) {
		this.beans.put(name, bean);
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	//---------------------------------------------------------------------

	public Object getBean(String name) throws BeansException {
		String beanName = BeanFactoryUtils.transformedBeanName(name);
		Object bean = this.beans.get(beanName);

		if (bean == null) {
			throw new NoSuchBeanDefinitionException(beanName,
					"Defined beans are [" + StringUtils.collectionToCommaDelimitedString(this.beans.keySet()) + "]");
		}

		// Don't let calling code try to dereference the
		// bean factory if the bean isn't a factory
		if (BeanFactoryUtils.isFactoryDereference(name) && !(bean instanceof FactoryBean)) {
			throw new BeanIsNotAFactoryException(beanName, bean.getClass());
		}

		if (bean instanceof FactoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
			try {
				return ((FactoryBean) bean).getObject();
			}
			catch (Exception ex) {
				throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
			}
		}
		return bean;
	}
	
	public Object getBean(String name, Class requiredType) throws BeansException {
		Object bean = getBean(name);
		if (requiredType != null && !requiredType.isAssignableFrom(bean.getClass())) {
			throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
		}
		return bean;
	}

	public Object getBean(String name, Object[] args) throws BeansException {
		if (args != null) {
			throw new UnsupportedOperationException(
					"StaticListableBeanFactory does not support explicit bean creation arguments)");
		}
		return getBean(name);
	}

	public boolean containsBean(String name) {
		return this.beans.containsKey(name);
	}

	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		Object bean = getBean(name);
		// In case of FactoryBean, return singleton status of created object.
		return (bean instanceof FactoryBean && ((FactoryBean) bean).isSingleton());
	}

	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		Object bean = getBean(name);
		// In case of FactoryBean, return prototype status of created object.
		return ((bean instanceof SmartFactoryBean && ((SmartFactoryBean) bean).isPrototype()) ||
				(bean instanceof FactoryBean && !((FactoryBean) bean).isSingleton()));
	}

	public boolean isTypeMatch(String name, Class targetType) throws NoSuchBeanDefinitionException {
		Class type = getType(name);
		return (targetType == null || (type != null && targetType.isAssignableFrom(type)));
	}

	public Class getType(String name) throws NoSuchBeanDefinitionException {
		String beanName = BeanFactoryUtils.transformedBeanName(name);

		Object bean = this.beans.get(beanName);
		if (bean == null) {
			throw new NoSuchBeanDefinitionException(beanName,
					"Defined beans are [" + StringUtils.collectionToCommaDelimitedString(this.beans.keySet()) + "]");
		}

		if (bean instanceof FactoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
			// If it's a FactoryBean, we want to look at what it creates, not the factory class.
			return ((FactoryBean) bean).getObjectType();
		}
		return bean.getClass();
	}

	public String[] getAliases(String name) {
		return new String[0];
	}


	//---------------------------------------------------------------------
	// Implementation of ListableBeanFactory interface
	//---------------------------------------------------------------------

	public boolean containsBeanDefinition(String name) {
		return this.beans.containsKey(name);
	}

	public int getBeanDefinitionCount() {
		return this.beans.size();
	}

	public String[] getBeanDefinitionNames() {
		return StringUtils.toStringArray(this.beans.keySet());
	}

	public String[] getBeanNamesForType(Class type) {
		return getBeanNamesForType(type, true, true);
	}

	public String[] getBeanNamesForType(Class type, boolean includeNonSingletons, boolean includeFactoryBeans) {
		boolean isFactoryType = (type != null && FactoryBean.class.isAssignableFrom(type));
		List matches = new ArrayList();
		Set keys = this.beans.keySet();
		Iterator it = keys.iterator();
		while (it.hasNext()) {
			String name = (String) it.next();
			Object beanInstance = this.beans.get(name);
			if (beanInstance instanceof FactoryBean && !isFactoryType) {
				if (includeFactoryBeans) {
					Class objectType = ((FactoryBean) beanInstance).getObjectType();
					if (objectType != null && type.isAssignableFrom(objectType)) {
						matches.add(name);
					}
				}
			}
			else {
				if (type.isInstance(beanInstance)) {
					matches.add(name);
				}
			}
		}
		return StringUtils.toStringArray(matches);
	}

	public Map getBeansOfType(Class type) throws BeansException {
		return getBeansOfType(type, true, true);
	}

	public Map getBeansOfType(Class type, boolean includeNonSingletons, boolean includeFactoryBeans)
			throws BeansException {

		boolean isFactoryType = (type != null && FactoryBean.class.isAssignableFrom(type));
		Map matches = new HashMap();

		Iterator it = this.beans.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			String beanName = (String) entry.getKey();
			Object beanInstance = entry.getValue();

			// Is bean a FactoryBean?
			if (beanInstance instanceof FactoryBean && !isFactoryType) {
				if (includeFactoryBeans) {
					// Match object created by FactoryBean.
					FactoryBean factory = (FactoryBean) beanInstance;
					Class objectType = factory.getObjectType();
					if ((includeNonSingletons || factory.isSingleton()) &&
							objectType != null && type.isAssignableFrom(objectType)) {
						matches.put(beanName, getBean(beanName));
					}
				}
			}
			else {
				if (type.isInstance(beanInstance)) {
					// If type to match is FactoryBean, return FactoryBean itself.
					// Else, return bean instance.
					if (isFactoryType) {
						beanName = FACTORY_BEAN_PREFIX + beanName;
					}
					matches.put(beanName, beanInstance);
				}
			}
		}
		return matches;
	}

}
