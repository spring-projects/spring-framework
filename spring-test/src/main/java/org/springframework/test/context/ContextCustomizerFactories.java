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

import org.springframework.core.annotation.AliasFor;

/**
 * {@code @ContextCustomizerFactories} is an annotation that can be applied to a
 * test class to configure which {@link ContextCustomizerFactory} implementations
 * should be registered with the <em>Spring TestContext Framework</em>.
 *
 * <p>{@code @ContextCustomizerFactories} is used to register factories for a
 * particular test class, its subclasses, and its nested classes. If you wish to
 * register a factory globally, you should register it via the automatic discovery
 * mechanism described in {@link ContextCustomizerFactory}.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>. In addition, this annotation will be inherited
 * from an enclosing test class by default. See
 * {@link NestedTestConfiguration @NestedTestConfiguration} for details.
 *
 * @author Sam Brannen
 * @since 6.1
 * @see ContextCustomizerFactory
 * @see ContextCustomizer
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ContextCustomizerFactories {

	/**
	 * Alias for {@link #factories}.
	 * <p>This attribute may <strong>not</strong> be used in conjunction with
	 * {@link #factories}, but it may be used instead of {@link #factories}.
	 */
	@AliasFor("factories")
	Class<? extends ContextCustomizerFactory>[] value() default {};

	/**
	 * The {@link ContextCustomizerFactory} implementations to register.
	 * <p>This attribute may <strong>not</strong> be used in conjunction with
	 * {@link #value}, but it may be used instead of {@link #value}.
	 */
	@AliasFor("value")
	Class<? extends ContextCustomizerFactory>[] factories() default {};

	/**
	 * Whether the configured set of {@link #factories} from superclasses and
	 * enclosing classes should be <em>inherited</em>.
	 * <p>The default value is {@code true}, which means that an annotated class
	 * will <em>inherit</em> the factories defined by an annotated superclass or
	 * enclosing class. Specifically, the factories for an annotated class will be
	 * appended to the list of factories defined by an annotated superclass or
	 * enclosing class. Thus, subclasses and nested classes have the option of
	 * <em>extending</em> the list of factories.
	 * <p>If {@code inheritListeners} is set to {@code false}, the factories for
	 * the annotated class will <em>shadow</em> and effectively replace any
	 * factories defined by a superclass or enclosing class.
	 */
	boolean inheritFactories() default true;

	/**
	 * The <em>merge mode</em> to use when {@code @ContextCustomizerFactories} is
	 * declared on a class that does <strong>not</strong> inherit factories from
	 * a superclass or enclosing class.
	 * <p>Can be set to {@link MergeMode#REPLACE_DEFAULTS REPLACE_DEFAULTS} to
	 * have locally declared factories replace the default factories.
	 * <p>The mode is ignored if factories are inherited from a superclass or
	 * enclosing class.
	 * <p>Defaults to {@link MergeMode#MERGE_WITH_DEFAULTS MERGE_WITH_DEFAULTS}.
	 * @see MergeMode
	 */
	MergeMode mergeMode() default MergeMode.MERGE_WITH_DEFAULTS;


	/**
	 * Enumeration of <em>modes</em> that dictate whether explicitly declared
	 * factories are merged with the default factories when
	 * {@code @ContextCustomizerFactories} is declared on a class that does
	 * <strong>not</strong> inherit factories from a superclass or enclosing
	 * class.
	 */
	enum MergeMode {

		/**
		 * Indicates that locally declared factories should be merged with the
		 * default factories.
		 * <p>The merging algorithm ensures that duplicates are removed from the
		 * list and that locally declared factories are appended to the list of
		 * default factories when merged.
		 */
		MERGE_WITH_DEFAULTS,

		/**
		 * Indicates that locally declared factories should replace the default
		 * factories.
		 */
		REPLACE_DEFAULTS

	}

}
