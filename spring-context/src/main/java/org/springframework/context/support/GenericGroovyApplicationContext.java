/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.context.support;

import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * An {@link org.springframework.context.ApplicationContext} implementation that extends
 * {@link GenericApplicationContext} and implements {@link GroovyObject} such that beans
 * can be retrieved with the dot de-reference syntax instead of using {@link #getBean}.
 *
 * <p>Consider this as the equivalent of {@link GenericXmlApplicationContext} for
 * Groovy bean definitions, or even an upgrade thereof since it seamlessly understands
 * XML bean definition files as well. The main difference is that, within a Groovy
 * script, the context can be used with an inline bean definition closure as follows:
 *
 * <pre class="code">
 * import org.hibernate.SessionFactory
 * import org.apache.commons.dbcp.BasicDataSource
 *
 * def context = new GenericGroovyApplicationContext()
 * context.reader.beans {
 *     dataSource(BasicDataSource) {                  // <--- invokeMethod
 *         driverClassName = "org.hsqldb.jdbcDriver"
 *         url = "jdbc:hsqldb:mem:grailsDB"
 *         username = "sa"                            // <-- setProperty
 *         password = ""
 *         settings = [mynew:"setting"]
 *     }
 *     sessionFactory(SessionFactory) {
 *         dataSource = dataSource                    // <-- getProperty for retrieving references
 *     }
 *     myService(MyService) {
 *         nestedBean = { AnotherBean bean ->         // <-- setProperty with closure for nested bean
 *             dataSource = dataSource
 *         }
 *     }
 * }
 * context.refresh()
 * </pre>
 *
 * <p>Alternatively, load a Groovy bean definition script like the following
 * from an external resource (e.g. an "applicationContext.groovy" file):
 *
 * <pre class="code">
 * import org.hibernate.SessionFactory
 * import org.apache.commons.dbcp.BasicDataSource
 *
 * beans {
 *     dataSource(BasicDataSource) {
 *         driverClassName = "org.hsqldb.jdbcDriver"
 *         url = "jdbc:hsqldb:mem:grailsDB"
 *         username = "sa"
 *         password = ""
 *         settings = [mynew:"setting"]
 *     }
 *     sessionFactory(SessionFactory) {
 *         dataSource = dataSource
 *     }
 *     myService(MyService) {
 *         nestedBean = { AnotherBean bean ->
 *             dataSource = dataSource
 *         }
 *     }
 * }
 * </pre>
 *
 * <p>With the following Java code creating the {@code GenericGroovyApplicationContext}
 * (potentially using Ant-style '*'/'**' location patterns):
 *
 * <pre class="code">
 * GenericGroovyApplicationContext context = new GenericGroovyApplicationContext();
 * context.load("org/myapp/applicationContext.groovy");
 * context.refresh();
 * </pre>
 *
 * <p>Or even more concise, provided that no extra configuration is needed:
 *
 * <pre class="code">
 * ApplicationContext context = new GenericGroovyApplicationContext("org/myapp/applicationContext.groovy");
 * </pre>
 *
 * <p><b>This application context also understands XML bean definition files,
 * allowing for seamless mixing and matching with Groovy bean definition files.</b>
 * ".xml" files will be parsed as XML content; all other kinds of resources will
 * be parsed as Groovy scripts.
 *
 * @author Juergen Hoeller
 * @author Jeff Brown
 * @since 4.0
 * @see org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader
 */
public class GenericGroovyApplicationContext extends GenericApplicationContext implements GroovyObject {

	private final GroovyBeanDefinitionReader reader = new GroovyBeanDefinitionReader(this);

	private final BeanWrapper contextWrapper = new BeanWrapperImpl(this);

	private MetaClass metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(getClass());


	/**
	 * Create a new GenericGroovyApplicationContext that needs to be
	 * {@link #load loaded} and then manually {@link #refresh refreshed}.
	 */
	public GenericGroovyApplicationContext() {
	}

	/**
	 * Create a new GenericGroovyApplicationContext, loading bean definitions
	 * from the given resources and automatically refreshing the context.
	 * @param resources the resources to load from
	 */
	public GenericGroovyApplicationContext(Resource... resources) {
		load(resources);
		refresh();
	}

	/**
	 * Create a new GenericGroovyApplicationContext, loading bean definitions
	 * from the given resource locations and automatically refreshing the context.
	 * @param resourceLocations the resources to load from
	 */
	public GenericGroovyApplicationContext(String... resourceLocations) {
		load(resourceLocations);
		refresh();
	}

	/**
	 * Create a new GenericGroovyApplicationContext, loading bean definitions
	 * from the given resource locations and automatically refreshing the context.
	 * @param relativeClass class whose package will be used as a prefix when
	 * loading each specified resource name
	 * @param resourceNames relatively-qualified names of resources to load
	 */
	public GenericGroovyApplicationContext(Class<?> relativeClass, String... resourceNames) {
		load(relativeClass, resourceNames);
		refresh();
	}


	/**
	 * Exposes the underlying {@link GroovyBeanDefinitionReader} for convenient access
	 * to the {@code loadBeanDefinition} methods on it as well as the ability
	 * to specify an inline Groovy bean definition closure.
	 * @see GroovyBeanDefinitionReader#loadBeanDefinitions(org.springframework.core.io.Resource...)
	 * @see GroovyBeanDefinitionReader#loadBeanDefinitions(String...)
	 */
	public final GroovyBeanDefinitionReader getReader() {
		return this.reader;
	}

	/**
	 * Delegates the given environment to underlying {@link GroovyBeanDefinitionReader}.
	 * Should be called before any call to {@code #load}.
	 */
	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		super.setEnvironment(environment);
		this.reader.setEnvironment(getEnvironment());
	}

	/**
	 * Load bean definitions from the given Groovy scripts or XML files.
	 * <p>Note that ".xml" files will be parsed as XML content; all other kinds
	 * of resources will be parsed as Groovy scripts.
	 * @param resources one or more resources to load from
	 */
	public void load(Resource... resources) {
		this.reader.loadBeanDefinitions(resources);
	}

	/**
	 * Load bean definitions from the given Groovy scripts or XML files.
	 * <p>Note that ".xml" files will be parsed as XML content; all other kinds
	 * of resources will be parsed as Groovy scripts.
	 * @param resourceLocations one or more resource locations to load from
	 */
	public void load(String... resourceLocations) {
		this.reader.loadBeanDefinitions(resourceLocations);
	}

	/**
	 * Load bean definitions from the given Groovy scripts or XML files.
	 * <p>Note that ".xml" files will be parsed as XML content; all other kinds
	 * of resources will be parsed as Groovy scripts.
	 * @param relativeClass class whose package will be used as a prefix when
	 * loading each specified resource name
	 * @param resourceNames relatively-qualified names of resources to load
	 */
	public void load(Class<?> relativeClass, String... resourceNames) {
		Resource[] resources = new Resource[resourceNames.length];
		for (int i = 0; i < resourceNames.length; i++) {
			resources[i] = new ClassPathResource(resourceNames[i], relativeClass);
		}
		load(resources);
	}


	// Implementation of the GroovyObject interface

	public void setMetaClass(MetaClass metaClass) {
		this.metaClass = metaClass;
	}

	public MetaClass getMetaClass() {
		return this.metaClass;
	}

	public Object invokeMethod(String name, Object args) {
		return this.metaClass.invokeMethod(this, name, args);
	}

	public void setProperty(String property, Object newValue) {
		if (newValue instanceof BeanDefinition) {
			registerBeanDefinition(property, (BeanDefinition) newValue);
		}
		else {
			this.metaClass.setProperty(this, property, newValue);
		}
	}

	@Nullable
	public Object getProperty(String property) {
		if (containsBean(property)) {
			return getBean(property);
		}
		else if (this.contextWrapper.isReadableProperty(property)) {
			return this.contextWrapper.getPropertyValue(property);
		}
		throw new NoSuchBeanDefinitionException(property);
	}

}
