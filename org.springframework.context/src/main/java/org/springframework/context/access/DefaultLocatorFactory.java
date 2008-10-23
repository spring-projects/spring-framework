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

package org.springframework.context.access;

import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.access.BeanFactoryLocator;

/**
 * A factory class to get a default ContextSingletonBeanFactoryLocator instance.
 *
 * @author Colin Sampaleanu
 * @see org.springframework.context.access.ContextSingletonBeanFactoryLocator
 */
public class DefaultLocatorFactory {

	/**
	 * Return an instance object implementing BeanFactoryLocator. This will normally
	 * be a singleton instance of the specific ContextSingletonBeanFactoryLocator class,
	 * using the default resource selector.
	 */
	public static BeanFactoryLocator getInstance() throws FatalBeanException {
		return ContextSingletonBeanFactoryLocator.getInstance();
	}

	/**
	 * Return an instance object implementing BeanFactoryLocator. This will normally
	 * be a singleton instance of the specific ContextSingletonBeanFactoryLocator class,
	 * using the specified resource selector.
	 * @param selector a selector variable which provides a hint to the factory as to
	 * which instance to return.
	 */
	public static BeanFactoryLocator getInstance(String selector) throws FatalBeanException {
		return ContextSingletonBeanFactoryLocator.getInstance(selector);
	}
}
