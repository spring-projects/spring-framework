/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.web.portlet.mvc.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.portlet.ClientDataRequest;
import javax.portlet.Event;
import javax.portlet.EventRequest;
import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.PortletRequest;
import javax.portlet.ResourceRequest;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.Mapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.portlet.bind.PortletRequestBindingException;
import org.springframework.web.portlet.bind.annotation.EventMapping;
import org.springframework.web.portlet.bind.annotation.ResourceMapping;
import org.springframework.web.portlet.handler.AbstractMapBasedHandlerMapping;
import org.springframework.web.portlet.handler.PortletRequestMethodNotSupportedException;

/**
 * Implementation of the {@link org.springframework.web.portlet.HandlerMapping}
 * interface that maps handlers based on portlet modes expressed through the
 * {@link RequestMapping} annotation at the type or method level.
 *
 * <p>Registered by default in {@link org.springframework.web.portlet.DispatcherPortlet}
 * on Java 5+. <b>NOTE:</b> If you define custom HandlerMapping beans in your
 * DispatcherPortlet context, you need to add a DefaultAnnotationHandlerMapping bean
 * explicitly, since custom HandlerMapping beans replace the default mapping strategies.
 * Defining a DefaultAnnotationHandlerMapping also allows for registering custom
 * interceptors:
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.web.portlet.mvc.annotation.DefaultAnnotationHandlerMapping"&gt;
 *   &lt;property name="interceptors"&gt;
 *     ...
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * Annotated controllers are usually marked with the {@link Controller} stereotype
 * at the type level. This is not strictly necessary when {@link RequestMapping} is
 * applied at the type level (since such a handler usually implements the
 * {@link org.springframework.web.portlet.mvc.Controller} interface). However,
 * {@link Controller} is required for detecting {@link RequestMapping} annotations
 * at the method level.
 *
 * <p><b>NOTE:</b> Method-level mappings are only allowed to narrow the mapping
 * expressed at the class level (if any). A portlet mode in combination with specific
 * parameter conditions needs to uniquely map onto one specific handler bean,
 * not spread across multiple handler beans. It is strongly recommended to
 * co-locate related handler methods into the same bean.
 *
 * <p>The {@link AnnotationMethodHandlerAdapter} is responsible for processing
 * annotated handler methods, as mapped by this HandlerMapping. For
 * {@link RequestMapping} at the type level, specific HandlerAdapters such as
 * {@link org.springframework.web.portlet.mvc.SimpleControllerHandlerAdapter} apply.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see RequestMapping
 * @see AnnotationMethodHandlerAdapter
 */
public class DefaultAnnotationHandlerMapping extends AbstractMapBasedHandlerMapping<PortletMode> {

	private final Map<Class, RequestMapping> cachedMappings = new HashMap<Class, RequestMapping>();


	/**
	 * Calls the <code>registerHandlers</code> method in addition
	 * to the superclass's initialization.
	 * @see #detectHandlers
	 */
	@Override
	public void initApplicationContext() throws BeansException {
		super.initApplicationContext();
		detectHandlers();
	}

	/**
	 * Register all handlers specified in the Portlet mode map for the corresponding modes.
	 * @throws org.springframework.beans.BeansException if the handler couldn't be registered
	 */
	protected void detectHandlers() throws BeansException {
		ApplicationContext context = getApplicationContext();
		String[] beanNames = context.getBeanNamesForType(Object.class);
		for (String beanName : beanNames) {
			Class<?> handlerType = context.getType(beanName);
			RequestMapping mapping = context.findAnnotationOnBean(beanName, RequestMapping.class);
			if (mapping != null) {
				// @RequestMapping found at type level
				this.cachedMappings.put(handlerType, mapping);
				String[] modeKeys = mapping.value();
				String[] params = mapping.params();
				boolean registerHandlerType = true;
				if (modeKeys.length == 0 || params.length == 0) {
					registerHandlerType = !detectHandlerMethods(handlerType, beanName, mapping);
				}
				if (registerHandlerType) {
					ParameterMappingPredicate predicate = new ParameterMappingPredicate(
							params, mapping.headers(), mapping.method());
					for (String modeKey : modeKeys) {
						registerHandler(new PortletMode(modeKey), beanName, predicate);
					}
				}
			}
			else if (AnnotationUtils.findAnnotation(handlerType, Controller.class) != null) {
				detectHandlerMethods(handlerType, beanName, mapping);
			}
		}
	}

