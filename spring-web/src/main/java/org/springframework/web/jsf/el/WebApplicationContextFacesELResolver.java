/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.jsf.el;

import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ELResolver;
import jakarta.faces.context.FacesContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.jsf.FacesContextUtils;

/**
 * Special JSF {@code ELResolver} that exposes the Spring {@code WebApplicationContext}
 * instance under a variable named "webApplicationContext".
 *
 * <p>In contrast to {@link SpringBeanFacesELResolver}, this ELResolver variant
 * does <i>not</i> resolve JSF variable names as Spring bean names. It rather
 * exposes Spring's root WebApplicationContext <i>itself</i> under a special name,
 * and is able to resolve "webApplicationContext.mySpringManagedBusinessObject"
 * dereferences to Spring-defined beans in that application context.
 *
 * <p>Configure this resolver in your {@code faces-config.xml} file as follows:
 *
 * <pre class="code">
 * &lt;application&gt;
 *   ...
 *   &lt;el-resolver&gt;org.springframework.web.jsf.el.WebApplicationContextFacesELResolver&lt;/el-resolver&gt;
 * &lt;/application&gt;</pre>
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see SpringBeanFacesELResolver
 * @see org.springframework.web.jsf.FacesContextUtils#getWebApplicationContext
 */
public class WebApplicationContextFacesELResolver extends ELResolver {

	/**
	 * Name of the exposed WebApplicationContext variable: "webApplicationContext".
	 */
	public static final String WEB_APPLICATION_CONTEXT_VARIABLE_NAME = "webApplicationContext";


	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public @Nullable Object getValue(ELContext elContext, @Nullable Object base, Object property) throws ELException {
		if (base != null) {
			if (base instanceof WebApplicationContext wac) {
				String beanName = property.toString();
				if (logger.isTraceEnabled()) {
					logger.trace("Attempting to resolve property '" + beanName + "' in root WebApplicationContext");
				}
				if (wac.containsBean(beanName)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Successfully resolved property '" + beanName + "' in root WebApplicationContext");
					}
					elContext.setPropertyResolved(true);
					try {
						return wac.getBean(beanName);
					}
					catch (BeansException ex) {
						throw new ELException(ex);
					}
				}
				else {
					// Mimic standard JSF/JSP behavior when base is a Map by returning null.
					return null;
				}
			}
		}
		else {
			if (WEB_APPLICATION_CONTEXT_VARIABLE_NAME.equals(property)) {
				elContext.setPropertyResolved(true);
				return getWebApplicationContext(elContext);
			}
		}

		return null;
	}

	@Override
	public @Nullable Class<?> getType(ELContext elContext, @Nullable Object base, Object property) throws ELException {
		if (base != null) {
			if (base instanceof WebApplicationContext wac) {
				String beanName = property.toString();
				if (logger.isDebugEnabled()) {
					logger.debug("Attempting to resolve property '" + beanName + "' in root WebApplicationContext");
				}
				if (wac.containsBean(beanName)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Successfully resolved property '" + beanName + "' in root WebApplicationContext");
					}
					elContext.setPropertyResolved(true);
					try {
						return wac.getType(beanName);
					}
					catch (BeansException ex) {
						throw new ELException(ex);
					}
				}
				else {
					// Mimic standard JSF/JSP behavior when base is a Map by returning null.
					return null;
				}
			}
		}
		else {
			if (WEB_APPLICATION_CONTEXT_VARIABLE_NAME.equals(property)) {
				elContext.setPropertyResolved(true);
				return WebApplicationContext.class;
			}
		}

		return null;
	}

	@Override
	public void setValue(ELContext elContext, Object base, Object property, Object value) throws ELException {
	}

	@Override
	public boolean isReadOnly(ELContext elContext, Object base, Object property) throws ELException {
		if (base instanceof WebApplicationContext) {
			elContext.setPropertyResolved(true);
			return true;
		}
		return false;
	}

	@Override
	public Class<?> getCommonPropertyType(ELContext elContext, Object base) {
		return Object.class;
	}


	/**
	 * Retrieve the {@link WebApplicationContext} reference to expose.
	 * <p>The default implementation delegates to {@link FacesContextUtils},
	 * returning {@code null} if no {@code WebApplicationContext} found.
	 * @param elContext the current JSF ELContext
	 * @return the Spring web application context
	 * @see org.springframework.web.jsf.FacesContextUtils#getWebApplicationContext
	 */
	protected @Nullable WebApplicationContext getWebApplicationContext(ELContext elContext) {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		return FacesContextUtils.getRequiredWebApplicationContext(facesContext);
	}

}
