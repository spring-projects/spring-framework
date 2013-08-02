/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.web.servlet.hypermedia;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.AnnotationAttribute;
import org.springframework.core.MethodParameter;
import org.springframework.core.MethodParameters;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.hypermedia.AnnotatedParametersParameterAccessor.BoundMethodParameter;
import org.springframework.web.servlet.hypermedia.RecordedInvocationUtils.LastInvocationAware;
import org.springframework.web.servlet.hypermedia.RecordedInvocationUtils.RecordedMethodInvocation;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
//import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriTemplate;

import static org.springframework.web.servlet.hypermedia.RecordedInvocationUtils.*;

/**
 * Builder to ease building {@link URI} instances pointing to Spring MVC controllers.
 * 
 * @author Oliver Gierke
 */
public class MvcUriComponentsBuilder extends UriComponentsBuilder {

	private static final AnnotationMappingDiscoverer DISCOVERER = new AnnotationMappingDiscoverer(
			RequestMapping.class);

	private static final AnnotatedParametersParameterAccessor PATH_VARIABLE_ACCESSOR = new AnnotatedParametersParameterAccessor(
			new AnnotationAttribute(PathVariable.class));

	private static final AnnotatedParametersParameterAccessor REQUEST_PARAM_ACCESSOR = new AnnotatedParametersParameterAccessor(
			new AnnotationAttribute(RequestParam.class));

	private final List<UriComponentsContributor> contributors;

	@Autowired(required = false)
	private RequestMappingHandlerAdapter adapter;

	/**
	 * Creates a new {@link LinkBuilderSupport} to grab the
	 * {@link UriComponentsContributor}s registered in the
	 * {@link RequestMappingHandlerAdapter}.
	 * 
	 * @param builder must not be {@literal null}.
	 */
	MvcUriComponentsBuilder() {

		SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
		List<UriComponentsContributor> contributors = new ArrayList<UriComponentsContributor>();

		if (adapter != null) {
			for (HandlerMethodArgumentResolver resolver : adapter.getArgumentResolvers()) {
				if (resolver instanceof UriComponentsContributor) {
					contributors.add((UriComponentsContributor) resolver);
				}
			}
		}

		this.contributors = contributors;
	}

	/**
	 * Creates a new {@link MvcUriComponentsBuilder} with a base of the mapping annotated
	 * to the given controller class.
	 * 
	 * @param controller the class to discover the annotation on, must not be
	 *        {@literal null}.
	 * @return
	 */
	public static UriComponentsBuilder from(Class<?> controller) {
		return from(controller, new Object[0]);
	}

	/**
	 * Creates a new {@link MvcUriComponentsBuilder} with a base of the mapping annotated
	 * to the given controller class. The additional parameters are used to fill up
	 * potentially available path variables in the class scop request mapping.
	 * 
	 * @param controller the class to discover the annotation on, must not be
	 *        {@literal null}.
	 * @param parameters additional parameters to bind to the URI template declared in the
	 *        annotation, must not be {@literal null}.
	 * @return
	 */
	public static UriComponentsBuilder from(Class<?> controller, Object... parameters) {

		Assert.notNull(controller);

		String mapping = DISCOVERER.getMapping(controller);
		UriTemplate template = new UriTemplate(mapping == null ? "/" : mapping);
		UriComponentsBuilder builder = UriComponentsBuilder.fromUri(template.expand(parameters));
		return getRootBuilder().with(builder);
	}

	public static UriComponentsBuilder from(Method method, Object... parameters) {
		MvcUriComponentsBuilder builder = new MvcUriComponentsBuilder();
		return from(method, parameters, builder.contributors);
	}

	static UriComponentsBuilder from(Method method, Object[] parameters,
			List<UriComponentsContributor> contributors) {

		UriTemplate template = new UriTemplate(DISCOVERER.getMapping(method));
		UriComponentsBuilder builder = UriComponentsBuilder.fromUri(template.expand(parameters));

		RecordedMethodInvocation invocation = getInvocation(method, parameters);
		UriComponentsBuilder appender = applyUriComponentsContributer(invocation,
				builder, contributors);

		return getRootBuilder().with(appender);
	}

	/**
	 * Creates a {@link MvcUriComponentsBuilder} pointing to a controller method. Hand in
	 * a dummy method invocation result you can create via
	 * {@link #methodOn(Class, Object...)} or
	 * {@link RecordedInvocationUtils#methodOn(Class, Object...)}.
	 * 
	 * <pre>
	 * @RequestMapping("/customers")
	 * class CustomerController {
	 * 
	 *   @RequestMapping("/{id}/addresses")
	 *   HttpEntity&lt;Addresses&gt; showAddresses(@PathVariable Long id) { … } 
	 * }
	 * 
	 * URI uri = linkTo(methodOn(CustomerController.class).showAddresses(2L)).toURI();
	 * </pre>
	 * 
	 * The resulting {@link URI} instance will point to {@code /customers/2/addresses}.
	 * For more details on the method invocation constraints, see
	 * {@link RecordedInvocationUtils#methodOn(Class, Object...)}.
	 * 
	 * @param invocationValue
	 * @return
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.hateoas.MethodLinkBuilderFactory#linkTo(java.lang.Object)
	 */
	public static UriComponentsBuilder from(Object invocationValue) {

		MvcUriComponentsBuilder builder = new MvcUriComponentsBuilder();
		return from(invocationValue, builder.contributors);
	}

