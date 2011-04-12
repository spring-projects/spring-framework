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

import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;

/**
 * Advisor implementation that advises beans if they contain conversation meta-data. Uses a
 * ConversationAttributeSourcePointcut to specify if the bean should be advised or not.
 *
 * @author Agim Emruli
 */
public class BeanFactoryConversationAttributeSourceAdvisor extends AbstractBeanFactoryPointcutAdvisor {

	private ConversationAttributeSource conversationAttributeSource;

	private Pointcut pointcut = new ConversationAttributeSourcePointcut() {
		@Override
		protected ConversationAttributeSource getConversationAttributeSource() {
			return conversationAttributeSource;
		}
	};

	/**
	 * Sets the ConversationAttributeSource instance that will be used to retrieve the ConversationDefinition meta-data.
	 * This instance will be used by the point-cut do specify if the target bean should be advised or not.
	 */
	public void setConversationAttributeSource(ConversationAttributeSource conversationAttributeSource) {
		this.conversationAttributeSource = conversationAttributeSource;
	}

	/**
	 * Returns the pointcut that will be used at runtime to test if the bean should be advised or not.
	 *
	 * @see org.springframework.conversation.interceptor.ConversationAttributeSourcePointcut
	 */
	public Pointcut getPointcut() {
		return pointcut;
	}
}
