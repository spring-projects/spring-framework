/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.transaction.annotation;

import java.lang.reflect.AnnotatedElement;

import org.springframework.transaction.interceptor.TransactionAttribute;

/**
 * Strategy interface for parsing known transaction annotation types.
 * {@link AnnotationTransactionAttributeSource} delegates to such
 * parsers for supporting specific annotation types such as Spring's own
 * {@link Transactional}, JTA 1.2's {@link javax.transaction.Transactional}
 * or EJB3's {@link javax.ejb.TransactionAttribute}.
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
	 * Parse the transaction attribute for the given method or class,
	 * based on a known annotation type.
	 * <p>This essentially parses a known transaction annotation into Spring's
	 * metadata attribute class. Returns {@code null} if the method/class
	 * is not transactional.
	 * @param ae the annotated method or class
	 * @return TransactionAttribute the configured transaction attribute,
	 * or {@code null} if none was found
	 * @see AnnotationTransactionAttributeSource#determineTransactionAttribute
	 */
	TransactionAttribute parseTransactionAnnotation(AnnotatedElement ae);

}