	/**
	 * Derive portlet mode mappings from the handler's method-level mappings.
	 * @param handlerType the handler type to introspect
	 * @param beanName the name of the bean introspected
	 * @param typeMapping the type level mapping (if any)
	 * @return <code>true</code> if at least 1 handler method has been registered;
	 * <code>false</code> otherwise
	 */
	protected boolean detectHandlerMethods(Class<?> handlerType, final String beanName, final RequestMapping typeMapping) {
		final Set<Boolean> handlersRegistered = new HashSet<Boolean>(1);
		Set<Class<?>> handlerTypes = new LinkedHashSet<Class<?>>();
		handlerTypes.add(handlerType);
		handlerTypes.addAll(Arrays.asList(handlerType.getInterfaces()));
		for (Class<?> currentHandlerType : handlerTypes) {
			ReflectionUtils.doWithMethods(currentHandlerType, new ReflectionUtils.MethodCallback() {
				public void doWith(Method method) {
					boolean mappingFound = false;
					String[] modeKeys = new String[0];
					String[] params = new String[0];
					String resourceId = null;
					String eventName = null;
					for (Annotation ann : method.getAnnotations()) {
						if (AnnotationUtils.findAnnotation(ann.getClass(), Mapping.class) != null) {
							mappingFound = true;
							if (ann instanceof RequestMapping) {
								RequestMapping rm = (RequestMapping) ann;
								modeKeys = rm.value();
								params = StringUtils.mergeStringArrays(params, rm.params());
							}
							else if (ann instanceof ResourceMapping) {
								ResourceMapping rm = (ResourceMapping) ann;
								resourceId = rm.value();
							}
							else if (ann instanceof EventMapping) {
								EventMapping em = (EventMapping) ann;
								eventName = em.value();
							}
							else {
								String[] specificParams = (String[]) AnnotationUtils.getValue(ann, "params");
								params = StringUtils.mergeStringArrays(params, specificParams);
							}
						}
					}
					if (mappingFound) {
						if (modeKeys.length == 0) {
							if (typeMapping != null) {
								modeKeys = typeMapping.value();
							}
							else {
								throw new IllegalStateException(
										"No portlet mode mappings specified - neither at type nor at method level");
							}
						}
						if (typeMapping != null) {
							if (!PortletAnnotationMappingUtils.validateModeMapping(modeKeys, typeMapping.value())) {
								throw new IllegalStateException("Mode mappings conflict between method and type level: " +
										Arrays.asList(modeKeys) + " versus " + Arrays.asList(typeMapping.value()));
							}
							params = StringUtils.mergeStringArrays(typeMapping.params(), params);
						}
						PortletRequestMappingPredicate predicate;
						if (resourceId != null) {
							predicate = new ResourceMappingPredicate(resourceId);
						}
						else if (eventName != null) {
							predicate = new EventMappingPredicate(eventName);
						}
						else {
							predicate = new ParameterMappingPredicate(params);
						}
						for (String modeKey : modeKeys) {
							registerHandler(new PortletMode(modeKey), beanName, predicate);
							handlersRegistered.add(Boolean.TRUE);
						}
					}
				}
			}, ReflectionUtils.USER_DECLARED_METHODS);
		}
		return !handlersRegistered.isEmpty();
	}

	/**
	 * Uses the current PortletMode as lookup key.
	 */
	@Override
	protected PortletMode getLookupKey(PortletRequest request) throws Exception {
		return request.getPortletMode();
	}

	/**
	 * Validate the given annotated handler against the current request.
	 * @see #validateMapping
	 */
	protected void validateHandler(Object handler, PortletRequest request) throws Exception {
		RequestMapping mapping = this.cachedMappings.get(handler.getClass());
		if (mapping == null) {
			mapping = AnnotationUtils.findAnnotation(handler.getClass(), RequestMapping.class);
		}
		if (mapping != null) {
			validateMapping(mapping, request);
		}
	}

