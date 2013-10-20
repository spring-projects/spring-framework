/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.mvc.support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.UriComponentsContributor;
import org.springframework.web.servlet.mvc.support.MvcUrlUtils.ControllerMethodValues;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriTemplate;

/**
 * A default {@link MvcUrls} implementation.
 *
 * @author Oliver Gierke
 * @author Rossen Stoyanchev
 *
 * @since 4.0
 */
public class DefaultMvcUrls implements MvcUrls {

	private static final ParameterNameDiscoverer parameterNameDiscoverer =
			new LocalVariableTableParameterNameDiscoverer();


	private final List<UriComponentsContributor> contributors = new ArrayList<UriComponentsContributor>();

	private final ConversionService conversionService;


	/**
	 * Create an instance providing a collection of {@link UriComponentsContributor}s or
	 * {@link HandlerMethodArgumentResolver}s. Since both of these tend to be implemented
	 * by the same class, the most convenient option is to obtain the configured
	 * {@code HandlerMethodArgumentResolvers} in the {@code RequestMappingHandlerAdapter}
	 * and provide that to this contstructor.
	 *
	 * @param uriComponentsContributors a collection of {@link UriComponentsContributor}
	 *        or {@link HandlerMethodArgumentResolver}s.
	 */
	public DefaultMvcUrls(Collection<?> uriComponentsContributors) {
		this(uriComponentsContributors, null);
	}

	/**
	 * Create an instance providing a collection of {@link UriComponentsContributor}s or
	 * {@link HandlerMethodArgumentResolver}s. Since both of these tend to be implemented
	 * by the same class, the most convenient option is to obtain the configured
	 * {@code HandlerMethodArgumentResolvers} in the {@code RequestMappingHandlerAdapter}
	 * and provide that to this contstructor.
	 * <p>
	 * If the {@link ConversionService} argument is {@code null},
	 * {@link DefaultFormattingConversionService} will be used by default.
	 *
	 * @param uriComponentsContributors a collection of {@link UriComponentsContributor}
	 *        or {@link HandlerMethodArgumentResolver}s.
	 * @param conversionService a ConversionService to use when method argument values
	 *        need to be formatted as Strings before being added to the URI
	 */
	public DefaultMvcUrls(Collection<?> uriComponentsContributors, ConversionService conversionService) {

		Assert.notNull(uriComponentsContributors, "'uriComponentsContributors' must not be null");

		for (Object contributor : uriComponentsContributors) {
			if (contributor instanceof UriComponentsContributor) {
				this.contributors.add((UriComponentsContributor) contributor);
			}
		}

		this.conversionService = (conversionService != null) ?
				conversionService : new DefaultFormattingConversionService();
	}


	@Override
	public UriComponentsBuilder linkToController(Class<?> controllerClass) {
		String mapping = MvcUrlUtils.getTypeLevelMapping(controllerClass);
		return ServletUriComponentsBuilder.fromCurrentServletMapping().path(mapping);
	}

	@Override
	public UriComponents linkToMethod(Method method, Object... argumentValues) {
		String mapping = MvcUrlUtils.getMethodMapping(method);
		UriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentServletMapping().path(mapping);
		Map<String, Object> uriVars = new HashMap<String, Object>();
		return applyContributers(builder, method, argumentValues, uriVars);
	}

	private UriComponents applyContributers(UriComponentsBuilder builder, Method method,
			Object[] argumentValues, Map<String, Object> uriVars) {

		if (this.contributors.isEmpty()) {
			return builder.buildAndExpand(uriVars);
		}

		int paramCount = method.getParameters().length;
		int argCount = argumentValues.length;

		Assert.isTrue(paramCount == argCount,  "Number of method parameters " + paramCount +
				" does not match number of argument values " + argCount);

		for (int i=0; i < paramCount; i++) {
			MethodParameter param = new MethodParameter(method, i);
			param.initParameterNameDiscovery(parameterNameDiscoverer);
			for (UriComponentsContributor c : this.contributors) {
				if (c.supportsParameter(param)) {
					c.contributeMethodArgument(param, argumentValues[i], builder, uriVars, this.conversionService);
					break;
				}
			}
		}

		return builder.buildAndExpand(uriVars);
	}

	@Override
	public UriComponents linkToMethodOn(Object mockController) {

		Assert.isInstanceOf(ControllerMethodValues.class, mockController);
		ControllerMethodValues controllerMethodValues = (ControllerMethodValues) mockController;

		Method method = controllerMethodValues.getControllerMethod();
		Object[] argumentValues = controllerMethodValues.getArgumentValues();

		Map<String, Object> uriVars = new HashMap<String, Object>();
		addTypeLevelUriVaris(controllerMethodValues, uriVars);

		String mapping = MvcUrlUtils.getMethodMapping(method);
		UriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentServletMapping().path(mapping);

		return applyContributers(builder, method, argumentValues, uriVars);
	}

	private void addTypeLevelUriVaris(ControllerMethodValues info, Map<String, Object> uriVariables) {

		Object[] values = info.getTypeLevelUriVariables();
		if (!ObjectUtils.isEmpty(values)) {

			String mapping = MvcUrlUtils.getTypeLevelMapping(info.getControllerMethod().getDeclaringClass());

			List<String> names = new UriTemplate(mapping).getVariableNames();
			Assert.isTrue(names.size() == values.length, "The provided type-level URI template variables " +
					Arrays.toString(values) + " do not match the template " + mapping);

			for (int i=0; i < names.size(); i++) {
				uriVariables.put(names.get(i), values[i]);
			}
		}
	}

}
