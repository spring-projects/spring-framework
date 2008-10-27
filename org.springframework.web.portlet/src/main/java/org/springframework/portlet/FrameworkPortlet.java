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

package org.springframework.web.portlet;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SourceFilteringListener;
import org.springframework.web.portlet.context.ConfigurablePortletApplicationContext;
import org.springframework.web.portlet.context.PortletApplicationContextUtils;
import org.springframework.web.portlet.context.PortletRequestHandledEvent;
import org.springframework.web.portlet.context.XmlPortletApplicationContext;

/**
 * Base portlet for Spring's portlet framework. Provides integration with
 * a Spring application context, in a JavaBean-based overall solution.
 *
 * <p>This class offers the following functionality:
 * <ul>
 * <li>Manages a Portlet {@link org.springframework.context.ApplicationContext}
 * instance per portlet. The portlet's configuration is determined by beans
 * in the portlet's namespace.
 * <li>Publishes events on request processing, whether or not a request is
 * successfully handled.
 * </ul>
 *
 * <p>Subclasses must implement {@link #doActionService} and {@link #doRenderService}
 * to handle action and render requests. Because this extends {@link GenericPortletBean}
 * rather than Portlet directly, bean properties are mapped onto it. Subclasses can
 * override {@link #initFrameworkPortlet()} for custom initialization.
 *
 * <p>Regards a "contextClass" parameter at the portlet init-param level,
 * falling back to the default context class
 * ({@link org.springframework.web.portlet.context.XmlPortletApplicationContext})
 * if not found. Note that, with the default FrameworkPortlet,
 * a context class needs to implement the
 * {@link org.springframework.web.portlet.context.ConfigurablePortletApplicationContext} SPI.
 *
 * <p>Passes a "contextConfigLocation" portlet init-param to the context instance,
 * parsing it into potentially multiple file paths which can be separated by any
 * number of commas and spaces, like "test-portlet.xml, myPortlet.xml".
 * If not explicitly specified, the context implementation is supposed to build a
 * default location from the namespace of the portlet.
 *
 * <p>Note: In case of multiple config locations, later bean definitions will
 * override ones defined in earlier loaded files, at least when using one of
 * Spring's default ApplicationContext implementations. This can be leveraged
 * to deliberately override certain bean definitions via an extra XML file.
 *
 * <p>The default namespace is "'portlet-name'-portlet", e.g. "test-portlet" for a
 * portlet-name "test" (leading to a "/WEB-INF/test-portlet.xml" default location
 * with XmlPortletApplicationContext). The namespace can also be set explicitly via
 * the "namespace" portlet init-param.
 *
 * @author William G. Thompson, Jr.
 * @author John A. Lewis
 * @author Juergen Hoeller
 * @since 2.0
 * @see #doActionService
 * @see #doRenderService
 * @see #setContextClass
 * @see #setContextConfigLocation
 * @see #setNamespace
 */
public abstract class FrameworkPortlet extends GenericPortletBean implements ApplicationListener {

	/**
	 * Default context class for FrameworkPortlet.
	 * @see org.springframework.web.portlet.context.XmlPortletApplicationContext
	 */
	public static final Class DEFAULT_CONTEXT_CLASS = XmlPortletApplicationContext.class;

	/**
	 * Suffix for Portlet ApplicationContext namespaces. If a portlet of this class is
	 * given the name "test" in a context, the namespace used by the portlet will
	 * resolve to "test-portlet".
	 */
	public static final String DEFAULT_NAMESPACE_SUFFIX = "-portlet";

	/**
	 * Prefix for the PortletContext attribute for the Portlet ApplicationContext.
	 * The completion is the portlet name.
	 */
	public static final String PORTLET_CONTEXT_PREFIX = FrameworkPortlet.class.getName() + ".CONTEXT.";

	/**
	 * Default USER_INFO attribute names to search for the current username:
	 * "user.login.id", "user.name".
	 */
	public static final String[] DEFAULT_USERINFO_ATTRIBUTE_NAMES = {"user.login.id", "user.name"};