	/**
	 * Validate the given type-level mapping metadata against the current request,
	 * checking HTTP request method and parameter conditions.
	 * @param mapping the mapping metadata to validate
	 * @param request current HTTP request
	 * @throws Exception if validation failed
	 */
	protected void validateMapping(RequestMapping mapping, PortletRequest request) throws Exception {
		RequestMethod[] mappedMethods = mapping.method();
		if (!PortletAnnotationMappingUtils.checkRequestMethod(mappedMethods, request)) {
			String[] supportedMethods = new String[mappedMethods.length];
			for (int i = 0; i < mappedMethods.length; i++) {
				supportedMethods[i] = mappedMethods[i].name();
			}
			if (request instanceof ClientDataRequest) {
				throw new PortletRequestMethodNotSupportedException(((ClientDataRequest) request).getMethod(), supportedMethods);
			}
			else {
				throw new PortletRequestMethodNotSupportedException(supportedMethods);
			}
		}

		String[] mappedHeaders = mapping.headers();
		if (!PortletAnnotationMappingUtils.checkHeaders(mappedHeaders, request)) {
			throw new PortletRequestBindingException("Header conditions \"" +
					StringUtils.arrayToDelimitedString(mappedHeaders, ", ") +
					"\" not met for actual request");
		}
	}


	/**
	 * Predicate that matches against parameter conditions.
	 */
	private static class ParameterMappingPredicate implements PortletRequestMappingPredicate {

		private final String[] params;

		private final String[] headers;

		private final Set<String> methods = new HashSet<String>();

		public ParameterMappingPredicate(String[] params) {
			this(params, null, null);
		}

		public ParameterMappingPredicate(String[] params, String[] headers, RequestMethod[] methods) {
			this.params = params;
			this.headers = headers;
			if (methods != null) {
				for (RequestMethod method : methods) {
					this.methods.add(method.name());
				}
			}
		}

		public boolean match(PortletRequest request) {
			return PortletAnnotationMappingUtils.checkParameters(this.params, request);
		}

		public void validate(PortletRequest request) throws PortletException {
			if (!PortletAnnotationMappingUtils.checkHeaders(this.headers, request)) {
				throw new PortletRequestBindingException("Header conditions \"" +
						StringUtils.arrayToDelimitedString(this.headers, ", ") +
						"\" not met for actual request");
			}

			if (!this.methods.isEmpty()) {
				if (!(request instanceof ClientDataRequest)) {
					throw new PortletRequestMethodNotSupportedException(StringUtils.toStringArray(this.methods));
				}
				String method = ((ClientDataRequest) request).getMethod();
				if (!this.methods.contains(method)) {
					throw new PortletRequestMethodNotSupportedException(method, StringUtils.toStringArray(this.methods));
				}
			}
		}

		public int compareTo(Object other) {
			if (other instanceof ParameterMappingPredicate) {
				return new Integer(((ParameterMappingPredicate) other).params.length).compareTo(this.params.length);
			}
			else {
				return 1;
			}
		}

		@Override
		public String toString() {
			return StringUtils.arrayToCommaDelimitedString(this.params);
		}
	}


	private static class ResourceMappingPredicate implements PortletRequestMappingPredicate {

		private final String resourceId;

		public ResourceMappingPredicate(String resourceId) {
			this.resourceId = resourceId;
		}

		public boolean match(PortletRequest request) {
			return (PortletRequest.RESOURCE_PHASE.equals(request.getAttribute(PortletRequest.LIFECYCLE_PHASE)) &&
					("".equals(this.resourceId) || this.resourceId.equals(((ResourceRequest) request).getResourceID())));
		}

		public void validate(PortletRequest request) {
		}

		public int compareTo(Object o) {
			return -1;
		}
	}


	private static class EventMappingPredicate implements PortletRequestMappingPredicate {

		private final String eventName;

		public EventMappingPredicate(String eventName) {
			this.eventName = eventName;
		}

		public boolean match(PortletRequest request) {
			if (!PortletRequest.EVENT_PHASE.equals(request.getAttribute(PortletRequest.LIFECYCLE_PHASE))) {
				return false;
			}
			if ("".equals(this.eventName)) {
				return true;
			}
			Event event = ((EventRequest) request).getEvent();
			return (this.eventName.equals(event.getName()) || this.eventName.equals(event.getQName().toString()));
		}

		public void validate(PortletRequest request) {
		}

		public int compareTo(Object o) {
			return -1;
		}
	}

}
