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

package org.springframework.conversation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.conversation.Conversation;
import org.springframework.conversation.ConversationManager;
import org.springframework.conversation.ConversationType;
import org.springframework.conversation.interceptor.ConversationAttribute;

/**
 * This annotation can be placed on any method to start a new conversation. This has the same effect as invoking {@link
 * org.springframework.conversation.ConversationManager#beginConversation(boolean, JoinMode)} using <code>false</code>
 * for the temporary mode and the join mode as being specified within the annotation or {@link JoinMode#NEW} as the
 * default.<br/> The new conversation is always long running (not a temporary one) and is ended by either manually
 * invoke {@link ConversationManager#endCurrentConversation(ConversationEndingType)}, invoking the {@link
 * Conversation#end(ConversationEndingType)} method on the conversation itself or by placing the {@link EndConversation}
 * annotation on a method.<br/> The new conversation is created BEFORE the method itself is invoked as a before-advice.
 *
 * @author Micha Kiener
 * @author Agim Emruli
 * @since 3.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface BeginConversation {
	/**
	 * The conversation type declares how to start a new conversation and how to handle an existing current one.
	 * See {@link ConversationType} for more information.
	 */
	ConversationType value() default ConversationType.NEW;

	/** The timeout for this conversation in seconds. */
	int timeout() default ConversationAttribute.DEFAULT_TIMEOUT;
}

