/*
 * Copyright 2002-2019 the original author or authors.
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
 * {@code ActiveProfiles} is a class-level annotation that is used to declare
 * which <em>active bean definition profiles</em> should be used when loading
 * an {@link org.springframework.context.ApplicationContext ApplicationContext}
 * for test classes.
 *
 * <p>As of Spring Framework 4.0, this annotation may be used as a
 * <em>meta-annotation</em> to create custom <em>composed annotations</em>.
 *
 * @author Sam Brannen
 * @since 3.1
 * @see SmartContextLoader
 * @see MergedContextConfiguration
 * @see ContextConfiguration
 * @see ActiveProfilesResolver
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.annotation.Profile
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ActiveProfiles {

	/**
	 * Alias for {@link #profiles}.
	 * <p>This attribute may <strong>not</strong> be used in conjunction with
	 * {@link #profiles}, but it may be used <em>instead</em> of {@link #profiles}.
	 */
	@AliasFor("profiles")
	String[] value() default {};

	/**
	 * The bean definition profiles to activate.
	 * <p>This attribute may <strong>not</strong> be used in conjunction with
	 * {@link #value}, but it may be used <em>instead</em> of {@link #value}.
	 */
	@AliasFor("value")
	String[] profiles() default {};

	/**
	 * The type of {@link ActiveProfilesResolver} to use for resolving the active
	 * bean definition profiles programmatically.
	 * @since 4.0
	 * @see ActiveProfilesResolver
	 */
	Class<? extends ActiveProfilesResolver> resolver() default ActiveProfilesResolver.class;

	/**
	 * Whether or not bean definition profiles from superclasses should be
	 * <em>inherited</em>.
	 * <p>The default value is {@code true}, which means that a test
	 * class will <em>inherit</em> bean definition profiles defined by a
	 * test superclass. Specifically, the bean definition profiles for a test
	 * class will be appended to the list of bean definition profiles
	 * defined by a test superclass. Thus, subclasses have the option of
	 * <em>extending</em> the list of bean definition profiles.
	 * <p>If {@code inheritProfiles} is set to {@code false}, the bean
	 * definition profiles for the test class will <em>shadow</em> and
	 * effectively replace any bean definition profiles defined by a superclass.
	 * <p>In the following example, the {@code ApplicationContext} for
	 * {@code BaseTest} will be loaded using only the &quot;base&quot;
	 * bean definition profile; beans defined in the &quot;extended&quot; profile
	 * will therefore not be loaded. In contrast, the {@code ApplicationContext}
	 * for {@code ExtendedTest} will be loaded using the &quot;base&quot;
	 * <strong>and</strong> &quot;extended&quot; bean definition profiles.
	 * <pre class="code">
	 * &#064;ActiveProfiles(&quot;base&quot;)
	 * &#064;ContextConfiguration
	 * public class BaseTest {
	 *     // ...
	 * }
	 *
	 * &#064;ActiveProfiles(&quot;extended&quot;)
	 * &#064;ContextConfiguration
	 * public class ExtendedTest extends BaseTest {
	 *     // ...
	 * }
	 * </pre>
	 * <p>Note: {@code @ActiveProfiles} can be used when loading an
	 * {@code ApplicationContext} from path-based resource locations or
	 * annotated classes.
	 * @see ContextConfiguration#locations
	 * @see ContextConfiguration#classes
	 * @see ContextConfiguration#inheritLocations
	 */
	boolean inheritProfiles() default true;

}
