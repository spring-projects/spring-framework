/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.messaging.handler;

import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;

/**
 * Represents a Spring-managed bean with cross-cutting functionality to be
 * applied to one or more Spring beans with annotation-based message
 * handling methods.
 *
 * <p>Component stereotypes such as
 * {@link org.springframework.stereotype.Controller @Controller} with annotation
 * handler methods often need cross-cutting functionality across all or a subset
 * of such annotated components. A primary example of this is the need for "global"
 * annotated exception handler methods but the concept applies more generally.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public interface MessagingAdviceBean extends Ordered {

	/**
	 * Return the type of the contained advice bean.
	 * <p>If the bean type is a CGLIB-generated class, the original user-defined
	 * class is returned.
	 */
	@Nullable
	Class<?> getBeanType();

	/**
	 * Return the advice bean instance, if necessary resolving a bean specified
	 * by name through the BeanFactory.
	 */
	Object resolveBean();

	/**
	 * Whether this {@link MessagingAdviceBean} applies to the given bean type.
	 * @param beanType the type of the bean to check
	 */
	boolean isApplicableToBeanType(Class<?> beanType);

}
