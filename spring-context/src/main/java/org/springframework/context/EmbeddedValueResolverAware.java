/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.context;

import java.util.Arrays;
import java.util.Set;

import org.springframework.beans.factory.Aware;
import org.springframework.util.StringValueResolver;

/**
 * Interface to be implemented by any object that wishes to be notified of a
 * {@code StringValueResolver} for the resolution of embedded definition values.
 *
 * <p>This is an alternative to a full ConfigurableBeanFactory dependency via the
 * {@code ApplicationContextAware}/{@code BeanFactoryAware} interfaces.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Jianbin Chen
 * @since 3.0.3
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#resolveEmbeddedValue(String)
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#getBeanExpressionResolver()
 * @see org.springframework.beans.factory.config.EmbeddedValueResolver
 */
public interface EmbeddedValueResolverAware extends Aware {

	/**
	 * Set the StringValueResolver to use for resolving embedded definition values.
	 */
	void setEmbeddedValueResolver(StringValueResolver resolver);

	/**
	 * resolve Origin Or Pattern Value
	 * @param origin origin
	 * @param origins origins
	 */
	default void resolveOriginOrPatternValue(String origin, Set<String> origins) {
		int bracketIndex = origin.indexOf("[");
		int commaIndex = origin.indexOf(",");
		int lastIndex = origin.lastIndexOf(",");
		if (lastIndex != commaIndex) {
			if (bracketIndex < commaIndex) {
				int index = origin.indexOf("]");
				origins.add(origin.substring(0, ++index));
				origin = origin.substring(origin.indexOf("]") + 2);
			}
			else {
				int index = origin.indexOf(",");
				String value = origin.substring(0, ++index);
				origins.add(value.substring(0, value.length() - 1));
				origin = origin.substring(origin.indexOf(",") + 1);
			}
			resolveOriginOrPatternValue(origin, origins);
		}
		else {
			if (bracketIndex == -1) {
				origins.addAll(Arrays.asList(origin.split(",")));
			}
			else {
				origins.add(origin);
			}
		}
	}
}
