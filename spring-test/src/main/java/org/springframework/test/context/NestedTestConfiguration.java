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

package org.springframework.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;

/**
 * {@code @NestedTestConfiguration} is an annotation that can be applied to a test
 * class to configure how Spring test configuration annotations are processed
 * within enclosing class hierarchies (i.e., for <em>inner</em> test classes).
 *
 * <p>If {@code @NestedTestConfiguration} is not <em>present</em> or
 * <em>meta-present</em> on a test class, in its supertype hierarchy, or in its
 * enclosing class hierarchy, the default <em>enclosing configuration inheritance
 * mode</em> will be used. A {@code @NestedTestConfiguration} declaration on an
 * enclosing class for a nested interface will be ignored when searching for the
 * annotation on classes that implement the interface. See
 * {@link #ENCLOSING_CONFIGURATION_PROPERTY_NAME} for details on how to change
 * the default mode.
 *
 * <p>When the {@link EnclosingConfiguration#INHERIT INHERIT} mode is in use,
 * configuration from an enclosing test class will be inherited by inner test
 * classes, analogous to the semantics within a test class inheritance hierarchy.
 * When the {@link EnclosingConfiguration#OVERRIDE OVERRIDE} mode is in use,
 * inner test classes will have to declare their own Spring test configuration
 * annotations. If you wish to explicitly configure the mode, annotate either
 * the inner test class or one of its enclosing classes with
 * {@code @NestedTestConfiguration(...)}. Note that a
 * {@code @NestedTestConfiguration(...)} declaration is inherited within the
 * superclass hierarchy as well as within the enclosing class hierarchy. Thus,
 * there is no need to redeclare the annotation unless you wish to switch the
 * mode.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>.
 *
 * <p>As of Spring Framework 5.3, the use of this annotation typically only makes
 * sense in conjunction with {@link org.junit.jupiter.api.Nested @Nested} test
 * classes in JUnit Jupiter; however, there may be other testing frameworks with
 * support for nested test classes that could also make use of this annotation.
 *
 * <h3>Supported Annotations</h3>
 * <p>The <em>Spring TestContext Framework</em> honors {@code @NestedTestConfiguration}
 * semantics for the following annotations.
 * <ul>
 * <li>{@link BootstrapWith @BootstrapWith}</li>
 * <li>{@link TestExecutionListeners @TestExecutionListeners}</li>
 * <li>{@link ContextConfiguration @ContextConfiguration}</li>
 * <li>{@link ContextHierarchy @ContextHierarchy}</li>
 * <li>{@link org.springframework.test.context.web.WebAppConfiguration @WebAppConfiguration}</li>
 * <li>{@link ActiveProfiles @ActiveProfiles}</li>
 * <li>{@link TestPropertySource @TestPropertySource}</li>
 * <li>{@link DynamicPropertySource @DynamicPropertySource}</li>
 * <li>{@link org.springframework.test.annotation.DirtiesContext @DirtiesContext}</li>
 * <li>{@link org.springframework.transaction.annotation.Transactional @Transactional}</li>
 * <li>{@link org.springframework.test.annotation.Rollback @Rollback}</li>
 * <li>{@link org.springframework.test.annotation.Commit @Commit}</li>
 * <li>{@link org.springframework.test.context.jdbc.Sql @Sql}</li>
 * <li>{@link org.springframework.test.context.jdbc.SqlConfig @SqlConfig}</li>
 * <li>{@link org.springframework.test.context.jdbc.SqlMergeMode @SqlMergeMode}</li>
 * <li>{@link TestConstructor @TestConstructor}</li>
 * </ul>
 *
 * @author Sam Brannen
 * @since 5.3
 * @see EnclosingConfiguration#INHERIT
 * @see EnclosingConfiguration#OVERRIDE
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface NestedTestConfiguration {

	/**
	 * JVM system property used to change the default <em>enclosing configuration
	 * inheritance mode</em>: {@value}.
	 * <p>Supported values include enum constants defined in
	 * {@link EnclosingConfiguration}, ignoring case. For example, the default
	 * may be changed to {@link EnclosingConfiguration#OVERRIDE} by supplying
	 * the following JVM system property via the command line.
	 * <pre style="code">-Dspring.test.enclosing.configuration=override</pre>
	 * <p>If the property is not set to {@code OVERRIDE}, test configuration for
	 * an inner test class will be <em>inherited</em> according to
	 * {@link EnclosingConfiguration#INHERIT} semantics by default.
	 * <p>May alternatively be configured via the
	 * {@link org.springframework.core.SpringProperties SpringProperties}
	 * mechanism.
	 * @see #value
	 */
	String ENCLOSING_CONFIGURATION_PROPERTY_NAME = "spring.test.enclosing.configuration";


	/**
	 * Configures the {@link EnclosingConfiguration} mode.
	 * @see EnclosingConfiguration#INHERIT
	 * @see EnclosingConfiguration#OVERRIDE
	 */
	EnclosingConfiguration value();


	/**
	 * Enumeration of <em>modes</em> that dictate how test configuration from
	 * enclosing classes is processed for inner test classes.
	 * @see #INHERIT
	 * @see #OVERRIDE
	 */
	enum EnclosingConfiguration {

		/**
		 * Indicates that test configuration for an inner test class should be
		 * <em>inherited</em> from its {@linkplain Class#getEnclosingClass()
		 * enclosing class}, as if the enclosing class were a superclass.
		 */
		INHERIT,

		/**
		 * Indicates that test configuration for an inner test class should
		 * <em>override</em> configuration from its
		 * {@linkplain Class#getEnclosingClass() enclosing class}.
		 */
		OVERRIDE;


		/**
		 * Get the {@code EnclosingConfiguration} enum constant with the supplied
		 * name, ignoring case.
		 * @param name the name of the enum constant to retrieve
		 * @return the corresponding enum constant or {@code null} if not found
		 * @see EnclosingConfiguration#valueOf(String)
		 */
		@Nullable
		public static EnclosingConfiguration from(@Nullable String name) {
			if (name == null) {
				return null;
			}
			try {
				return EnclosingConfiguration.valueOf(name.trim().toUpperCase());
			}
			catch (IllegalArgumentException ex) {
				Log logger = LogFactory.getLog(EnclosingConfiguration.class);
				if (logger.isDebugEnabled()) {
					logger.debug(String.format(
						"Failed to parse enclosing configuration mode from '%s': %s",
						name, ex.getMessage()));
				}
				return null;
			}
		}

	}

}
