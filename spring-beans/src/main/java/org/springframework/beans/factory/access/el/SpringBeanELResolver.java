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

package org.springframework.beans.factory.access.el;

import java.beans.FeatureDescriptor;
import java.util.Iterator;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotWritableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;

/**
 * Unified EL <code>ELResolver</code> that delegates to a Spring BeanFactory,
 * resolving name references to Spring-defined beans.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 * @see org.springframework.web.jsf.el.SpringBeanFacesELResolver
 */
public abstract class SpringBeanELResolver extends ELResolver {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public Object getValue(ELContext elContext, Object base, Object property) throws ELException {
		if (base == null) {
			String beanName = property.toString();
			BeanFactory bf = getBeanFactory(elContext);
			if (bf.containsBean(beanName)) {
				if (logger.isTraceEnabled()) {
					logger.trace("Successfully resolved variable '" + beanName + "' in Spring BeanFactory");
				}
				elContext.setPropertyResolved(true);
				return bf.getBean(beanName);
			}
		}
		return null;
	}

	@Override
	public Class<?> getType(ELContext elContext, Object base, Object property) throws ELException {
		if (base == null) {
			String beanName = property.toString();
			BeanFactory bf = getBeanFactory(elContext);
			if (bf.containsBean(beanName)) {
				elContext.setPropertyResolved(true);
				return bf.getType(beanName);
			}
		}
		return null;
	}

	@Override
	public void setValue(ELContext elContext, Object base, Object property, Object value) throws ELException {
		if (base == null) {
			String beanName = property.toString();
			BeanFactory bf = getBeanFactory(elContext);
			if (bf.containsBean(beanName)) {
				throw new PropertyNotWritableException(
						"Variable '" + beanName + "' refers to a Spring bean which by definition is not writable");
			}
		}
	}

	@Override
	public boolean isReadOnly(ELContext elContext, Object base, Object property) throws ELException {
		if (base == null) {
			String beanName = property.toString();
			BeanFactory bf = getBeanFactory(elContext);
			if (bf.containsBean(beanName)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext elContext, Object base) {
		return null;
	}

	@Override
	public Class<?> getCommonPropertyType(ELContext elContext, Object base) {
		return Object.class;
	}


	/**
	 * Retrieve the Spring BeanFactory to delegate bean name resolution to.
	 * @param elContext the current ELContext
	 * @return the Spring BeanFactory (never <code>null</code>)
	 */
	protected abstract BeanFactory getBeanFactory(ELContext elContext);

}