	static UriComponentsBuilder from(Object invocationValue,
			List<? extends UriComponentsContributor> contributors) {

		Assert.isInstanceOf(LastInvocationAware.class, invocationValue);
		LastInvocationAware invocations = (LastInvocationAware) invocationValue;

		RecordedMethodInvocation invocation = invocations.getLastInvocation();
		Iterator<Object> classMappingParameters = invocations.getObjectParameters();
		Method method = invocation.getMethod();

		String mapping = DISCOVERER.getMapping(method);
		UriComponentsBuilder builder = getRootBuilder().path(mapping);

		UriTemplate template = new UriTemplate(mapping);
		Map<String, Object> values = new HashMap<String, Object>();

		Iterator<String> names = template.getVariableNames().iterator();
		while (classMappingParameters.hasNext()) {
			values.put(names.next(), classMappingParameters.next());
		}

		for (BoundMethodParameter parameter : PATH_VARIABLE_ACCESSOR.getBoundParameters(invocation)) {
			values.put(parameter.getVariableName(), parameter.asString());
		}

		for (BoundMethodParameter parameter : REQUEST_PARAM_ACCESSOR.getBoundParameters(invocation)) {

			Object value = parameter.getValue();
			String key = parameter.getVariableName();

			if (value instanceof Collection) {
				for (Object element : (Collection<?>) value) {
					builder.queryParam(key, element);
				}
			}
			else {
				builder.queryParam(key, parameter.asString());
			}
		}

		UriComponents components = applyUriComponentsContributer(invocation, builder,
				contributors).buildAndExpand(values);
		return UriComponentsBuilder.fromUri(components.toUri());
	}

	/**
	 * Wrapper for {@link RecordedInvocationUtils#methodOn(Class, Object...)} to be
	 * available in case you work with static imports of {@link MvcUriComponentsBuilder}.
	 * 
	 * @param controller must not be {@literal null}.
	 * @param parameters parameters to extend template variables in the type level
	 *        mapping.
	 * @return
	 */
	public static <T> T methodOn(Class<T> controller, Object... parameters) {
		return RecordedInvocationUtils.methodOn(controller, parameters);
	}

	/**
	 * Returns a {@link UriComponentsBuilder} obtained from the current servlet mapping
	 * with the host tweaked in case the request contains an {@code X-Forwarded-Host}
	 * header.
	 * 
	 * @return
	 */
	static UriComponentsBuilder getRootBuilder() {

		HttpServletRequest request = getCurrentRequest();
		UriComponentsBuilder builder = ServletUriComponentsBuilder.fromServletMapping(request);

		String header = request.getHeader("X-Forwarded-Host");

		if (!StringUtils.hasText(header)) {
			return builder;
		}

		String[] hosts = StringUtils.commaDelimitedListToStringArray(header);
		String hostToUse = hosts[0];

		if (hostToUse.contains(":")) {

			String[] hostAndPort = StringUtils.split(hostToUse, ":");

			builder.host(hostAndPort[0]);
			builder.port(Integer.parseInt(hostAndPort[1]));

		}
		else {
			builder.host(hostToUse);
		}

		return builder;
	}

	/**
	 * Applies the configured {@link UriComponentsContributor}s to the given
	 * {@link UriComponentsBuilder}.
	 * 
	 * @param builder will never be {@literal null}.
	 * @param invocation will never be {@literal null}.
	 * @return
	 */
	private static UriComponentsBuilder applyUriComponentsContributer(
			RecordedMethodInvocation invocation, UriComponentsBuilder builder,
			Collection<? extends UriComponentsContributor> contributors) {

		if (contributors.isEmpty()) {
			return builder;
		}

		MethodParameters parameters = new MethodParameters(invocation.getMethod());
		Iterator<Object> parameterValues = Arrays.asList(invocation.getArguments()).iterator();

		for (MethodParameter parameter : parameters.getParameters()) {
			Object parameterValue = parameterValues.next();
			for (UriComponentsContributor contributor : contributors) {
				if (contributor.supportsParameter(parameter)) {
					contributor.enhance(builder, parameter, parameterValue);
				}
			}
		}

		return builder;
	}

	/**
	 * Copy of {@link ServletUriComponentsBuilder#getCurrentRequest()} until SPR-10110
	 * gets fixed.
	 * 
	 * @return
	 */
	private static HttpServletRequest getCurrentRequest() {

		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		Assert.state(requestAttributes != null,
				"Could not find current request via RequestContextHolder");
		Assert.isInstanceOf(ServletRequestAttributes.class, requestAttributes);
		HttpServletRequest servletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
		Assert.state(servletRequest != null, "Could not find current HttpServletRequest");
		return servletRequest;
	}
}
