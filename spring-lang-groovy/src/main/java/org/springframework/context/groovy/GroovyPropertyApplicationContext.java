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

import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.UiApplicationContextUtils;

/**
 * An ApplicationContext that extends StaticApplicationContext and implements GroovyObject such that beans can be retrieved with the dot
 * de-reference syntax instead of using getBean('name')
 *
 */
public class GroovyPropertyApplicationContext extends GenericApplicationContext implements GroovyObject {
    protected MetaClass metaClass;
    private BeanWrapper ctxBean = new BeanWrapperImpl(this);
    private ThemeSource themeSource;

    public GroovyPropertyApplicationContext(DefaultListableBeanFactory defaultListableBeanFactory) {
        super(defaultListableBeanFactory);
        this.metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(getClass());
    }

    public GroovyPropertyApplicationContext(DefaultListableBeanFactory defaultListableBeanFactory, ApplicationContext applicationContext) {
        super(defaultListableBeanFactory, applicationContext);
        this.metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(getClass());
    }

    public GroovyPropertyApplicationContext(org.springframework.context.ApplicationContext parent) throws org.springframework.beans.BeansException {
        super(parent);
        this.metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(getClass());
    }

    public GroovyPropertyApplicationContext() throws org.springframework.beans.BeansException {
        this.metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(getClass());
    }

    public MetaClass getMetaClass() {
		return this.metaClass;
	}

    public Object getProperty(String property) {
		if(containsBean(property)) {
			return getBean(property);
		}
		else if(ctxBean.isReadableProperty(property)) {
			return ctxBean.getPropertyValue(property);
		}
		return null;
	}

    public Object invokeMethod(String name, Object args) {
		return metaClass.invokeMethod(this, name, args);
	}

    public void setMetaClass(MetaClass metaClass) {
		this.metaClass = metaClass;
	}

    /**
	 * Initialize the theme capability.
     */
    protected void onRefresh() {
        this.themeSource = UiApplicationContextUtils.initThemeSource(this);
    }

    public Theme getTheme(String themeName) {
		return this.themeSource.getTheme(themeName);
	}

    public void setProperty(String property, Object newValue) {
		if(newValue instanceof BeanDefinition) {
            if(containsBean(property)) {
                removeBeanDefinition(property);
            }

            registerBeanDefinition(property, (BeanDefinition)newValue);
		}
		else {
			metaClass.setProperty(this, property, newValue);
		}
	}


    /**
     * Register a singleton bean with the underlying bean factory.
     * <p>For more advanced needs, register with the underlying BeanFactory directly.
     * @see #getDefaultListableBeanFactory
     */
    public void registerSingleton(String name, Class clazz) throws BeansException {
        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setBeanClass(clazz);
        getDefaultListableBeanFactory().registerBeanDefinition(name, bd);
    }

    /**
     * Register a singleton bean with the underlying bean factory.
     * <p>For more advanced needs, register with the underlying BeanFactory directly.
     * @see #getDefaultListableBeanFactory
     */
    public void registerSingleton(String name, Class clazz, MutablePropertyValues pvs) throws BeansException {
        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setBeanClass(clazz);
        bd.setPropertyValues(pvs);
        getDefaultListableBeanFactory().registerBeanDefinition(name, bd);
    }

    /**
     * Register a prototype bean with the underlying bean factory.
     * <p>For more advanced needs, register with the underlying BeanFactory directly.
     * @see #getDefaultListableBeanFactory
     */
    public void registerPrototype(String name, Class clazz) throws BeansException {
        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setScope(GenericBeanDefinition.SCOPE_PROTOTYPE);
        bd.setBeanClass(clazz);
        getDefaultListableBeanFactory().registerBeanDefinition(name, bd);
    }

    /**
     * Register a prototype bean with the underlying bean factory.
     * <p>For more advanced needs, register with the underlying BeanFactory directly.
     * @see #getDefaultListableBeanFactory
     */
    public void registerPrototype(String name, Class clazz, MutablePropertyValues pvs) throws BeansException {
        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setScope(GenericBeanDefinition.SCOPE_PROTOTYPE);
        bd.setBeanClass(clazz);
        bd.setPropertyValues(pvs);
        getDefaultListableBeanFactory().registerBeanDefinition(name, bd);
    }


}

