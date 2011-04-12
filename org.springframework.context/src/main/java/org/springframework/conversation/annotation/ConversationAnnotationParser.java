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

import java.lang.reflect.AnnotatedElement;

import org.springframework.conversation.interceptor.ConversationAttribute;

/**
 * Parser interface that resolver the concrete conversation annotation from an AnnotatedElement.
 *
 * @author Agim Emruli
 */
interface ConversationAnnotationParser {

	/**
	 * This method returns the ConversationAttribute for a particular AnnotatedElement which can be a method or class at
	 * all.
	 */
	ConversationAttribute parseConversationAnnotation(AnnotatedElement annotatedElement);

}
