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

import org.springframework.conversation.ConversationType;

/**
 * ConversationDefinition Attributes that will be used by the AOP interceptor to start and end conversation before and
 * after methods. This attributes are not in the conversation definition itself because these attributes are only
 * relevant for a interceptor based approach to handle conversations.
 *
 * @author Agim Emruli
 */
public interface ConversationAttribute {

	/**
	 * The default timeout for a conversation, means there is no timeout defined for the conversation. It is up to the
	 * concrete conversation manager implementation to handle conversation without a timeout, like a indefinite
	 * conversation or some other system specific timeout
	 */
	int DEFAULT_TIMEOUT = -1;

	/**
	 * Defines if the a conversation should be started before the interceptor delegates to the target (like a method
	 * invocation). This can be a short-running conversation where the method is the whole life-cycle of a conversation or
	 * a long-running conversation where the conversation will be started but not stopped while calling the target.
	 *
	 * @return if the conversation should be started
	 */
	boolean shouldStartConversation();

	/**
	 * Defines if the a conversation should be ended after the interceptor delegates to the target (like a method
	 * invocation). The stopped that should be stopped after the call to the target can be a short-running conversation
	 * that has been start before the method call or a long-running conversation that has been started on some other method
	 * call before in the life-cycle of the application.
	 *
	 * @return if the conversation should be ended
	 */
	boolean shouldEndConversation();

	boolean shouldEndRoot();

	/**
	 * Returns the type used to start a new conversation. See {@link org.springframework.conversation.ConversationType}
	 * for a more detailed description of the different types available.<br/>
	 * Default type is {@link org.springframework.conversation.ConversationType#NEW} which will create a new root
	 * conversation and will automatically end the current one, if not ended before.
	 *
	 * @return the conversation type to use for creating a new conversation
	 */
	ConversationType getConversationType();

	/**
	 * Returns the timeout to be set within the newly created conversation, default is <code>-1</code> which means to use
	 * the default timeout as being configured on the {@link org.springframework.conversation.ConversationManager}.
	 * A value of <code>0</code> means there is no timeout any other positive value is interpreted as a timeout in
	 * milliseconds.
	 *
	 * @return the timeout in milliseconds to be set on the new conversation
	 */
	int getTimeout();
}
