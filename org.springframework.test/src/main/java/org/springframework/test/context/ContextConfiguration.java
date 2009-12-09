/*
 * Copyright 2002-2009 the original author or authors.
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
 * {@link org.springframework.context.ApplicationContext ApplicationContext}.
 * 
 * @author Sam Brannen
 * @since 2.5
 * @see ContextLoader
 * @see org.springframework.context.ApplicationContext
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ContextConfiguration {

	/**
	 * Alias for {@link #locations() locations}.
	 * @since 3.0
	 */
	String[] value() default {};

	/**
	 * The resource locations to use for loading an
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}.
	 * <p>Check out {@link org.springframework.test.context.support.AbstractContextLoader#modifyLocations)}'s
	 * javadoc for details on how a location String will be interpreted at runtime,
	 * in particular in case of a relative path. Also, check out the documentation on
	 * {@link org.springframework.test.context.support.AbstractContextLoader#generateDefaultLocations}
	 * for details on the default locations that are going to be used if none are specified.
	 * <p>Note that the above-mentioned default rules only apply for a standard
	 * {@link org.springframework.test.context.support.AbstractContextLoader} subclass
	 * such as {@link org.springframework.test.context.support.GenericXmlContextLoader}
	 * which is the effective default implementation used at runtime.
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
	 * example, the {@link org.springframework.context.ApplicationContext ApplicationContext}
     * for <code>ExtendedTest</code> will be loaded from
	 * &quot;base-context.xml&quot; <strong>and</strong>
	 * &quot;extended-context.xml&quot;, in that order. Beans defined in
	 * &quot;extended-context.xml&quot; may therefore override those defined in
	 * &quot;base-context.xml&quot;.
	 * <pre class="code">
	 * &#064;ContextConfiguration(&quot;base-context.xml&quot;)
	 * public class BaseTest {
	 * 	// ...
	 * }
	 * 
	 * &#064;ContextConfiguration(&quot;extended-context.xml&quot;)
	 * public class ExtendedTest extends BaseTest {
	 * 	// ...
	 * }
	 * </pre>
	 * If <code>inheritLocations</code> is set to <code>false</code>, the
	 * resource locations for the annotated class will <em>shadow</em> and
	 * effectively replace any resource locations defined by a superclass.
	 */
	boolean inheritLocations() default true;

	/**
	 * The type of {@link ContextLoader} to use for loading an
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}.
	 * <p>If not specified, the loader will be inherited from the first superclass
	 * which is annotated with <code>&#064;ContextConfiguration</code> and specifies
     * an explicit loader. If no class in the hierarchy specifies an explicit
     * loader, a default loader will be used instead.
	 * <p>The default concrete implementation chosen at runtime will be
	 * {@link org.springframework.test.context.support.GenericXmlContextLoader}.
	 * Also check out {@link org.springframework.test.context.support.AbstractContextLoader}'s
	 * javadoc for details on the default behavior there.
	 */
	Class<? extends ContextLoader> loader() default ContextLoader.class;

}
