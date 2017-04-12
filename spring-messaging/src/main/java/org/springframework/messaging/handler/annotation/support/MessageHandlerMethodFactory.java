/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.messaging.handler.annotation.support;

import java.lang.reflect.Method;

import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;

/**
 * A factory for {@link InvocableHandlerMethod} that is suitable to process
 * an incoming {@link org.springframework.messaging.Message}
 *
 * <p>Typically used by listener endpoints that require a flexible method
 * signature.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public interface MessageHandlerMethodFactory {

	/**
	 * Create the {@link InvocableHandlerMethod} that is able to process the specified
	 * method endpoint.
	 * @param bean the bean instance
	 * @param method the method to invoke
	 * @return an {@link InvocableHandlerMethod} suitable for that method
	 */
	InvocableHandlerMethod createInvocableHandlerMethod(Object bean, Method method);

}
