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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.aop.support.AopUtils;
import org.springframework.conversation.interceptor.ConversationAttribute;
import org.springframework.conversation.interceptor.ConversationAttributeSource;
import org.springframework.util.Assert;

/**
 * ConversationAttributeSource implementation that uses annotation meta-data to provide a ConversationAttribute instance
 * for a particular method.
 *
 * @author Agim Emruli
 */
public class AnnotationConversationAttributeSource implements ConversationAttributeSource {

	private final Set<ConversationAnnotationParser> conversationAnnotationParsers;

	/**
	 * Default constructor that uses the Spring standard ConversationAnnotationParser instances to parse the annotation
	 * meta-data.
	 *
	 * @see org.springframework.conversation.annotation.BeginConversationAnnotationParser
	 * @see org.springframework.conversation.annotation.EndConversationAnnotationParser
	 * @see org.springframework.conversation.annotation.ConversationAnnotationParser
	 */
	public AnnotationConversationAttributeSource() {
		Set<ConversationAnnotationParser> defaultParsers = new LinkedHashSet<ConversationAnnotationParser>();
		Collections
				.addAll(defaultParsers, new BeginConversationAnnotationParser(), new EndConversationAnnotationParser());
		conversationAnnotationParsers = defaultParsers;
	}

	/**
	 * Constructor that uses the custom ConversationAnnotationParser to parse the annotation meta-data.
	 *
	 * @param conversationAnnotationParsers The ConversationAnnotationParser instance that will be used to parse annotation
	 * meta-data
	 */
	public AnnotationConversationAttributeSource(ConversationAnnotationParser conversationAnnotationParsers) {
		this(Collections.singleton(conversationAnnotationParsers));
	}

	/**
	 * Constructor that uses a pre-built set with annotation parsers to retrieve the conversation meta-data. It is up to
	 * the caller to provide a sorted set of annotation parsers if the order of them is important.
	 *
	 * @param parsers The Set of annotation parsers used to retrieve conversation meta-data.
	 */
	public AnnotationConversationAttributeSource(Set<ConversationAnnotationParser> parsers) {
		Assert.notNull(parsers, "ConversationAnnotationParsers must not be null");
		conversationAnnotationParsers = parsers;
	}

	/**
	 * Resolves the conversation meta-data by delegating to the ConversationAnnotationParser instances. This implementation
	 * returns the first ConversationAttribute instance that will be returned by a ConversationAnnotationParser. This
	 * method returns null if no ConversationAnnotationParser returns a non-null result.
	 *
	 * The implementation searches for the most specific method (e.g. if there is a interface method this methods searches
	 * for the implementation method) before calling the underlying ConversationAnnotationParser instances. If there is no
	 * Annotation available on the implementation method, this methods falls back to the interface method.
	 *
	 * @param method The method for which the ConversationAttribute should be returned.
	 * @param targetClass The target class where the implementation should look for.
	 */
	public ConversationAttribute getConversationAttribute(Method method, Class<?> targetClass) {
		Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);

		for (ConversationAnnotationParser parser : conversationAnnotationParsers) {
			ConversationAttribute attribute = parser.parseConversationAnnotation(specificMethod);
			if (attribute != null) {
				return attribute;
			}

			if(method != specificMethod){
				attribute = parser.parseConversationAnnotation(method);
				if(attribute != null){
					return attribute;
				}
			}
		}
		return null;
	}
}
