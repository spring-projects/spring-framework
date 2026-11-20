/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.annotation;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;

import jakarta.ejb.ApplicationException;
import jakarta.ejb.TransactionAttributeType;
import org.jspecify.annotations.Nullable;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;

/**
 * Strategy implementation for parsing EJB3's {@link jakarta.ejb.TransactionAttribute} annotation.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see SpringTransactionAnnotationParser
 * @see JtaTransactionAnnotationParser
 */
@SuppressWarnings("serial")
public class Ejb3TransactionAnnotationParser implements TransactionAnnotationParser, Serializable {

	@Override
	public boolean isCandidateClass(Class<?> targetClass) {
		return AnnotationUtils.isCandidateClass(targetClass, jakarta.ejb.TransactionAttribute.class);
	}

	@Override
	public @Nullable TransactionAttribute parseTransactionAnnotation(AnnotatedElement element) {
		jakarta.ejb.TransactionAttribute ann = element.getAnnotation(jakarta.ejb.TransactionAttribute.class);
		if (ann != null) {
			return parseTransactionAnnotation(ann);
		}
		else {
			return null;
		}
	}

	public TransactionAttribute parseTransactionAnnotation(jakarta.ejb.TransactionAttribute ann) {
		return new Ejb3TransactionAttribute(ann.value());
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (other instanceof Ejb3TransactionAnnotationParser);
	}

	@Override
	public int hashCode() {
		return Ejb3TransactionAnnotationParser.class.hashCode();
	}


	/**
	 * EJB3-specific TransactionAttribute, implementing EJB3's rollback rules
	 * which are based on annotated exceptions.
	 */
	private static class Ejb3TransactionAttribute extends DefaultTransactionAttribute {

		public Ejb3TransactionAttribute(TransactionAttributeType type) {
			setPropagationBehaviorName(PREFIX_PROPAGATION + type.name());
		}

		@Override
		public boolean rollbackOn(Throwable ex) {
			ApplicationException ann = ex.getClass().getAnnotation(ApplicationException.class);
			return (ann != null ? ann.rollback() : super.rollbackOn(ex));
		}
	}

}
