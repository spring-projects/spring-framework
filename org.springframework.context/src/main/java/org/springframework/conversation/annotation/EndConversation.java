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

/**
 * This annotation can be placed on a method to end the current conversation. It
 * has the same effect as a manual invocation of
 * {@link org.springframework.conversation.ConversationManager#endCurrentConversation(ConversationEndingType)}.<br/>
 * The conversation is ended AFTER the method was invoked as an after-advice.
 * 
 * @author Micha Kiener
 * @since 3.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface EndConversation {

	/**
	 * If root is <code>true</code> which is the default, using this annotation will end a current conversation
	 * completely including its path up to the top root conversation. If declared as <code>false</code>, it will
	 * only end the current conversation, making its parent as the new current conversation.
	 * If the current conversation is not a nested or isolated conversation, the <code>root</code> parameter has
	 * no impact.
	 */
	boolean root() default true;
}
