/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.expression;

/**
 * A bean resolver can be registered with the evaluation context and will kick in
 * for bean references: {@code @myBeanName} and {@code &myBeanName} expressions.
 * The {@code &} variant syntax allows access to the factory bean where relevant.
 *
 * @author Andy Clement
 * @since 3.0.3
 */
public interface BeanResolver {

	/**
	 * Look up a bean by the given name and return a corresponding instance for it.
	 * For attempting access to a factory bean, the name needs a {@code &} prefix.
	 * @param context the current evaluation context
	 * @param beanName the name of the bean to look up
	 * @return an object representing the bean
	 * @throws AccessException if there is an unexpected problem resolving the bean
	 */
	Object resolve(EvaluationContext context, String beanName) throws AccessException;

}
