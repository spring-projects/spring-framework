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

package org.springframework.web.method.support;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A {@link UriComponentsContributor} containing a list of other contributors
 * to delegate and also encapsulating a specific {@link ConversionService} to
 * use for formatting method argument values to Strings.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class CompositeUriComponentsContributor implements UriComponentsContributor {

	private final List<UriComponentsContributor> contributors = new ArrayList<UriComponentsContributor>();

	private final ConversionService conversionService;


	/**
	 * Create an instance from a collection of {@link UriComponentsContributor}s or
	 * {@link HandlerMethodArgumentResolver}s. Since both of these tend to be implemented
	 * by the same class, the most convenient option is to obtain the configured
	 * {@code HandlerMethodArgumentResolvers} in {@code RequestMappingHandlerAdapter} and
	 * provide that to this constructor.
	 * @param contributors a collection of {@link UriComponentsContributor}
	 *        or {@link HandlerMethodArgumentResolver}s.
	 */
	public CompositeUriComponentsContributor(Collection<?> contributors) {
		this(contributors, null);
	}

	/**
	 * Create an instance from a collection of {@link UriComponentsContributor}s or
	 * {@link HandlerMethodArgumentResolver}s. Since both of these tend to be implemented
	 * by the same class, the most convenient option is to obtain the configured
	 * {@code HandlerMethodArgumentResolvers} in the {@code RequestMappingHandlerAdapter}
	 * and provide that to this constructor.
	 * <p>If the {@link ConversionService} argument is {@code null},
	 * {@link org.springframework.format.support.DefaultFormattingConversionService}
	 * will be used by default.
	 * @param contributors a collection of {@link UriComponentsContributor}
	 * or {@link HandlerMethodArgumentResolver}s.
	 * @param conversionService a ConversionService to use when method argument values
	 * need to be formatted as Strings before being added to the URI
	 */
	public CompositeUriComponentsContributor(Collection<?> contributors, ConversionService conversionService) {

		Assert.notNull(contributors, "'uriComponentsContributors' must not be null");

		for (Object contributor : contributors) {
			if (contributor instanceof UriComponentsContributor) {
				this.contributors.add((UriComponentsContributor) contributor);
			}
		}

		this.conversionService = (conversionService != null) ?
				conversionService : new DefaultFormattingConversionService();
	}


	public boolean hasContributors() {
		return this.contributors.isEmpty();
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		for (UriComponentsContributor c : this.contributors) {
			if (c.supportsParameter(parameter)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void contributeMethodArgument(MethodParameter parameter, Object value,
			UriComponentsBuilder builder, Map<String, Object> uriVariables, ConversionService conversionService) {

		for (UriComponentsContributor c : this.contributors) {
			if (c.supportsParameter(parameter)) {
				c.contributeMethodArgument(parameter, value, builder, uriVariables, conversionService);
				break;
			}
		}
	}

	/**
	 * An overloaded method that uses the ConversionService created at construction.
	 */
	public void contributeMethodArgument(MethodParameter parameter, Object value, UriComponentsBuilder builder,
			Map<String, Object> uriVariables) {

		this.contributeMethodArgument(parameter, value, builder, uriVariables, this.conversionService);
	}

}