	/** Portlet ApplicationContext implementation class to use */
	private Class contextClass = DEFAULT_CONTEXT_CLASS;

	/** Namespace for this portlet */
	private String namespace;

	/** Explicit context config location */
	private String contextConfigLocation;

	/** Should we publish the context as a PortletContext attribute? */
	private boolean publishContext = true;

	/** Should we publish a PortletRequestHandledEvent at the end of each request? */
	private boolean publishEvents = true;

	/** USER_INFO attributes that may contain the username of the current user */
	private String[] userinfoUsernameAttributes = DEFAULT_USERINFO_ATTRIBUTE_NAMES;

	/** ApplicationContext for this portlet */
	private ApplicationContext portletApplicationContext;

	/** Flag used to detect whether onRefresh has already been called */
	private boolean refreshEventReceived = false;


	/**
	 * Set a custom context class. This class must be of type ApplicationContext;
	 * when using the default FrameworkPortlet implementation, the context class
	 * must also implement ConfigurablePortletApplicationContext.
	 * @see #createPortletApplicationContext
	 */
	public void setContextClass(Class contextClass) {
		this.contextClass = contextClass;
	}

	/**
	 * Return the custom context class.
	 */
	public Class getContextClass() {
		return this.contextClass;
	}

	/**
	 * Set a custom namespace for this portlet,
	 * to be used for building a default context config location.
	 */
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	/**
	 * Return the namespace for this portlet, falling back to default scheme if
	 * no custom namespace was set. (e.g. "test-portlet" for a portlet named "test")
	 */
	public String getNamespace() {
		return (this.namespace != null) ? this.namespace : getPortletName() + DEFAULT_NAMESPACE_SUFFIX;
	}

	/**
	 * Set the context config location explicitly, instead of relying on the default
	 * location built from the namespace. This location string can consist of
	 * multiple locations separated by any number of commas and spaces.
	 */
	public void setContextConfigLocation(String contextConfigLocation) {
		this.contextConfigLocation = contextConfigLocation;
	}

	/**
	 * Return the explicit context config location, if any.
	 */
	public String getContextConfigLocation() {
		return this.contextConfigLocation;
	}

	/**
	 * Set whether to publish this portlet's context as a PortletContext attribute,
	 * available to all objects in the web container. Default is true.
	 * <p>This is especially handy during testing, although it is debatable whether
	 * it's good practice to let other application objects access the context this way.
	 */
	public void setPublishContext(boolean publishContext) {
		this.publishContext = publishContext;
	}

	/**
	 * Return whether to publish this portlet's context as a PortletContext attribute.
	 */
	public boolean isPublishContext() {
		return this.publishContext;
	}

	/**
	 * Set whether this portlet should publish a PortletRequestHandledEvent at the end
	 * of each request. Default is true; can be turned off for a slight performance
	 * improvement, provided that no ApplicationListeners rely on such events.
	 * @see org.springframework.web.portlet.context.PortletRequestHandledEvent
	 */
	public void setPublishEvents(boolean publishEvents) {
		this.publishEvents = publishEvents;
	}

	/**
	 * Return whether this portlet should publish a PortletRequestHandledEvent at the end
	 * of each request.
	 */
	public boolean isPublishEvents() {
		return this.publishEvents;
	}

	/**
	 * Set the list of attributes to search in the USER_INFO map when trying
	 * to find the username of the current user.
	 * @see #getUsernameForRequest
	 */
	public void setUserinfoUsernameAttributes(String[] userinfoUsernameAttributes) {
		this.userinfoUsernameAttributes = userinfoUsernameAttributes;
	}

	/**
	 * Returns the list of attributes that will be searched in the USER_INFO map
	 * when trying to find the username of the current user
	 * @see #getUsernameForRequest
	 */
	public String[] getUserinfoUsernameAttributes() {
		return this.userinfoUsernameAttributes;
	}


