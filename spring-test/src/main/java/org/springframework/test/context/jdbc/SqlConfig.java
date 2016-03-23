/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.context.jdbc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @SqlConfig} defines metadata that is used to determine how to parse
 * and execute SQL scripts configured via the {@link Sql @Sql} annotation.
 *
 * <h3>Configuration Scope</h3>
 * <p>When declared as a class-level annotation on an integration test class,
 * {@code @SqlConfig} serves as <strong><em>global</em></strong> configuration
 * for all SQL scripts within the test class hierarchy. When declared directly
 * via the {@link Sql#config config} attribute of the {@code @Sql} annotation,
 * {@code @SqlConfig} serves as <strong><em>local</em></strong> configuration
 * for the SQL scripts declared within the enclosing {@code @Sql} annotation.
 *
 * <h3>Default Values</h3>
 * <p>Every attribute in {@code @SqlConfig} has an <em>implicit</em> default value
 * which is documented in the javadocs of the corresponding attribute. Due to the
 * rules defined for annotation attributes in the Java Language Specification, it
 * is unfortunately not possible to assign a value of {@code null} to an annotation
 * attribute. Thus, in order to support overrides of <em>inherited</em> global
 * configuration, {@code @SqlConfig} attributes have an <em>explicit</em>
 * {@code default} value of either {@code ""} for Strings or {@code DEFAULT} for
 * Enums. This approach allows local declarations of {@code @SqlConfig} to
 * selectively override individual attributes from global declarations of
 * {@code @SqlConfig} by providing a value other than {@code ""} or {@code DEFAULT}.
 *
 * <h3>Inheritance and Overrides</h3>
 * <p>Global {@code @SqlConfig} attributes are <em>inherited</em> whenever local
 * {@code @SqlConfig} attributes do not supply an explicit value other than
 * {@code ""} or {@code DEFAULT}. Explicit local configuration therefore
 * <em>overrides</em> global configuration.
 *
 * @author Sam Brannen
 * @author Tadaya Tsuyukubo
 * @since 4.1
 * @see Sql
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface SqlConfig {

	/**
	 * The bean name of the {@link javax.sql.DataSource} against which the
	 * scripts should be executed.
	 * <p>The name is only required if there is more than one bean of type
	 * {@code DataSource} in the test's {@code ApplicationContext}. If there
	 * is only one such bean, it is not necessary to specify a bean name.
	 * <p>Defaults to an empty string, requiring that one of the following is
	 * true:
	 * <ol>
	 * <li>An explicit bean name is defined in a global declaration of
	 * {@code @SqlConfig}.
	 * <li>The data source can be retrieved from the transaction manager
	 * by using reflection to invoke a public method named
	 * {@code getDataSource()} on the transaction manager.
	 * <li>There is only one bean of type {@code DataSource} in the test's
	 * {@code ApplicationContext}.</li>
	 * <li>The {@code DataSource} to use is named {@code "dataSource"}.</li>
	 * </ol>
	 * @see org.springframework.test.context.transaction.TestContextTransactionUtils#retrieveDataSource
	 */
	String dataSource() default "";

	/**
	 * The bean name of the {@link org.springframework.transaction.PlatformTransactionManager
	 * PlatformTransactionManager} that should be used to drive transactions.
	 * <p>The name is only used if there is more than one bean of type
	 * {@code PlatformTransactionManager} in the test's {@code ApplicationContext}.
	 * If there is only one such bean, it is not necessary to specify a bean name.
	 * <p>Defaults to an empty string, requiring that one of the following is
	 * true:
	 * <ol>
	 * <li>An explicit bean name is defined in a global declaration of
	 * {@code @SqlConfig}.
	 * <li>There is only one bean of type {@code PlatformTransactionManager} in
	 * the test's {@code ApplicationContext}.</li>
	 * <li>{@link org.springframework.transaction.annotation.TransactionManagementConfigurer
	 * TransactionManagementConfigurer} has been implemented to specify which
	 * {@code PlatformTransactionManager} bean should be used for annotation-driven
	 * transaction management.</li>
	 * <li>The {@code PlatformTransactionManager} to use is named
	 * {@code "transactionManager"}.</li>
	 * </ol>
	 * @see org.springframework.test.context.transaction.TestContextTransactionUtils#retrieveTransactionManager
	 */
	String transactionManager() default "";

	/**
	 * The <em>mode</em> to use when determining whether SQL scripts should be
	 * executed within a transaction.
	 * <p>Defaults to {@link TransactionMode#DEFAULT DEFAULT}.
	 * <p>Can be set to {@link TransactionMode#ISOLATED} to ensure that the SQL
	 * scripts are executed in a new, isolated transaction that will be immediately
	 * committed.
	 * @see TransactionMode
	 */
	TransactionMode transactionMode() default TransactionMode.DEFAULT;

	/**
	 * The encoding for the supplied SQL scripts, if different from the platform
	 * encoding.
	 * <p>An empty string denotes that the platform encoding should be used.
	 */
	String encoding() default "";

	/**
	 * The character string used to separate individual statements within the
	 * SQL scripts.
	 * <p>Implicitly defaults to {@code ";"} if not specified and falls back to
	 * {@code "\n"} as a last resort.
	 * <p>May be set to
	 * {@link org.springframework.jdbc.datasource.init.ScriptUtils#EOF_STATEMENT_SEPARATOR}
	 * to signal that each script contains a single statement without a
	 * separator.
	 * @see org.springframework.jdbc.datasource.init.ScriptUtils#DEFAULT_STATEMENT_SEPARATOR
	 * @see org.springframework.jdbc.datasource.init.ScriptUtils#EOF_STATEMENT_SEPARATOR
	 */
	String separator() default "";

	/**
	 * The prefix that identifies single-line comments within the SQL scripts.
	 * <p>Implicitly defaults to {@code "--"}.
	 * @see org.springframework.jdbc.datasource.init.ScriptUtils#DEFAULT_COMMENT_PREFIX
	 */
	String commentPrefix() default "";

	/**
	 * The start delimiter that identifies block comments within the SQL scripts.
	 * <p>Implicitly defaults to {@code "/*"}.
	 * @see #blockCommentEndDelimiter
	 * @see org.springframework.jdbc.datasource.init.ScriptUtils#DEFAULT_BLOCK_COMMENT_START_DELIMITER
	 */
	String blockCommentStartDelimiter() default "";

	/**
	 * The end delimiter that identifies block comments within the SQL scripts.
	 * <p>Implicitly defaults to <code>"*&#47;"</code>.
	 * @see #blockCommentStartDelimiter
	 * @see org.springframework.jdbc.datasource.init.ScriptUtils#DEFAULT_BLOCK_COMMENT_END_DELIMITER
	 */
	String blockCommentEndDelimiter() default "";

	/**
	 * The <em>mode</em> to use when an error is encountered while executing an
	 * SQL statement.
	 * <p>Defaults to {@link ErrorMode#DEFAULT DEFAULT}.
	 * @see ErrorMode
	 */
	ErrorMode errorMode() default ErrorMode.DEFAULT;


	/**
	 * Enumeration of <em>modes</em> that dictate whether SQL scripts should be
	 * executed within a transaction and what the transaction propagation behavior
	 * should be.
	 */
	enum TransactionMode {

		/**
		 * Indicates that the <em>default</em> transaction mode should be used.
		 * <p>The meaning of <em>default</em> depends on the context in which
		 * {@code @SqlConfig} is declared:
		 * <ul>
		 * <li>If {@code @SqlConfig} is declared <strong>only</strong> locally,
		 * the default transaction mode is {@link #INFERRED}.</li>
		 * <li>If {@code @SqlConfig} is declared globally, the default transaction
		 * mode is {@link #INFERRED}.</li>
		 * <li>If {@code @SqlConfig} is declared globally <strong>and</strong>
		 * locally, the default transaction mode for the local declaration is
		 * inherited from the global declaration.</li>
		 * </ul>
		 */
		DEFAULT,

		/**
		 * Indicates that the transaction mode to use when executing SQL
		 * scripts should be <em>inferred</em> using the rules listed below.
		 * In the context of these rules, the term "<em>available</em>"
		 * means that the bean for the data source or transaction manager
		 * is either explicitly specified via a corresponding annotation
		 * attribute in {@code @SqlConfig} or discoverable via conventions. See
		 * {@link org.springframework.test.context.transaction.TestContextTransactionUtils TestContextTransactionUtils}
		 * for details on the conventions used to discover such beans in
		 * the {@code ApplicationContext}.
		 *
		 * <h4>Inference Rules</h4>
		 * <ol>
		 * <li>If neither a transaction manager nor a data source is
		 * available, an exception will be thrown.
		 * <li>If a transaction manager is not available but a data source
		 * is available, SQL scripts will be executed directly against the
		 * data source without a transaction.
		 * <li>If a transaction manager is available:
		 * <ul>
		 * <li>If a data source is not available, an attempt will be made
		 * to retrieve it from the transaction manager by using reflection
		 * to invoke a public method named {@code getDataSource()} on the
		 * transaction manager. If the attempt fails, an exception will be
		 * thrown.
		 * <li>Using the resolved transaction manager and data source, SQL
		 * scripts will be executed within an existing transaction if
		 * present; otherwise, scripts will be executed in a new transaction
		 * that will be immediately committed. An <em>existing</em>
		 * transaction will typically be managed by the
		 * {@link org.springframework.test.context.transaction.TransactionalTestExecutionListener TransactionalTestExecutionListener}.
		 * </ul>
		 * </ol>
		 * @see #ISOLATED
		 * @see org.springframework.test.context.transaction.TestContextTransactionUtils#retrieveDataSource
		 * @see org.springframework.test.context.transaction.TestContextTransactionUtils#retrieveTransactionManager
		 */
		INFERRED,

		/**
		 * Indicates that SQL scripts should always be executed in a new,
		 * <em>isolated</em> transaction that will be immediately committed.
		 * <p>In contrast to {@link #INFERRED}, this mode requires the
		 * presence of a transaction manager <strong>and</strong> a data
		 * source.
		 */
		ISOLATED
	}


	/**
	 * Enumeration of <em>modes</em> that dictate how errors are handled while
	 * executing SQL statements.
	 */
	enum ErrorMode {

		/**
		 * Indicates that the <em>default</em> error mode should be used.
		 * <p>The meaning of <em>default</em> depends on the context in which
		 * {@code @SqlConfig} is declared:
		 * <ul>
		 * <li>If {@code @SqlConfig} is declared <strong>only</strong> locally,
		 * the default error mode is {@link #FAIL_ON_ERROR}.</li>
		 * <li>If {@code @SqlConfig} is declared globally, the default error
		 * mode is {@link #FAIL_ON_ERROR}.</li>
		 * <li>If {@code @SqlConfig} is declared globally <strong>and</strong>
		 * locally, the default error mode for the local declaration is
		 * inherited from the global declaration.</li>
		 * </ul>
		 */
		DEFAULT,

		/**
		 * Indicates that script execution will fail if an error is encountered.
		 * In other words, no errors should be ignored.
		 * <p>This is effectively the default error mode so that if a script
		 * is accidentally executed, it will fail fast if any SQL statement in
		 * the script results in an error.
		 * @see #CONTINUE_ON_ERROR
		 */
		FAIL_ON_ERROR,

		/**
		 * Indicates that all errors in SQL scripts should be logged but not
		 * propagated as exceptions.
		 * <p>{@code CONTINUE_ON_ERROR} is the logical <em>opposite</em> of
		 * {@code FAIL_ON_ERROR} and a <em>superset</em> of {@code IGNORE_FAILED_DROPS}.
		 * @see #FAIL_ON_ERROR
		 * @see #IGNORE_FAILED_DROPS
		 */
		CONTINUE_ON_ERROR,

		/**
		 * Indicates that failed SQL {@code DROP} statements can be ignored.
		 * <p>This is useful for a non-embedded database whose SQL dialect does
		 * not support an {@code IF EXISTS} clause in a {@code DROP} statement.
		 * @see #CONTINUE_ON_ERROR
		 */
		IGNORE_FAILED_DROPS
	}

}
