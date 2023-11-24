/*
 * Copyright 2002-2022 the original author or authors.
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @SqlMergeMode} is used to annotate a test class or test method to
 * configure whether method-level {@code @Sql} declarations are merged with
 * class-level {@code @Sql} declarations.
 *
 * <p>A method-level {@code @SqlMergeMode} declaration overrides a class-level
 * declaration.
 *
 * <p>If {@code @SqlMergeMode} is not declared on a test class or test method,
 * {@link MergeMode#OVERRIDE} will be used by default.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em> with attribute overrides.
 *
 * <p>As of Spring Framework 5.3, this annotation will be inherited from an
 * enclosing test class by default. See
 * {@link org.springframework.test.context.NestedTestConfiguration @NestedTestConfiguration}
 * for details.
 *
 * @author Sam Brannen
 * @author Dmitry Semukhin
 * @since 5.2
 * @see Sql
 * @see MergeMode#MERGE
 * @see MergeMode#OVERRIDE
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface SqlMergeMode {

	/**
	 * Indicates whether method-level {@code @Sql} annotations should be merged
	 * with class-level {@code @Sql} annotations or override them.
	 */
	MergeMode value();


	/**
	 * Enumeration of <em>modes</em> that dictate whether method-level {@code @Sql}
	 * declarations are merged with class-level {@code @Sql} declarations.
	 */
	enum MergeMode {

		/**
		 * Indicates that method-level {@code @Sql} declarations should be merged
		 * with class-level {@code @Sql} declarations, with class-level SQL
		 * scripts and statements executed before method-level scripts and
		 * statements.
		 */
		MERGE,

		/**
		 * Indicates that method-level {@code @Sql} declarations should override
		 * class-level {@code @Sql} declarations.
		 */
		OVERRIDE

	}

}
