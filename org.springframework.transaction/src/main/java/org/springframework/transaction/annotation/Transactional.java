/*
 * Copyright 2002-2006 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.transaction.TransactionDefinition;

/**
 * Describes transaction attributes on a method or class.
 *
 * <p>This annotation type is generally directly comparable to Spring's
 * {@link org.springframework.transaction.interceptor.RuleBasedTransactionAttribute}
 * class, and in fact {@link AnnotationTransactionAttributeSource} will directly
 * convert the data to the latter class, so that Spring's transaction support code
 * does not have to know about annotations. If no rules are relevant to the exception,
 * it will be treated like
 * {@link org.springframework.transaction.interceptor.DefaultTransactionAttribute}
 * (rolling back on runtime exceptions).
 * 
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @since 1.2
 * @see org.springframework.transaction.interceptor.DefaultTransactionAttribute
 * @see org.springframework.transaction.interceptor.RuleBasedTransactionAttribute
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Transactional {
	
	/**
	 * The transaction propagation type.
	 * <p>Defaults to {@link Propagation#REQUIRED}.
	 */
	Propagation propagation() default Propagation.REQUIRED;
	
	/**
	 * The transaction isolation level.
	 * <p>Defaults to {@link Isolation#DEFAULT}.
	 */
	Isolation isolation() default Isolation.DEFAULT;

	/**
	 * The timeout for this transaction.
	 * <p>Defaults to the default timeout of the underlying transaction system.
	 */
	int timeout() default TransactionDefinition.TIMEOUT_DEFAULT;

	/**
	 * <code>true</code> if the transaction is read-only.
	 * <p>Defaults to <code>false</code>.
	 */
	boolean readOnly() default false;
	
	/**
	 * Defines zero (0) or more exception {@link Class classes}, which must be a
	 * subclass of {@link Throwable}, indicating which exception types must cause
	 * a transaction rollback.
	 * <p>This is the preferred way to construct a rollback rule, matching the
	 * exception class and subclasses.
	 * <p>Similar to {@link org.springframework.transaction.interceptor.RollbackRuleAttribute#RollbackRuleAttribute(Class clazz)}
	 */
	Class<? extends Throwable>[] rollbackFor() default {};
	
	/**
	 * Defines zero (0) or more exception names (for exceptions which must be a
	 * subclass of {@link Throwable}), indicating which exception types must cause
	 * a transaction rollback.
	 * <p>This can be a substring, with no wildcard support at present.
	 * A value of "ServletException" would match
	 * {@link javax.servlet.ServletException} and subclasses, for example.
	 * <p><b>NB: </b>Consider carefully how specific the pattern is, and whether
	 * to include package information (which isn't mandatory). For example,
	 * "Exception" will match nearly anything, and will probably hide other rules.
	 * "java.lang.Exception" would be correct if "Exception" was meant to define
	 * a rule for all checked exceptions. With more unusual {@link Exception}
	 * names such as "BaseBusinessException" there is no need to use a FQN.
	 * <p>Similar to {@link org.springframework.transaction.interceptor.RollbackRuleAttribute#RollbackRuleAttribute(String exceptionName)}
	 */
	String[] rollbackForClassName() default {};
    
	/**
	 * Defines zero (0) or more exception {@link Class Classes}, which must be a
	 * subclass of {@link Throwable}, indicating which exception types must <b>not</b>
	 * cause a transaction rollback.
	 * <p>This is the preferred way to construct a rollback rule, matching the
     * exception class and subclasses.
	 * <p>Similar to {@link org.springframework.transaction.interceptor.NoRollbackRuleAttribute#NoRollbackRuleAttribute(Class clazz)}
	 */
	Class<? extends Throwable>[] noRollbackFor() default {};
	
	/**
	 * Defines zero (0) or more exception names (for exceptions which must be a
	 * subclass of {@link Throwable}) indicating which exception types must <b>not</b>
	 * cause a transaction rollback.
	 * <p>See the description of {@link #rollbackForClassName()} for more info on how
	 * the specified names are treated.
	 * <p>Similar to {@link org.springframework.transaction.interceptor.NoRollbackRuleAttribute#NoRollbackRuleAttribute(String exceptionName)}
	 */
	String[] noRollbackForClassName() default {};

}
