/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory.access;

import org.springframework.beans.BeansException;

/**
 * Defines a contract for the lookup, use, and release of a
 * {@link org.springframework.beans.factory.BeanFactory},
 * or a {@code BeanFactory} subclass such as an
 * {@link org.springframework.context.ApplicationContext}.
 *
 * <p>Where this interface is implemented as a singleton class such as
 * {@link SingletonBeanFactoryLocator}, the Spring team <strong>strongly</strong>
 * suggests that it be used sparingly and with caution. By far the vast majority
 * of the code inside an application is best written in a Dependency Injection
 * style, where that code is served out of a
 * {@code BeanFactory}/{@code ApplicationContext} container, and has
 * its own dependencies supplied by the container when it is created. However,
 * even such a singleton implementation sometimes has its use in the small glue
 * layers of code that is sometimes needed to tie other code together. For
 * example, third party code may try to construct new objects directly, without
 * the ability to force it to get these objects out of a {@code BeanFactory}.
 * If the object constructed by the third party code is just a small stub or
 * proxy, which then uses an implementation of this class to get a
 * {@code BeanFactory} from which it gets the real object, to which it
 * delegates, then proper Dependency Injection has been achieved.
 *
 * <p>As another example, in a complex J2EE app with multiple layers, with each
 * layer having its own {@code ApplicationContext} definition (in a
 * hierarchy), a class like {@code SingletonBeanFactoryLocator} may be used
 * to demand load these contexts.
 *
 * @author Colin Sampaleanu
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.context.access.DefaultLocatorFactory
 * @see org.springframework.context.ApplicationContext
 */
public interface BeanFactoryLocator {

	/**
	 * Use the {@link org.springframework.beans.factory.BeanFactory} (or derived
	 * interface such as {@link org.springframework.context.ApplicationContext})
	 * specified by the {@code factoryKey} parameter.
	 * <p>The definition is possibly loaded/created as needed.
	 * @param factoryKey a resource name specifying which {@code BeanFactory} the
	 * {@code BeanFactoryLocator} must return for usage. The actual meaning of the
	 * resource name is specific to the implementation of {@code BeanFactoryLocator}.
	 * @return the {@code BeanFactory} instance, wrapped as a {@link BeanFactoryReference} object
	 * @throws BeansException if there is an error loading or accessing the {@code BeanFactory}
	 */
	BeanFactoryReference useBeanFactory(String factoryKey) throws BeansException;

}
