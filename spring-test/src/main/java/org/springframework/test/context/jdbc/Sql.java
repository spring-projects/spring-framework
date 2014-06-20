/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.jdbc;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.util.ResourceUtils;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

/**
 * {@code @Sql} is used to annotate a test class or test method to configure SQL
 * scripts to be executed against a given database during integration tests.
 *
 * <p>Method-level declarations override class-level declarations.
 *
 * <p>Script execution is performed by the {@link SqlScriptsTestExecutionListener},
 * which is enabled by default.
 *
 * <p>The configuration options provided by this annotation are equivalent to
 * those supported by {@link ScriptUtils} and {@link org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
 * ResourceDatabasePopulator} but are a superset of those provided by the
 * {@code <jdbc:initialize-database />} XML namespace element. Consult the
 * Javadoc of individual attributes in this annotation for details.
 *
 * <p>Beginning with Java 8, {@code @Sql} can be used as a
 * <em>{@linkplain Repeatable repeatable}</em> annotation. Otherwise,
 * {@link SqlGroup @SqlGroup} can be used as an explicit container for declaring
 * multiple instances of {@code @Sql}.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>; however, attribute overrides are not currently
 * supported for {@linkplain Repeatable repeatable} annotations that are used as
 * meta-annotations.
 *
 * @author Sam Brannen
 * @author Tadaya Tsuyukubo
 * @since 4.1
 * @see SqlGroup
 * @see SqlScriptsTestExecutionListener
 * @see org.springframework.transaction.annotation.Transactional
 * @see org.springframework.test.context.transaction.TransactionalTestExecutionListener
 * @see org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
 * @see org.springframework.jdbc.datasource.init.ScriptUtils
 */
@Documented
@Inherited
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
@Repeatable(SqlGroup.class)
public @interface Sql {

	/**
	 * Enumeration of <em>phases</em> that dictate when SQL scripts are executed.
	 */
	static enum ExecutionPhase {

		/**
		 * The configured SQL scripts will be executed <em>before</em> the
		 * corresponding test method.
		 */
		BEFORE_TEST_METHOD,

		/**
		 * The configured SQL scripts will be executed <em>after</em> the
		 * corresponding test method.
		 */
		AFTER_TEST_METHOD;
	}


	/**
	 * Alias for {@link #scripts}.
	 * <p>This attribute may <strong>not</strong> be used in conjunction with
	 * {@link #scripts}, but it may be used instead of {@link #scripts}.
	 */
	String[] value() default {};

	/**
	 * The paths to the SQL scripts to execute.
	 *
	 * <p>This attribute may <strong>not</strong> be used in conjunction with
	 * {@link #value}, but it may be used instead of {@link #value}.
	 *
	 * <h3>Path Resource Semantics</h3>
	 * <p>Each path will be interpreted as a Spring
	 * {@link org.springframework.core.io.Resource Resource}. A plain path
	 * &mdash; for example, {@code "schema.sql"} &mdash; will be treated as a
	 * classpath resource that is <em>relative</em> to the package in which the
	 * test class is defined. A path starting with a slash will be treated as an
	 * <em>absolute</em> classpath resource, for example:
	 * {@code "/org/example/schema.sql"}. A path which references a
	 * URL (e.g., a path prefixed with
	 * {@link ResourceUtils#CLASSPATH_URL_PREFIX classpath:},
	 * {@link ResourceUtils#FILE_URL_PREFIX file:}, {@code http:}, etc.) will be
	 * loaded using the specified resource protocol.
	 *
	 * <h3>Default Script Detection</h3>
	 * <p>If no SQL scripts are specified, an attempt will be made to detect a
	 * <em>default</em> script depending on where this annotation is declared.
	 * If a default cannot be detected, an {@link IllegalStateException} will be
	 * thrown.
	 * <ul>
	 * <li><strong>class-level declaration</strong>: if the annotated test class
	 * is {@code com.example.MyTest}, the corresponding default script is
	 * {@code "classpath:com/example/MyTest.sql"}.</li>
	 * <li><strong>method-level declaration</strong>: if the annotated test
	 * method is named {@code testMethod()} and is defined in the class
	 * {@code com.example.MyTest}, the corresponding default script is
	 * {@code "classpath:com/example/MyTest.testMethod.sql"}.</li>
	 * </ul>
	 */
	String[] scripts() default {};

