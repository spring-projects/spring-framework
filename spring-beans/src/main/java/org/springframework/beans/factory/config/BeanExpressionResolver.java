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

package org.springframework.beans.factory.config;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;

/**
 * Strategy interface for resolving a value by evaluating it as an expression,
 * if applicable.
 *
 * <p>A raw {@link org.springframework.beans.factory.BeanFactory} does not
 * contain a default implementation of this strategy. However,
 * {@link org.springframework.context.ApplicationContext} implementations
 * will provide expression support out of the box.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public interface BeanExpressionResolver {

	/**
	 * Evaluate the given value as an expression, if applicable;
	 * return the value as-is otherwise.
	 * @param value the value to evaluate as an expression
	 * @param beanExpressionContext the bean expression context to use when
	 * evaluating the expression
	 * @return the resolved value (potentially the given value as-is)
	 * @throws BeansException if evaluation failed
	 */
	@Nullable Object evaluate(@Nullable String value, BeanExpressionContext beanExpressionContext) throws BeansException;

}
