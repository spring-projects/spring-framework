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

package org.springframework.web.filter;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.support.ServletContextResourceLoader;
import org.springframework.web.context.support.StandardServletEnvironment;

/**
 * Simple base implementation of {@link jakarta.servlet.Filter} which treats
 * its config parameters ({@code init-param} entries within the
 * {@code filter} tag in {@code web.xml}) as bean properties.
 *
 * <p>A handy superclass for any type of filter. Type conversion of config
 * parameters is automatic, with the corresponding setter method getting
 * invoked with the converted value. It is also possible for subclasses to
 * specify required properties. Parameters without matching bean property
 * setter will simply be ignored.
 *
 * <p>This filter leaves actual filtering to subclasses, which have to
 * implement the {@link jakarta.servlet.Filter#doFilter} method.
 *
 * <p>This generic filter base class has no dependency on the Spring
 * {@link org.springframework.context.ApplicationContext} concept.
 * Filters usually don't load their own context but rather access service
 * beans from the Spring root application context, accessible via the
 * filter's {@link #getServletContext() ServletContext} (see
 * {@link org.springframework.web.context.support.WebApplicationContextUtils}).
 *
 * @author Juergen Hoeller
 * @since 06.12.2003
 * @see #addRequiredProperty
 * @see #initFilterBean
 * @see #doFilter
 */
