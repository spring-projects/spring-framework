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

package org.springframework.web.method.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * A {@link UriComponentsContributor} containing a list of other contributors
 * to delegate to and also encapsulating a specific {@link ConversionService} to
 * use for formatting method argument values as Strings.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
public class CompositeUriComponentsContributor implements UriComponentsContributor {

	private final List<Object> contributors;

	private final ConversionService conversionService;


	/**
	 * Create an instance from a collection of {@link UriComponentsContributor UriComponentsContributors} or
	 * {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}. Since both of these tend to be implemented
	 * by the same class, the most convenient option is to obtain the configured
	 * {@code HandlerMethodArgumentResolvers} in {@code RequestMappingHandlerAdapter}
	 * and provide that to this constructor.
	 * @param contributors a collection of {@link UriComponentsContributor}
	 * or {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}
	 */
	public CompositeUriComponentsContributor(UriComponentsContributor... contributors) {
		this.contributors = Arrays.asList((Object[]) contributors);
		this.conversionService = new DefaultFormattingConversionService();
	}

	/**
	 * Create an instance from a collection of {@link UriComponentsContributor UriComponentsContributors} or
	 * {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}. Since both of these tend to be implemented
	 * by the same class, the most convenient option is to obtain the configured
	 * {@code HandlerMethodArgumentResolvers} in {@code RequestMappingHandlerAdapter}
	 * and provide that to this constructor.
	 * @param contributors a collection of {@link UriComponentsContributor}
	 * or {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}
	 */
	public CompositeUriComponentsContributor(Collection<?> contributors) {
		this(contributors, null);
	}

	/**
	 * Create an instance from a collection of {@link UriComponentsContributor UriComponentsContributors} or
	 * {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}. Since both of these tend to be implemented
	 * by the same class, the most convenient option is to obtain the configured
	 * {@code HandlerMethodArgumentResolvers} in the {@code RequestMappingHandlerAdapter}
	 * and provide that to this constructor.
	 * <p>If the {@link ConversionService} argument is {@code null},
	 * {@link org.springframework.format.support.DefaultFormattingConversionService}
	 * will be used by default.
	 * @param contributors a collection of {@link UriComponentsContributor}
	 * or {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}
	 * @param cs a ConversionService to use when method argument values
	 * need to be formatted as Strings before being added to the URI
	 */
	public CompositeUriComponentsContributor(@Nullable Collection<?> contributors, @Nullable ConversionService cs) {
		this.contributors = (contributors != null ? new ArrayList<>(contributors) : Collections.emptyList());
		this.conversionService = (cs != null ? cs : new DefaultFormattingConversionService());
	}

	/**
	 * Determine if this {@code CompositeUriComponentsContributor} has any
	 * contributors.
	 * @return {@code true} if this {@code CompositeUriComponentsContributor}
	 * was created with contributors to delegate to
	 */
	public boolean hasContributors() {
		return !this.contributors.isEmpty();
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		for (Object contributor : this.contributors) {
			if (contributor instanceof UriComponentsContributor ucc) {
				if (ucc.supportsParameter(parameter)) {
					return true;
				}
			}
			else if (contributor instanceof HandlerMethodArgumentResolver resolver) {
				if (resolver.supportsParameter(parameter)) {
					return false;
				}
			}
		}
		return false;
	}

	@Override
	public void contributeMethodArgument(MethodParameter parameter, Object value,
			UriComponentsBuilder builder, Map<String, Object> uriVariables, ConversionService conversionService) {

		for (Object contributor : this.contributors) {
			if (contributor instanceof UriComponentsContributor ucc) {
				if (ucc.supportsParameter(parameter)) {
					ucc.contributeMethodArgument(parameter, value, builder, uriVariables, conversionService);
					break;
				}
			}
			else if (contributor instanceof HandlerMethodArgumentResolver resolver) {
				if (resolver.supportsParameter(parameter)) {
					break;
				}
			}
		}
	}

	/**
	 * An overloaded method that uses the ConversionService created at construction.
	 */
	public void contributeMethodArgument(MethodParameter parameter, Object value, UriComponentsBuilder builder,
			Map<String, Object> uriVariables) {

		contributeMethodArgument(parameter, value, builder, uriVariables, this.conversionService);
	}

}