	/**
	 * Overridden method of GenericPortletBean, invoked after any bean properties
	 * have been set. Creates this portlet's ApplicationContext.
	 */
	protected final void initPortletBean() throws PortletException, BeansException {
		getPortletContext().log("Initializing Spring FrameworkPortlet '" + getPortletName() + "'");
		if (logger.isInfoEnabled()) {
			logger.info("FrameworkPortlet '" + getPortletName() + "': initialization started");
		}
		long startTime = System.currentTimeMillis();

		try {
			this.portletApplicationContext = initPortletApplicationContext();
			initFrameworkPortlet();
		}
		catch (PortletException ex) {
			logger.error("Context initialization failed", ex);
			throw ex;
		}
		catch (BeansException ex) {
			logger.error("Context initialization failed", ex);
			throw ex;
		}

		if (logger.isInfoEnabled()) {
			long elapsedTime = System.currentTimeMillis() - startTime;
			logger.info("FrameworkPortlet '" + getPortletName() + "': initialization completed in " + elapsedTime + " ms");
		}
	}

	/**
	 * Initialize and publish the Portlet ApplicationContext for this portlet.
	 * <p>Delegates to {@link #createPortletApplicationContext} for actual creation.
	 * Can be overridden in subclasses.
	 * @return the ApplicationContext for this portlet
	 * @throws BeansException if the context couldn't be initialized
	 */
	protected ApplicationContext initPortletApplicationContext() throws BeansException {
		ApplicationContext parent = PortletApplicationContextUtils.getWebApplicationContext(getPortletContext());
		ApplicationContext pac = createPortletApplicationContext(parent);

		if (!this.refreshEventReceived) {
			// Apparently not a ConfigurableApplicationContext with refresh support:
			// triggering initial onRefresh manually here.
			onRefresh(pac);
		}

		if (isPublishContext()) {
			// publish the context as a portlet context attribute
			String attName = getPortletContextAttributeName();
			getPortletContext().setAttribute(attName, pac);
			if (logger.isDebugEnabled()) {
				logger.debug("Published ApplicationContext of portlet '" + getPortletName() +
						"' as PortletContext attribute with name [" + attName + "]");
			}
		}
		return pac;
	}

	/**
	 * Instantiate the Portlet ApplicationContext for this portlet, either a default
	 * XmlPortletApplicationContext or a custom context class if set.
	 * <p>This implementation expects custom contexts to implement
	 * ConfigurablePortletApplicationContext. Can be overridden in subclasses.
	 * @param parent the parent ApplicationContext to use, or null if none
	 * @return the Portlet ApplicationContext for this portlet
	 * @throws BeansException if the context couldn't be initialized
	 * @see #setContextClass
	 * @see org.springframework.web.portlet.context.XmlPortletApplicationContext
	 */
	protected ApplicationContext createPortletApplicationContext(ApplicationContext parent)
			throws BeansException {

		if (logger.isDebugEnabled()) {
			logger.debug("Portlet with name '" + getPortletName() +
					"' will try to create custom ApplicationContext context of class '" +
					getContextClass().getName() + "'" + ", using parent context [" + parent + "]");
		}
		if (!ConfigurablePortletApplicationContext.class.isAssignableFrom(getContextClass())) {
			throw new ApplicationContextException("Fatal initialization error in portlet with name '" + getPortletName() +
					"': custom ApplicationContext class [" + getContextClass().getName() +
					"] is not of type ConfigurablePortletApplicationContext");
		}

		ConfigurablePortletApplicationContext pac =
				(ConfigurablePortletApplicationContext) BeanUtils.instantiateClass(getContextClass());
		pac.setParent(parent);
		pac.setPortletContext(getPortletContext());
		pac.setPortletConfig(getPortletConfig());
		pac.setNamespace(getNamespace());
		pac.setConfigLocation(getContextConfigLocation());
		pac.addApplicationListener(new SourceFilteringListener(pac, this));

		postProcessPortletApplicationContext(pac);
		pac.refresh();

		return pac;
	}

