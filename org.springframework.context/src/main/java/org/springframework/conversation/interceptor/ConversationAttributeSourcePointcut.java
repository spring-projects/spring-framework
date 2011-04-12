/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.conversation.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.aop.support.StaticMethodMatcherPointcut;

/**
 * Pointcut implementation that matches for methods where conversation meta-data is available for. This class is a base
 * class for concrete Pointcut implementations that will provide the particular ConversationAttributeSource instance.
 *
 * @author Agim Emruli
 */
abstract class ConversationAttributeSourcePointcut extends StaticMethodMatcherPointcut implements Serializable {

	public boolean matches(Method method, Class<?> targetClass) {
		ConversationAttributeSource attributeSource = getConversationAttributeSource();
		return (attributeSource != null && attributeSource.getConversationAttribute(method, targetClass) != null);
	}

	/**
	 * @return - the ConversationAttributeSource instance that will be used to retrieve the conversation meta-data
	 */
	protected abstract ConversationAttributeSource getConversationAttributeSource();
}
