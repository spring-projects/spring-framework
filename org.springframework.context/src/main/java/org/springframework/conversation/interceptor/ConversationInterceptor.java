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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.conversation.Conversation;
import org.springframework.conversation.ConversationManager;

/**
 * MethodInterceptor that manages conversations based on ConversationAttribute meta-data.
 *
 * @author Agim Emruli
 */
public class ConversationInterceptor implements MethodInterceptor {

	private ConversationManager conversationManager;

	private ConversationAttributeSource conversationAttributeSource;

	/**
	 * Sets the ConversationManager implementation that will be used to actually handle the conversations.
	 *
	 * @see org.springframework.conversation.manager.DefaultConversationManager
	 */
	public void setConversationManager(ConversationManager conversationManager) {
		this.conversationManager = conversationManager;
	}

	/**
	 * Sets the ConversationAttributeSource that will be used to retrieve the meta-data for one particular method at
	 * runtime.
	 *
	 * @see org.springframework.conversation.annotation.AnnotationConversationAttributeSource
	 */
	public void setConversationAttributeSource(ConversationAttributeSource conversationAttributeSource) {
		this.conversationAttributeSource = conversationAttributeSource;
	}

	/**
	 * Advice implementations that actually handles the conversations. This method retrieves and consults the
	 * ConversationAttribute at runtime and performs the particular actions before and after the target method call.
	 *
	 * @param invocation The MethodInvocation that represents the context object for this interceptor.
	 */
	public Object invoke(MethodInvocation invocation) throws Throwable {

		Class targetClass = (invocation.getThis() != null ? invocation.getThis().getClass() : null);

		ConversationAttribute conversationAttribute =
				conversationAttributeSource.getConversationAttribute(invocation.getMethod(), targetClass);

		Object returnValue;
		try {

			if (conversationAttribute != null && conversationAttribute.shouldStartConversation()) {
				Conversation conversation =
						conversationManager.beginConversation(conversationAttribute.getConversationType());
				if (conversationAttribute.getTimeout() != ConversationAttribute.DEFAULT_TIMEOUT) {
					conversation.setTimeout(conversationAttribute.getTimeout());
				}
			}

			returnValue = invocation.proceed();

			if (conversationAttribute != null && conversationAttribute.shouldEndConversation()) {
				conversationManager.endCurrentConversation(conversationAttribute.shouldEndRoot());
			}

		}
		catch (Throwable th) {
			if (conversationAttribute != null) {
				conversationManager.endCurrentConversation(conversationAttribute.shouldEndRoot());
			}
			throw th;
		}

		return returnValue;
	}
}
