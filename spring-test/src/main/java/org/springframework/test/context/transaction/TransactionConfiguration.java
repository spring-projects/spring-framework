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

package org.springframework.test.context.transaction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code TransactionConfiguration} defines class-level metadata for configuring
 * transactional tests.
 *
 * <p>As of Spring Framework 4.0, this annotation may be used as a
 * <em>meta-annotation</em> to create custom <em>composed annotations</em>.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see TransactionalTestExecutionListener
 * @see org.springframework.test.context.ContextConfiguration
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TransactionConfiguration {

	/**
	 * The bean name of the {@link org.springframework.transaction.PlatformTransactionManager
	 * PlatformTransactionManager} that should be used to drive transactions.
	 *
	 * <p>This attribute is not required and only needs to be declared if there
	 * are multiple beans of type {@code PlatformTransactionManager} in the test's
	 * {@code ApplicationContext} <em>and</em> if one of the following is true.
	 * <ul>
	 * <li>the bean name of the desired {@code PlatformTransactionManager} is not
	 * "transactionManager"</li>
	 * <li>{@link org.springframework.transaction.annotation.TransactionManagementConfigurer
	 * TransactionManagementConfigurer} was not implemented to specify which
	 * {@code PlatformTransactionManager} bean should be used for annotation-driven
	 * transaction management
	 * </ul>
	 *
	 * <p><b>NOTE:</b> The XML {@code <tx:annotation-driven>} element also refers
	 * to a bean named "transactionManager" by default. If you are using both
	 * features in combination, make sure to point to the same transaction manager
	 * bean - here in {@code @TransactionConfiguration} and also in
	 * {@code <tx:annotation-driven transaction-manager="...">}.
	 */
	String transactionManager() default "transactionManager";

	/**
	 * Should transactions be rolled back by default?
	 */
	boolean defaultRollback() default true;

}
