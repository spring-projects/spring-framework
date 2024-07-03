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

package org.springframework.test.context.jdbc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * {@code @Sql} is used to annotate a test class or test method to configure
 * SQL {@link #scripts} and {@link #statements} to be executed against a given
 * database during integration tests.
 *
 * <p>Method-level declarations override class-level declarations by default,
 * but this behavior can be configured via {@link SqlMergeMode @SqlMergeMode}.
 * However, this does not apply to class-level declarations configured for the
 * {@link ExecutionPhase#BEFORE_TEST_CLASS BEFORE_TEST_CLASS} or
 * {@link ExecutionPhase#AFTER_TEST_CLASS AFTER_TEST_CLASS} execution phase. Such
 * declarations cannot be overridden, and the corresponding scripts and statements
 * will be executed once per class in addition to any method-level scripts and
 * statements.
 *
 * <p>Script execution is performed by the {@link SqlScriptsTestExecutionListener},
 * which is enabled by default.
 *
 * <p>The configuration options provided by this annotation and
 * {@link SqlConfig @SqlConfig} are equivalent to those supported by
 * {@link org.springframework.jdbc.datasource.init.ScriptUtils ScriptUtils} and
 * {@link org.springframework.jdbc.datasource.init.ResourceDatabasePopulator ResourceDatabasePopulator}
 * but are a superset of those provided by the {@code <jdbc:initialize-database/>}
 * XML namespace element. Consult the javadocs of individual attributes in this
 * annotation and {@link SqlConfig @SqlConfig} for details.
 *
 * <p>{@code @Sql} can be used as a <em>{@linkplain Repeatable repeatable}</em>
 * annotation. Otherwise, {@link SqlGroup @SqlGroup} can be used as an explicit
 * container for declaring multiple instances of {@code @Sql}.
 *
 * <p>This annotation will be inherited from an enclosing test class by default. See
 * {@link org.springframework.test.context.NestedTestConfiguration @NestedTestConfiguration}
 * for details. This annotation may also be used as a <em>meta-annotation</em> to
 * create custom <em>composed annotations</em> with attribute overrides.
 *
 * <p>If you want to see which SQL scripts are being executed, set the
 * {@code org.springframework.test.context.jdbc} logging category to {@code DEBUG}.
 * If you want to see which SQL statements are being executed, set the
 * {@code org.springframework.jdbc.datasource.init} logging category to {@code DEBUG}.
 *
 * <p>Use of this annotation requires the {@code spring-jdbc} and {@code spring-tx}
 * modules as well as their transitive dependencies to be present on the classpath.
 *
 * @author Sam Brannen
 * @author Andreas Ahlenstorf
 * @since 4.1
 * @see SqlConfig
 * @see SqlMergeMode
 * @see SqlGroup
 * @see SqlScriptsTestExecutionListener
 * @see org.springframework.transaction.annotation.Transactional
 * @see org.springframework.test.context.transaction.TransactionalTestExecutionListener
 * @see org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
 * @see org.springframework.jdbc.datasource.init.ScriptUtils
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(SqlGroup.class)
public @interface Sql {

	/**
	 * Alias for {@link #scripts}.
	 * <p>This attribute may <strong>not</strong> be used in conjunction with
	 * {@link #scripts}, but it may be used instead of {@link #scripts}.
	 * @see #scripts
	 * @see #statements
	 */
	@AliasFor("scripts")
	String[] value() default {};

	/**
	 * The paths to the SQL scripts to execute.
	 * <p>This attribute may <strong>not</strong> be used in conjunction with
	 * {@link #value}, but it may be used instead of {@link #value}. Similarly,
	 * this attribute may be used in conjunction with or instead of
	 * {@link #statements}.
	 * <h4>Path Resource Semantics</h4>
	 * <p>Each path will be interpreted as a Spring
	 * {@link org.springframework.core.io.Resource Resource}. A plain path
	 * &mdash; for example, {@code "schema.sql"} &mdash; will be treated as a
	 * classpath resource that is <em>relative</em> to the package in which the
	 * test class is defined. A path starting with a slash will be treated as an
	 * <em>absolute</em> classpath resource, for example:
	 * {@code "/org/example/schema.sql"}. A path which references a
	 * URL (e.g., a path prefixed with
	 * {@link org.springframework.util.ResourceUtils#CLASSPATH_URL_PREFIX classpath:},
	 * {@link org.springframework.util.ResourceUtils#FILE_URL_PREFIX file:},
	 * {@code http:}, etc.) will be loaded using the specified resource protocol.
	 * <p>As of Spring Framework 6.2, paths may contain property placeholders
	 * (<code>${...}</code>) that will be replaced by properties stored in the
	 * {@link org.springframework.core.env.Environment Environment} of the test's
	 * {@code ApplicationContext}.
	 * <h4>Default Script Detection</h4>
	 * <p>If no SQL scripts or {@link #statements} are specified, an attempt will
	 * be made to detect a <em>default</em> script depending on where this
	 * annotation is declared. If a default cannot be detected, an
	 * {@link IllegalStateException} will be thrown.
	 * <ul>
	 * <li><strong>class-level declaration</strong>: if the annotated test class
	 * is {@code com.example.MyTest}, the corresponding default script is
	 * {@code "classpath:com/example/MyTest.sql"}.</li>
	 * <li><strong>method-level declaration</strong>: if the annotated test
	 * method is named {@code testMethod()} and is defined in the class
	 * {@code com.example.MyTest}, the corresponding default script is
	 * {@code "classpath:com/example/MyTest.testMethod.sql"}.</li>
	 * </ul>
	 * @see #value
	 * @see #statements
	 * @see org.springframework.core.env.Environment#resolveRequiredPlaceholders(String)
	 */
	@AliasFor("value")
	String[] scripts() default {};

	/**
	 * <em>Inlined SQL statements</em> to execute.
	 * <p>This attribute may be used in conjunction with or instead of
	 * {@link #scripts}.
	 * <h4>Ordering</h4>
	 * <p>Statements declared via this attribute will be executed after
	 * statements loaded from resource {@link #scripts}. If you wish to have
	 * inlined statements executed before scripts, simply declare multiple
	 * instances of {@code @Sql} on the same class or method.
	 * @since 4.2
	 * @see #scripts
	 */
	String[] statements() default {};

	/**
	 * When the SQL scripts and statements should be executed.
	 * <p>Defaults to {@link ExecutionPhase#BEFORE_TEST_METHOD BEFORE_TEST_METHOD}.
	 */
	ExecutionPhase executionPhase() default ExecutionPhase.BEFORE_TEST_METHOD;

	/**
	 * Local configuration for the SQL scripts and statements declared within
	 * this {@code @Sql} annotation.
	 * <p>See the class-level javadocs for {@link SqlConfig} for explanations of
	 * local vs. global configuration, inheritance, overrides, etc.
	 * <p>Defaults to an empty {@link SqlConfig @SqlConfig} instance.
	 */
	SqlConfig config() default @SqlConfig;


	/**
	 * Enumeration of <em>phases</em> that dictate when SQL scripts are executed.
	 */
	enum ExecutionPhase {

		/**
		 * The configured SQL scripts and statements will be executed once per
		 * test class <em>before</em> any test method is run.
		 * <p>Specifically, the configured SQL scripts and statements will be
		 * executed prior to any <em>before class lifecycle methods</em> of a
		 * particular testing framework &mdash; for example, methods annotated
		 * with JUnit Jupiter's {@link org.junit.jupiter.api.BeforeAll @BeforeAll}
		 * annotation.
		 * <p>NOTE: Configuring {@code BEFORE_TEST_CLASS} as the execution phase
		 * causes the test's {@code ApplicationContext} to be eagerly loaded
		 * during test class initialization which can potentially result in
		 * undesired side effects. For example,
		 * {@link org.springframework.test.context.DynamicPropertySource @DynamicPropertySource}
		 * methods will be invoked before {@code @BeforeAll} methods when using
		 * {@code BEFORE_TEST_CLASS}.
		 * @since 6.1
		 * @see #AFTER_TEST_CLASS
		 * @see #BEFORE_TEST_METHOD
		 * @see #AFTER_TEST_METHOD
		 */
		BEFORE_TEST_CLASS,

		/**
		 * The configured SQL scripts and statements will be executed once per
		 * test class <em>after</em> all test methods have run.
		 * <p>Specifically, the configured SQL scripts and statements will be
		 * executed after any <em>after class lifecycle methods</em> of a
		 * particular testing framework &mdash; for example, methods annotated
		 * with JUnit Jupiter's {@link org.junit.jupiter.api.AfterAll @AfterAll}
		 * annotation.
		 * @since 6.1
		 * @see #BEFORE_TEST_CLASS
		 * @see #BEFORE_TEST_METHOD
		 * @see #AFTER_TEST_METHOD
		 */
		AFTER_TEST_CLASS,

		/**
		 * The configured SQL scripts and statements will be executed
		 * <em>before</em> the corresponding test method.
		 * <p>Specifically, the configured SQL scripts and statements will be
		 * executed prior to any <em>before test lifecycle methods</em> of a
		 * particular testing framework &mdash; for example, methods annotated
		 * with JUnit Jupiter's {@link org.junit.jupiter.api.BeforeEach @BeforeEach}
		 * annotation.
		 * @see #BEFORE_TEST_CLASS
		 * @see #AFTER_TEST_CLASS
		 * @see #AFTER_TEST_METHOD
		 */
		BEFORE_TEST_METHOD,

		/**
		 * The configured SQL scripts and statements will be executed
		 * <em>after</em> the corresponding test method.
		 * <p>Specifically, the configured SQL scripts and statements will be
		 * executed after any <em>after test lifecycle methods</em> of a
		 * particular testing framework &mdash; for example, methods annotated
		 * with JUnit Jupiter's {@link org.junit.jupiter.api.AfterEach @AfterEach}
		 * annotation.
		 * @see #BEFORE_TEST_CLASS
		 * @see #AFTER_TEST_CLASS
		 * @see #BEFORE_TEST_METHOD
		 */
		AFTER_TEST_METHOD
	}

}
