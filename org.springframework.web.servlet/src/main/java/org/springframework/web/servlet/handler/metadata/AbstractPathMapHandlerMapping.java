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

package org.springframework.web.servlet.handler.metadata;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Constants;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;

/**
 * Abstract implementation of the HandlerMapping interface that recognizes 
 * metadata attributes of type PathMap on application Controllers and automatically
 * wires them into the current servlet's WebApplicationContext.
 *
 * <p>The path must be mapped to the relevant Spring DispatcherServlet in /WEB-INF/web.xml.
 * It's possible to have multiple PathMap attributes on the one controller class.
 *
 * <p>Controllers instantiated by this class may have dependencies on middle tier
 * objects, expressed via JavaBean properties or constructor arguments. These will
 * be resolved automatically.
 *
 * <p>You will normally use this HandlerMapping with at most one DispatcherServlet in your
 * web application. Otherwise you'll end with one instance of the mapped controller for
 * each DispatcherServlet's context. You <i>might</i> want this -- for example, if
 * one's using a .pdf mapping and a PDF view, and another a JSP view, or if
 * using different middle tier objects, but should understand the implications. All
 * Controllers with attributes will be picked up by each DispatcherServlet's context.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @deprecated as of Spring 2.5, in favor of annotation-based request mapping.
 * To be removed in Spring 3.0.
 */
public abstract class AbstractPathMapHandlerMapping extends AbstractUrlHandlerMapping {

	/** Constants instance for AutowireCapableBeanFactory */
	private static final Constants constants = new Constants(AutowireCapableBeanFactory.class);

	private int autowireMode = AutowireCapableBeanFactory.AUTOWIRE_AUTODETECT;

	private boolean dependencyCheck = true;


	/**
	 * Set the autowire mode for handlers, by the name of the corresponding constant
	 * in the AutowireCapableBeanFactory interface, e.g. "AUTOWIRE_BY_NAME".
	 * @param constantName name of the constant
	 * @throws java.lang.IllegalArgumentException if an invalid constant was specified
	 * @see #setAutowireMode
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#AUTOWIRE_BY_NAME
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#AUTOWIRE_BY_TYPE
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#AUTOWIRE_CONSTRUCTOR
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#AUTOWIRE_AUTODETECT
	 */
	public void setAutowireModeName(String constantName) throws IllegalArgumentException {
		setAutowireMode(constants.asNumber(constantName).intValue());
	}

	/**
	 * Set the autowire mode for handlers. This determines whether any automagical
	 * detection and setting of bean references will happen.
	 * <p>Default is AUTOWIRE_AUTODETECT, which means either constructor autowiring or
	 * autowiring by type (depending on the constructors available in the class).
	 * @param autowireMode the autowire mode to set.
	 * Must be one of the constants defined in the AutowireCapableBeanFactory interface.
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#AUTOWIRE_BY_NAME
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#AUTOWIRE_BY_TYPE
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#AUTOWIRE_CONSTRUCTOR
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#AUTOWIRE_AUTODETECT
	 */
	public void setAutowireMode(int autowireMode) {
		this.autowireMode = autowireMode;
	}

	/**
	 * Set whether to perform a dependency check for objects on autowired handlers.
	 * Not applicable to autowiring a constructor, thus ignored there.
	 * <p>Default is "true".
	 */
	public void setDependencyCheck(boolean dependencyCheck) {
		this.dependencyCheck = dependencyCheck;
	}


	/**
	 * Calls the <code>detectAndCreateHandlers</code> method in addition
	 * to the superclass's initialization.
	 * @see #detectAndCreateHandlers
	 */
	public void initApplicationContext() throws BeansException {
		super.initApplicationContext();

		if (!(getApplicationContext() instanceof ConfigurableApplicationContext)) {
			throw new IllegalStateException(
					"[" + getClass().getName() + "] needs to run in a ConfigurableApplicationContext");
		}
		ConfigurableListableBeanFactory beanFactory =
				((ConfigurableApplicationContext) getApplicationContext()).getBeanFactory();
		detectAndCreateHandlers(beanFactory);
	}

	/**
	 * Look for all classes with a PathMap class attribute, instantiate them in
	 * the owning ApplicationContext, and register them as MVC handlers usable
	 * by the current DispatcherServlet.
	 * @param beanFactory the ConfigurableListableBeanFactory to register the
	 * created handler instances with
	 * @throws BeansException if handler detection or creation failed
	 * @see PathMap
	 * @see #getClassesWithPathMapAttributes()
	 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#createBean
	 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#registerSingleton
	 */
	protected void detectAndCreateHandlers(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		try {
			Class[] handlerClasses = getClassesWithPathMapAttributes();
			if (logger.isDebugEnabled()) {
				logger.debug("Found " + handlerClasses.length + " attribute-targeted handlers");
			}

			// for each Class returned by the Commons Attribute indexer
			for (int i = 0; i < handlerClasses.length; i++) {
				Class handlerClass = handlerClasses[i];

				// Autowire the given handler class via AutowireCapableBeanFactory.
				// Either autowires a constructor or by type, depending on the
				// constructors available in the given class.
				Object handler = beanFactory.createBean(handlerClass, this.autowireMode, this.dependencyCheck);

				// We now have an "autowired" handler, that may reference beans in the
				// application context. We now add the new handler to the factory.
				// This isn't necessary for the handler to work, but is useful if we want
				// to enumerate controllers in the factory etc.
				beanFactory.registerSingleton(handlerClass.getName(), handler);

				// There may be multiple paths mapped to this handler.
				PathMap[] pathMaps = getPathMapAttributes(handlerClass);
				registerHandler(pathMaps, handler);
			}
		}
		catch (BeansException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new BeanInitializationException("Could not retrieve PathMap attributes", ex);
		}
	}

	/**
	 * Register the given handler for the URL paths indicated by the given PathMaps.
	 * @param pathMaps the PathMap attributes for the handler class
	 * @param handler the handler instance
	 * @throws BeansException if the handler couldn't be registered
	 * @throws IllegalStateException if there is a conflicting handler registered
	 */
	protected void registerHandler(PathMap[] pathMaps, Object handler) throws BeansException, IllegalStateException {
		for (int j = 0; j < pathMaps.length; j++) {
			PathMap pathMap = pathMaps[j];
			String path = pathMap.getUrl();
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			registerHandler(path, handler);
		}
	}


	/**
	 * Use an attribute index to get a Collection of Class objects
	 * with the required PathMap attribute.
	 * @return a array of Class objects
	 */
	protected abstract Class[] getClassesWithPathMapAttributes() throws Exception;

	/**
	 * Use Attributes API to find PathMap attributes for the given handler class.
	 * We know there's at least one, as the getClassNamesWithPathMapAttributes
	 * method return this class name.
	 * @param handlerClass the handler class to look for
	 * @return an array of PathMap objects
	 */
	protected abstract PathMap[] getPathMapAttributes(Class handlerClass) throws Exception;

}
