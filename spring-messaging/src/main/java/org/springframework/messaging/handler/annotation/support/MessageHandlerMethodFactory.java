/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
