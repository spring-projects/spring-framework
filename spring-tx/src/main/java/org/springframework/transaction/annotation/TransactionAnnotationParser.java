/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.transaction.annotation;

import java.lang.reflect.AnnotatedElement;

import org.springframework.lang.Nullable;
import org.springframework.transaction.interceptor.TransactionAttribute;

/**
 * Strategy interface for parsing known transaction annotation types.
 * {@link AnnotationTransactionAttributeSource} delegates to such
 * parsers for supporting specific annotation types such as Spring's own
 * {@link Transactional}, JTA 1.2's {@link jakarta.transaction.Transactional}
 * or EJB3's {@link jakarta.ejb.TransactionAttribute}.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see AnnotationTransactionAttributeSource
 * @see SpringTransactionAnnotationParser
 * @see Ejb3TransactionAnnotationParser
 * @see JtaTransactionAnnotationParser
 */
public interface TransactionAnnotationParser {

	/**
	 * Determine whether the given class is a candidate for transaction attributes
	 * in the annotation format of this {@code TransactionAnnotationParser}.
	 * <p>If this method returns {@code false}, the methods on the given class
	 * will not get traversed for {@code #parseTransactionAnnotation} introspection.
	 * Returning {@code false} is therefore an optimization for non-affected
	 * classes, whereas {@code true} simply means that the class needs to get
	 * fully introspected for each method on the given class individually.
	 * @param targetClass the class to introspect
	 * @return {@code false} if the class is known to have no transaction
	 * annotations at class or method level; {@code true} otherwise. The default
	 * implementation returns {@code true}, leading to regular introspection.
	 * @since 5.2
	 */
	default boolean isCandidateClass(Class<?> targetClass) {
		return true;
	}

	/**
	 * Parse the transaction attribute for the given method or class,
	 * based on an annotation type understood by this parser.
	 * <p>This essentially parses a known transaction annotation into Spring's metadata
	 * attribute class. Returns {@code null} if the method/class is not transactional.
	 * <p>The returned attribute will typically (but not necessarily) be of type
	 * {@link org.springframework.transaction.interceptor.RuleBasedTransactionAttribute}.
	 * @param element the annotated method or class
	 * @return the configured transaction attribute, or {@code null} if none found
	 * @see AnnotationTransactionAttributeSource#determineTransactionAttribute
	 */
	@Nullable
	TransactionAttribute parseTransactionAnnotation(AnnotatedElement element);

}
