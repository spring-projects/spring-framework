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
package org.springframework.config.java.support;

import java.lang.reflect.Method;

import net.sf.cglib.proxy.MethodInterceptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.util.Assert;


/**
 * Base class for all {@link MethodInterceptor} implementations.
 * 
 * @author Chris Beams
 */
abstract class AbstractMethodInterceptor implements BeanFactoryAware, MethodInterceptor {
	protected final Log log = LogFactory.getLog(this.getClass());
	protected DefaultListableBeanFactory beanFactory;

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(DefaultListableBeanFactory.class, beanFactory);
		this.beanFactory = (DefaultListableBeanFactory) beanFactory;
	}

	protected String getBeanName(Method method) {
		return method.getName();
	}
}
