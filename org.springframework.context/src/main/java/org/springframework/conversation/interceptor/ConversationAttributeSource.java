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

import java.lang.reflect.Method;

/**
 * Interface used by the ConversationInterceptor to retrieve the meta-data for the particular conversation. The
 * meta-data can be provided by any implementation which is capable to return a ConversationAttribute instance based on
 * a method and class. The implementation could be a annotation-based one or a XML-based implementation that retrieves
 * the meta-data through a XML-configuration.
 *
 * @author Agim Emruli
 */
public interface ConversationAttributeSource {

	/**
	 * Resolves the ConversatioNAttribute for a particular method if available. This method must return null if there are
	 * no ConversationAttribute meta-data available for one particular method. It is up to the implementation to look for
	 * alternative sources like class-level annotations that applies to all methods inside a particular class.
	 *
	 * @param method The method for which the ConversationAttribute should be returned.
	 * @param targetClass The target class where the implementation should look for.
	 * @return the conversation attributes if available for the method, otherwise null.
	 */
	ConversationAttribute getConversationAttribute(Method method, Class<?> targetClass);
}