public abstract class GenericFilterBean implements Filter, BeanNameAware, EnvironmentAware,
		EnvironmentCapable, ServletContextAware, InitializingBean, DisposableBean {

	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	private @Nullable String beanName;

	private @Nullable Environment environment;

	private @Nullable ServletContext servletContext;

	private @Nullable FilterConfig filterConfig;

	private final Set<String> requiredProperties = new HashSet<>(4);


	/**
	 * Stores the bean name as defined in the Spring bean factory.
	 * <p>Only relevant in case of initialization as bean, to have a name as
	 * fallback to the filter name usually provided by a FilterConfig instance.
	 * @see org.springframework.beans.factory.BeanNameAware
	 * @see #getFilterName()
	 */
	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	/**
	 * Set the {@code Environment} that this filter runs in.
	 * <p>Any environment set here overrides the {@link StandardServletEnvironment}
	 * provided by default.
	 * <p>This {@code Environment} object is used only for resolving placeholders in
	 * resource paths passed into init-parameters for this filter. If no init-params are
	 * used, this {@code Environment} can be essentially ignored.
	 */
	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Return the {@link Environment} associated with this filter.
	 * <p>If none specified, a default environment will be initialized via
	 * {@link #createEnvironment()}.
	 * @since 4.3.9
	 */
	@Override
	public Environment getEnvironment() {
		if (this.environment == null) {
			this.environment = createEnvironment();
		}
		return this.environment;
	}

	/**
	 * Create and return a new {@link StandardServletEnvironment}.
	 * <p>Subclasses may override this in order to configure the environment or
	 * specialize the environment type returned.
	 * @since 4.3.9
	 */
	protected Environment createEnvironment() {
		return new StandardServletEnvironment();
	}

	/**
	 * Stores the ServletContext that the bean factory runs in.
	 * <p>Only relevant in case of initialization as bean, to have a ServletContext
	 * as fallback to the context usually provided by a FilterConfig instance.
	 * @see org.springframework.web.context.ServletContextAware
	 * @see #getServletContext()
	 */
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/**
	 * Calls the {@code initFilterBean()} method that might
	 * contain custom initialization of a subclass.
	 * <p>Only relevant in case of initialization as bean, where the
	 * standard {@code init(FilterConfig)} method won't be called.
	 * @see #initFilterBean()
	 * @see #init(jakarta.servlet.FilterConfig)
	 */
	@Override
	public void afterPropertiesSet() throws ServletException {
		initFilterBean();
	}

	/**
	 * Subclasses may override this to perform custom filter shutdown.
	 * <p>Note: This method will be called from standard filter destruction
	 * as well as filter bean destruction in a Spring application context.
	 * <p>This default implementation is empty.
	 */
	@Override
	public void destroy() {
	}


	/**
	 * Subclasses can invoke this method to specify that this property
	 * (which must match a JavaBean property they expose) is mandatory,
	 * and must be supplied as a config parameter. This should be called
	 * from the constructor of a subclass.
	 * <p>This method is only relevant in case of traditional initialization
	 * driven by a FilterConfig instance.
	 * @param property name of the required property
	 */
	protected final void addRequiredProperty(String property) {
		this.requiredProperties.add(property);
	}

	/**
	 * Standard way of initializing this filter.
	 * Map config parameters onto bean properties of this filter, and
	 * invoke subclass initialization.
	 * @param filterConfig the configuration for this filter
	 * @throws ServletException if bean properties are invalid (or required
	 * properties are missing), or if subclass initialization fails.
	 * @see #initFilterBean
	 */
	@Override
	public final void init(FilterConfig filterConfig) throws ServletException {
		Assert.notNull(filterConfig, "FilterConfig must not be null");

		this.filterConfig = filterConfig;

		// Set bean properties from init parameters.
		PropertyValues pvs = new FilterConfigPropertyValues(filterConfig, this.requiredProperties);
		if (!pvs.isEmpty()) {
			try {
				BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
				ResourceLoader resourceLoader = new ServletContextResourceLoader(filterConfig.getServletContext());
				Environment env = this.environment;
				if (env == null) {
					env = new StandardServletEnvironment();
				}
				bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, env));
				initBeanWrapper(bw);
				bw.setPropertyValues(pvs, true);
			}
			catch (BeansException ex) {
				String msg = "Failed to set bean properties on filter '" +
						filterConfig.getFilterName() + "': " + ex.getMessage();
				logger.error(msg, ex);
				throw new ServletException(msg, ex);
			}
		}

		// Let subclasses do whatever initialization they like.
		initFilterBean();

		if (logger.isDebugEnabled()) {
			logger.debug("Filter '" + filterConfig.getFilterName() + "' configured for use");
		}
	}

	/**
	 * Initialize the BeanWrapper for this GenericFilterBean,
	 * possibly with custom editors.
	 * <p>This default implementation is empty.
	 * @param bw the BeanWrapper to initialize
	 * @throws BeansException if thrown by BeanWrapper methods
	 * @see org.springframework.beans.BeanWrapper#registerCustomEditor
	 */
	protected void initBeanWrapper(BeanWrapper bw) throws BeansException {
	}

	/**
	 * Subclasses may override this to perform custom initialization.
	 * All bean properties of this filter will have been set before this
	 * method is invoked.
	 * <p>Note: This method will be called from standard filter initialization
	 * as well as filter bean initialization in a Spring application context.
	 * Filter name and ServletContext will be available in both cases.
	 * <p>This default implementation is empty.
	 * @throws ServletException if subclass initialization fails
	 * @see #getFilterName()
	 * @see #getServletContext()
	 */
	protected void initFilterBean() throws ServletException {
	}

	/**
	 * Make the FilterConfig of this filter available, if any.
	 * Analogous to GenericServlet's {@code getServletConfig()}.
	 * <p>Public to resemble the {@code getFilterConfig()} method
	 * of the Servlet Filter version that shipped with WebLogic 6.1.
	 * @return the FilterConfig instance, or {@code null} if none available
	 * @see jakarta.servlet.GenericServlet#getServletConfig()
	 */
	public @Nullable FilterConfig getFilterConfig() {
		return this.filterConfig;
	}

	/**
	 * Make the name of this filter available to subclasses.
	 * Analogous to GenericServlet's {@code getServletName()}.
	 * <p>Takes the FilterConfig's filter name by default.
	 * If initialized as bean in a Spring application context,
	 * it falls back to the bean name as defined in the bean factory.
	 * @return the filter name, or {@code null} if none available
	 * @see jakarta.servlet.GenericServlet#getServletName()
	 * @see jakarta.servlet.FilterConfig#getFilterName()
	 * @see #setBeanName
	 */
	protected @Nullable String getFilterName() {
		return (this.filterConfig != null ? this.filterConfig.getFilterName() : this.beanName);
	}

	/**
	 * Make the ServletContext of this filter available to subclasses.
	 * Analogous to GenericServlet's {@code getServletContext()}.
	 * <p>Takes the FilterConfig's ServletContext by default.
	 * If initialized as bean in a Spring application context,
	 * it falls back to the ServletContext that the bean factory runs in.
	 * @return the ServletContext instance
	 * @throws IllegalStateException if no ServletContext is available
	 * @see jakarta.servlet.GenericServlet#getServletContext()
	 * @see jakarta.servlet.FilterConfig#getServletContext()
	 * @see #setServletContext
	 */
	protected ServletContext getServletContext() {
		if (this.filterConfig != null) {
			return this.filterConfig.getServletContext();
		}
		else if (this.servletContext != null) {
			return this.servletContext;
		}
		else {
			throw new IllegalStateException("No ServletContext");
		}
	}


	/**
	 * PropertyValues implementation created from FilterConfig init parameters.
	 */
	@SuppressWarnings("serial")
	private static class FilterConfigPropertyValues extends MutablePropertyValues {

		/**
		 * Create new FilterConfigPropertyValues.
		 * @param config the FilterConfig we'll use to take PropertyValues from
		 * @param requiredProperties set of property names we need, where
		 * we can't accept default values
		 * @throws ServletException if any required properties are missing
		 */
		public FilterConfigPropertyValues(FilterConfig config, Set<String> requiredProperties)
				throws ServletException {

			Set<String> missingProps = (!CollectionUtils.isEmpty(requiredProperties) ?
					new HashSet<>(requiredProperties) : null);

			Enumeration<String> paramNames = config.getInitParameterNames();
			while (paramNames.hasMoreElements()) {
				String property = paramNames.nextElement();
				Object value = config.getInitParameter(property);
				addPropertyValue(new PropertyValue(property, value));
				if (missingProps != null) {
					missingProps.remove(property);
				}
			}

			// Fail if we are still missing properties.
			if (!CollectionUtils.isEmpty(missingProps)) {
				throw new ServletException(
						"Initialization from FilterConfig for filter '" + config.getFilterName() +
						"' failed; the following required properties were missing: " +
						StringUtils.collectionToDelimitedString(missingProps, ", "));
			}
		}
	}

}
