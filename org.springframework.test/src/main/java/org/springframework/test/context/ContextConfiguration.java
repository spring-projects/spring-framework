/*
 * Copyright 2002-2008 the original author or authors.
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
 * ContextConfiguration defines class-level metadata which can be used to
 * instruct client code with regard to how to load and configure an
 * {@link org.springframework.context.ApplicationContext}.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see ContextLoader
 * @see org.springframework.context.ApplicationContext
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface ContextConfiguration {

	/**
	 * The resource locations to use for loading an
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}.
	 */
	String[] locations() default {};

	/**
	 * Whether or not {@link #locations() resource locations} from superclasses
	 * should be <em>inherited</em>.
	 * <p>The default value is <code>true</code>, which means that an annotated
	 * class will <em>inherit</em> the resource locations defined by an
	 * annotated superclass. Specifically, the resource locations for an
	 * annotated class will be appended to the list of resource locations
	 * defined by an annotated superclass. Thus, subclasses have the option of
	 * <em>extending</em> the list of resource locations. In the following
	 * example, the {@link org.springframework.context.ApplicationContext}
	 * for <code>ExtendedTest</code> will be loaded from
	 * &quot;base-context.xml&quot; <strong>and</strong>
	 * &quot;extended-context.xml&quot;, in that order. Beans defined in
	 * &quot;extended-context.xml&quot; may therefore override those defined in
	 * &quot;base-context.xml&quot;.
	 * <pre class="code">
	 * {@link ContextConfiguration @ContextConfiguration}(locations={&quot;base-context.xml&quot;})
	 * public class BaseTest {
	 *     // ...
	 * }
	 * {@link ContextConfiguration @ContextConfiguration}(locations={&quot;extended-context.xml&quot;})
	 * public class ExtendedTest extends BaseTest {
	 *     // ...
	 * }
	 * </pre>
	 * If <code>inheritLocations</code> is set to <code>false</code>, the
	 * resource locations for the annotated class will <em>shadow</em> and
	 * effectively replace any resource locations defined by a superclass.
	 */
	boolean inheritLocations() default true;

	/**
	 * The type of {@link ContextLoader} to use for loading an
	 * {@link org.springframework.context.ApplicationContext}.
	 */
	Class<? extends ContextLoader> loader() default ContextLoader.class;

}
