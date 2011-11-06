/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code ActiveProfiles} is a class-level annotation that is used to declare
 * which <em>active bean definition profiles</em> should be used when loading
 * an {@link org.springframework.context.ApplicationContext ApplicationContext}
 * for test classes.
 *
 * @author Sam Brannen
 * @since 3.1
 * @see SmartContextLoader
 * @see MergedContextConfiguration
 * @see ContextConfiguration
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.annotation.Profile
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ActiveProfiles {

	/**
	 * Alias for {@link #profiles}.
	 * 
	 * <p>This attribute may <strong>not</strong> be used in conjunction
	 * with {@link #profiles}, but it may be used <em>instead</em> of
	 * {@link #profiles}.
	 */
	String[] value() default {};

	/**
	 * The bean definition profiles to activate.
	 * 
	 * <p>This attribute may <strong>not</strong> be used in conjunction
	 * with {@link #value}, but it may be used <em>instead</em> of
	 * {@link #value}.
	 */
	String[] profiles() default {};

	/**
	 * Whether or not bean definition profiles from superclasses should be
	 * <em>inherited</em>.
	 *
	 * <p>The default value is <code>true</code>, which means that an annotated
	 * class will <em>inherit</em> bean definition profiles defined by an
	 * annotated superclass. Specifically, the bean definition profiles for an
	 * annotated class will be appended to the list of bean definition profiles
	 * defined by an annotated superclass. Thus, subclasses have the option of
	 * <em>extending</em> the list of bean definition profiles.
	 *
	 * <p>If <code>inheritProfiles</code> is set to <code>false</code>, the bean
	 * definition profiles for the annotated class will <em>shadow</em> and
	 * effectively replace any bean definition profiles defined by a superclass.
	 *
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
	 *
	 * <p>Note: {@code @ActiveProfiles} can be used when loading an 
	 * {@code ApplicationContext} from path-based resource locations or
	 * configuration classes.
	 * @see ContextConfiguration#locations
	 * @see ContextConfiguration#classes
	 * @see ContextConfiguration#inheritLocations
	 */
	boolean inheritProfiles() default true;

}
