/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.portlet.handler;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import javax.portlet.Portlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.web.portlet.context.PortletConfigAware;
import org.springframework.web.portlet.context.PortletContextAware;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor}
 * that applies initialization and destruction callbacks to beans that
 * implement the {@link javax.portlet.Portlet} interface.
 *
 * <p>After initialization of the bean instance, the Portlet {@code init}
 * method will be called with a PortletConfig that contains the bean name
 * of the Portlet and the PortletContext that it is running in.
 *
 * <p>Before destruction of the bean instance, the Portlet {@code destroy}
 * will be called.
 *
 * <p><b>Note that this post-processor does not support Portlet initialization
 * parameters.</b> Bean instances that implement the Portlet interface are
 * supposed to be configured like any other Spring bean, that is, through
 * constructor arguments or bean properties.
 *
 * <p>For reuse of a Portlet implementation in a plain Portlet container
 * and as a bean in a Spring context, consider deriving from Spring's
 * {@link org.springframework.web.portlet.GenericPortletBean} base class that
 * applies Portlet initialization parameters as bean properties, supporting
 * both the standard Portlet and the Spring bean initialization style.
 *
 * <p><b>Alternatively, consider wrapping a Portlet with Spring's
 * {@link org.springframework.web.portlet.mvc.PortletWrappingController}.</b>
 * This is particularly appropriate for existing Portlet classes,
 * allowing to specify Portlet initialization parameters etc.
 *
 * @author Juergen Hoeller
 * @author John A. Lewis
 * @since 2.0
 * @see javax.portlet.Portlet
 * @see javax.portlet.PortletConfig
 * @see SimplePortletHandlerAdapter
 * @see org.springframework.web.portlet.GenericPortletBean
 * @see org.springframework.web.portlet.mvc.PortletWrappingController
 */
public class SimplePortletPostProcessor
	implements DestructionAwareBeanPostProcessor, PortletContextAware, PortletConfigAware {

	private boolean useSharedPortletConfig = true;

	private PortletContext portletContext;

	private PortletConfig portletConfig;


	/**
	 * Set whether to use the shared PortletConfig object passed in
	 * through {@code setPortletConfig}, if available.
	 * <p>Default is "true". Turn this setting to "false" to pass in
	 * a mock PortletConfig object with the bean name as portlet name,
	 * holding the current PortletContext.
	 * @see #setPortletConfig
	 */
	public void setUseSharedPortletConfig(boolean useSharedPortletConfig) {
		this.useSharedPortletConfig = useSharedPortletConfig;
	}

	@Override
	public void setPortletContext(PortletContext portletContext) {
		this.portletContext = portletContext;
	}

	@Override
	public void setPortletConfig(PortletConfig portletConfig) {
		this.portletConfig = portletConfig;
	}


	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof Portlet) {
			PortletConfig config = this.portletConfig;
			if (config == null || !this.useSharedPortletConfig) {
				config = new DelegatingPortletConfig(beanName, this.portletContext, this.portletConfig);
			}
			try {
				((Portlet) bean).init(config);
			}
			catch (PortletException ex) {
				throw new BeanInitializationException("Portlet.init threw exception", ex);
			}
		}
		return bean;
	}

	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		if (bean instanceof Portlet) {
			((Portlet) bean).destroy();
		}
	}

	@Override
	public boolean requiresDestruction(Object bean) {
		return (bean instanceof Portlet);
	}


	/**
	 * Internal implementation of the PortletConfig interface, to be passed
	 * to the wrapped servlet.
	 */
	private static class DelegatingPortletConfig implements PortletConfig {

		private final String portletName;

		private final PortletContext portletContext;

		private final PortletConfig portletConfig;

		public DelegatingPortletConfig(String portletName, PortletContext portletContext, PortletConfig portletConfig) {
			this.portletName = portletName;
			this.portletContext = portletContext;
			this.portletConfig = portletConfig;
		}

		@Override
		public String getPortletName() {
			return this.portletName;
		}

		@Override
		public PortletContext getPortletContext() {
			return this.portletContext;
		}

		@Override
		public String getInitParameter(String paramName) {
			return null;
		}

		@Override
		public Enumeration<String> getInitParameterNames() {
			return Collections.enumeration(Collections.<String>emptySet());
		}

		@Override
		public ResourceBundle getResourceBundle(Locale locale) {
			return (this.portletConfig != null ? this.portletConfig.getResourceBundle(locale) : null);
		}

		@Override
		public Enumeration<String> getPublicRenderParameterNames() {
			return Collections.enumeration(Collections.<String>emptySet());
		}

		@Override
		public String getDefaultNamespace() {
			return XMLConstants.NULL_NS_URI;
		}

		@Override
		public Enumeration<QName> getPublishingEventQNames() {
			return Collections.enumeration(Collections.<QName>emptySet());
		}

		@Override
		public Enumeration<QName> getProcessingEventQNames() {
			return Collections.enumeration(Collections.<QName>emptySet());
		}

		@Override
		public Enumeration<Locale> getSupportedLocales() {
			return Collections.enumeration(Collections.<Locale>emptySet());
		}

		@Override
		public Map<String, String[]> getContainerRuntimeOptions() {
			return (this.portletConfig != null ? this.portletConfig.getContainerRuntimeOptions() : null);
		}
	}

}