	/**
	 * Post-process the given Portlet ApplicationContext before it is refreshed
	 * and activated as context for this portlet.
	 * <p>The default implementation is empty. <code>refresh()</code> will
	 * be called automatically after this method returns.
	 * @param pac the configured Portlet ApplicationContext (not refreshed yet)
	 * @see #createPortletApplicationContext
	 * @see ConfigurableApplicationContext#refresh()
	 */
	protected void postProcessPortletApplicationContext(ConfigurableApplicationContext pac) {
	}

	/**
	 * Return the PortletContext attribute name for this portlets's ApplicationContext.
	 * <p>The default implementation returns PORTLET_CONTEXT_PREFIX + portlet name.
	 * @see #PORTLET_CONTEXT_PREFIX
	 * @see #getPortletName
	 */
	public String getPortletContextAttributeName() {
		return PORTLET_CONTEXT_PREFIX + getPortletName();
	}

	/**
	 * Return this portlet's ApplicationContext.
	 */
	public final ApplicationContext getPortletApplicationContext() {
		return this.portletApplicationContext;
	}


	/**
	 * This method will be invoked after any bean properties have been set and
	 * the ApplicationContext has been loaded.
	 * <p>The default implementation is empty; subclasses may override this method
	 * to perform any initialization they require.
	 * @throws PortletException in case of an initialization exception
	 * @throws BeansException if thrown by ApplicationContext methods
	 */
	protected void initFrameworkPortlet() throws PortletException, BeansException {
	}

	/**
	 * Refresh this portlet's application context, as well as the
	 * dependent state of the portlet.
	 * @throws BeansException in case of errors
	 * @see #getPortletApplicationContext()
	 * @see org.springframework.context.ConfigurableApplicationContext#refresh()
	 */
	public void refresh() throws BeansException {
		ApplicationContext pac = getPortletApplicationContext();
		if (!(pac instanceof ConfigurableApplicationContext)) {
			throw new IllegalStateException("Portlet ApplicationContext does not support refresh: " + pac);
		}
		((ConfigurableApplicationContext) pac).refresh();
	}