	/**
	 * The encoding for the supplied SQL scripts, if different from the platform
	 * encoding.
	 * <p>An empty string denotes that the platform encoding should be used.
	 */
	String encoding() default "";

	/**
	 * The bean name of the {@link javax.sql.DataSource} against which the scripts
	 * should be executed.
	 * <p>The name is only used if there is more than one bean of type
	 * {@code DataSource} in the test's {@code ApplicationContext}. If there is
	 * only one such bean, it is not necessary to specify a bean name.
	 * <p>Defaults to an empty string, requiring that one of the following is
	 * true:
	 * <ol>
	 * <li>There is only one bean of type {@code DataSource} in the test's
	 * {@code ApplicationContext}.</li>
	 * <li>The {@code DataSource} to use is named {@code "dataSource"}.</li>
	 * </ol>
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
	 * <li>There is only one bean of type {@code PlatformTransactionManager} in
	 * the test's {@code ApplicationContext}.</li>
	 * <li>{@link org.springframework.transaction.annotation.TransactionManagementConfigurer
	 * TransactionManagementConfigurer} has been implemented to specify which
	 * {@code PlatformTransactionManager} bean should be used for annotation-driven
	 * transaction management.</li>
	 * <li>The {@code PlatformTransactionManager} to use is named
	 * {@code "transactionManager"}.</li>
	 * </ol>
	 */
	String transactionManager() default "";

	/**
	 * Flag to indicate that the SQL scripts must be executed in a new transaction.
	 * <p>Defaults to {@code false}, meaning that the SQL scripts will be executed
	 * within the current transaction if present. The <em>current</em> transaction
	 * will typically be managed by the
	 * {@link org.springframework.test.context.transaction.TransactionalTestExecutionListener
	 * TransactionalTestExecutionListener}.
	 * <p>Can be set to {@code true} to ensure that the scripts are executed in
	 * a new, isolated transaction that will be immediately committed.
	 */
	boolean requireNewTransaction() default false;

	/**
	 * The character string used to separate individual statements within the
	 * SQL scripts.
	 * <p>Defaults to {@code ";"} if not specified and falls back to {@code "\n"}
	 * as a last resort; may be set to {@link ScriptUtils#EOF_STATEMENT_SEPARATOR}
	 * to signal that each script contains a single statement without a separator.
	 */
	String separator() default ScriptUtils.DEFAULT_STATEMENT_SEPARATOR;

	/**
	 * The prefix that identifies single-line comments within the SQL scripts.
	 * <p>Defaults to {@code "--"}.
	 */
	String commentPrefix() default ScriptUtils.DEFAULT_COMMENT_PREFIX;

	/**
	 * The start delimiter that identifies block comments within the SQL scripts.
	 * <p>Defaults to {@code "/*"}.
	 * @see #blockCommentEndDelimiter
	 */
	String blockCommentStartDelimiter() default ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER;

	/**
	 * The end delimiter that identifies block comments within the SQL scripts.
	 * <p>Defaults to <code>"*&#47;"</code>.
	 * @see #blockCommentStartDelimiter
	 */
	String blockCommentEndDelimiter() default ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER;

	/**
	 * Flag to indicate that all failures in SQL should be logged but not cause
	 * a failure.
	 * <p>Defaults to {@code false}.
	 * @see #ignoreFailedDrops
	 */
	boolean continueOnError() default false;

	/**
	 * Flag to indicate that a failed SQL {@code DROP} statement can be ignored.
	 * <p>This is useful for a non-embedded database whose SQL dialect does not
	 * support an {@code IF EXISTS} clause in a {@code DROP} statement.
	 * <p>The default is {@code false} so that if a script is accidentally
	 * executed, it will fail fast if the script starts with a {@code DROP}
	 * statement.
	 * @see #continueOnError
	 */
	boolean ignoreFailedDrops() default false;

	/**
	 * When the SQL scripts should be executed.
	 * <p>Defaults to {@link ExecutionPhase#BEFORE_TEST_METHOD BEFORE_TEST_METHOD}.
	 */
	ExecutionPhase executionPhase() default ExecutionPhase.BEFORE_TEST_METHOD;

}