	/**
	 * ApplicationListener endpoint that receives events from this servlet's
	 * WebApplicationContext.
	 * <p>The default implementation calls {@link #onRefresh} in case of a
	 * {@link org.springframework.context.event.ContextRefreshedEvent},
	 * triggering a refresh of this servlet's context-dependent state.
	 * @param event the incoming ApplicationContext event
	 */
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ContextRefreshedEvent) {
			this.refreshEventReceived = true;
			onRefresh(((ContextRefreshedEvent) event).getApplicationContext());
		}
	}

	/**
	 * Template method which can be overridden to add portlet-specific refresh work.
	 * Called after successful context refresh.
	 * <p>This implementation is empty.
	 * @param context the current Portlet ApplicationContext
	 * @throws BeansException in case of errors
	 * @see #refresh()
	 */
	protected void onRefresh(ApplicationContext context) throws BeansException {
		// For subclasses: do nothing by default.
	}


	/**
	 * Overridden for friendlier behavior in unit tests.
	 */
	protected String getTitle(RenderRequest renderRequest) {
		try {
			return super.getTitle(renderRequest);
		}
		catch (NullPointerException ex) {
			return getPortletName();
		}
	}

	/**
	 * Delegate render requests to processRequest/doRenderService.
	 */
	protected final void doDispatch(RenderRequest request, RenderResponse response)
			throws PortletException, IOException {

		processRequest(request, response);
	}

	/**
	 * Delegate action requests to processRequest/doActionService.
	 */
	public final void processAction(ActionRequest request, ActionResponse response)
			throws PortletException, IOException {

		processRequest(request, response);
	}
	
	/**
	 * Process this request, publishing an event regardless of the outcome.
	 * The actual event handling is performed by the abstract
	 * <code>doActionService()</code> and <code>doRenderService()</code> template methods.
	 * @see #doActionService
	 * @see #doRenderService
	 */
	protected final void processRequest(PortletRequest request, PortletResponse response)
			throws PortletException, IOException {

		long startTime = System.currentTimeMillis();
		Throwable failureCause = null;

		try {
			if (request instanceof ActionRequest) {
				doActionService((ActionRequest) request, (ActionResponse) response);
			}
			else {
				doRenderService((RenderRequest) request, (RenderResponse) response);
			}
		}
		catch (PortletException ex) {
			failureCause = ex;
			throw ex;
		}
		catch (IOException ex) {
			failureCause = ex;
			throw ex;
		}
		catch (Throwable ex) {
			failureCause = ex;
			throw new PortletException("Request processing failed", ex);
		}

		finally {
			if (failureCause != null) {
				logger.error("Could not complete request", failureCause);
			}
			else {
				logger.debug("Successfully completed request");
			}
			if (isPublishEvents()) {
				// Whether or not we succeeded, publish an event.
				long processingTime = System.currentTimeMillis() - startTime;
				this.portletApplicationContext.publishEvent(
						new PortletRequestHandledEvent(this,
								getPortletConfig().getPortletName(), request.getPortletMode().toString(),
								(request instanceof ActionRequest ? "action" : "render"),
								request.getRequestedSessionId(), getUsernameForRequest(request),
								processingTime, failureCause));
			}
		}
	}

	/**
	 * Determine the username for the given request.
	 * <p>The default implementation first tries the UserPrincipal.
	 * If that does not exist, then it checks the USER_INFO map.
	 * Can be overridden in subclasses.
	 * @param request current portlet request
	 * @return the username, or <code>null</code> if none found
	 * @see javax.portlet.PortletRequest#getUserPrincipal()
	 * @see javax.portlet.PortletRequest#getRemoteUser()
	 * @see javax.portlet.PortletRequest#USER_INFO
	 * @see #setUserinfoUsernameAttributes
	 */
	protected String getUsernameForRequest(PortletRequest request) {
		// Try the principal.
		Principal userPrincipal = request.getUserPrincipal();
		if (userPrincipal != null) {
			return userPrincipal.getName();
		}

		// Try the remote user name.
		String userName = request.getRemoteUser();
		if (userName != null) {
			return userName;
		}
		
		// Try the Portlet USER_INFO map.
		Map userInfo = (Map) request.getAttribute(PortletRequest.USER_INFO);
		if (userInfo != null) {
			for (int i = 0, n = this.userinfoUsernameAttributes.length; i < n; i++) {
				userName = (String) userInfo.get(this.userinfoUsernameAttributes[i]);
				if (userName != null) {
					return userName;
				}
			}
		}
		
		// Nothing worked...
		return null;
	}

	/**
	 * Subclasses must implement this method to do the work of render request handling.
	 * <p>The contract is essentially the same as that for the <code>doDispatch</code>
	 * method of GenericPortlet.
	 * <p>This class intercepts calls to ensure that exception handling and
	 * event publication takes place.
	 * @param request current render request
	 * @param response current render response
	 * @throws Exception in case of any kind of processing failure
	 * @see javax.portlet.GenericPortlet#doDispatch
	 */
	protected abstract void doRenderService(RenderRequest request, RenderResponse response)
			throws Exception;

	/**
	 * Subclasses must implement this method to do the work of action request handling.
	 * <p>The contract is essentially the same as that for the <code>processAction</code>
	 * method of GenericPortlet.
	 * <p>This class intercepts calls to ensure that exception handling and
	 * event publication takes place.
	 * @param request current action request
	 * @param response current action response
	 * @throws Exception in case of any kind of processing failure
	 * @see javax.portlet.GenericPortlet#processAction
	 */
	protected abstract void doActionService(ActionRequest request, ActionResponse response)
			throws Exception;


	/**
	 * Close the ApplicationContext of this portlet.
	 * @see org.springframework.context.ConfigurableApplicationContext#close()
	 */
	public void destroy() {
		getPortletContext().log("Destroying Spring FrameworkPortlet '" + getPortletName() + "'");
		if (this.portletApplicationContext instanceof ConfigurableApplicationContext) {
			((ConfigurableApplicationContext) this.portletApplicationContext).close();
		}
	}

}
