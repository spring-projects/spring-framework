/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.test.context.junit.jupiter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.core.annotation.AliasFor;

/**
 * {@code @EnabledIf} is used to signal that the annotated test class or test
 * method is <em>enabled</em> and should be executed if the supplied
 * {@link #expression} evaluates to {@code true}.
 *
 * <p>When applied at the class level, all test methods within that class
 * are automatically enabled by default as well.
 *
 * <p>For basic examples, see the Javadoc for {@link #expression}.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create
 * custom <em>composed annotations</em>. For example, a custom
 * {@code @EnabledOnMac} annotation can be created as follows.
 *
 * <pre style="code">
 * {@literal @}Target({ElementType.TYPE, ElementType.METHOD})
 * {@literal @}Retention(RetentionPolicy.RUNTIME)
 * {@literal @}EnabledIf(
 *     expression = "#{systemProperties['os.name'].toLowerCase().contains('mac')}",
 *     reason = "Enabled on Mac OS"
 * )
 * public {@literal @}interface EnabledOnMac {}
 * </pre>
 *
 * <p>Please note that {@code @EnabledOnMac} is meant only as an example of what
 * is possible. If you have that exact use case, please use the built-in
 * {@link org.junit.jupiter.api.condition.EnabledOnOs @EnabledOnOs(MAC)} support
 * in JUnit Jupiter.
 *
 * <p>JUnit Jupiter also has a condition annotation named
 * {@link org.junit.jupiter.api.condition.EnabledIf @EnabledIf}. Thus, if you
 * wish to use Spring's {@code @EnabledIf} support make sure you import the
 * annotation type from the correct package.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see SpringExtension
 * @see DisabledIf
 * @see org.junit.jupiter.api.Disabled
 * @see org.junit.jupiter.api.condition.EnabledIf
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(EnabledIfCondition.class)
public @interface EnabledIf {

	/**
	 * Alias for {@link #expression}; only intended to be used if {@link #reason}
	 * and {@link #loadContext} are not specified.
	 *
	 * @see #expression
	 */
	@AliasFor("expression")
	String value() default "";

	/**
	 * The expression that will be evaluated to determine if the annotated test
	 * class or test method is <em>enabled</em>.
	 *
	 * <p>If the expression evaluates to {@link Boolean#TRUE} or a {@link String}
	 * equal to {@code "true"} (ignoring case), the test will be enabled.
	 *
	 * <p>Expressions can be any of the following.
	 *
	 * <ul>
	 * <li>Spring Expression Language (SpEL) expression &mdash; for example:
	 * <pre style="code">@EnabledIf("#{systemProperties['os.name'].toLowerCase().contains('mac')}")</pre>
	 * <li>Placeholder for a property available in the Spring
	 * {@link org.springframework.core.env.Environment Environment} &mdash; for example:
	 * <pre style="code">@EnabledIf("${smoke.tests.enabled}")</pre>
	 * <li>Text literal &mdash; for example:
	 * <pre style="code">@EnabledIf("true")</pre>
	 * </ul>
	 *
	 * <p>Note, however, that a <em>text literal</em> which is not the result of
	 * dynamic resolution of a property placeholder is of zero practical value
	 * since {@code @EnabledIf("false")} is equivalent to {@code @Disabled}
	 * and {@code @EnabledIf("true")} is logically meaningless.
	 *
	 * @see #reason
	 * @see #loadContext
	 * @see #value
	 */
	@AliasFor("value")
	String expression() default "";

	/**
	 * The reason this test is enabled.
	 *
	 * @see #expression
	 */
	String reason() default "";

	/**
	 * Whether the {@code ApplicationContext} associated with the current test
	 * should be eagerly loaded in order to evaluate the {@link #expression}.
	 *
	 * <p>Defaults to {@code false} so that test application contexts are not
	 * eagerly loaded unnecessarily. If an expression is based solely on system
	 * properties or environment variables or does not interact with beans in
	 * the test's application context, there is no need to load the context
	 * prematurely since doing so would be a waste of time if the test ends up
	 * being disabled.
	 *
	 * @see #expression
	 */
	boolean loadContext() default false;

}
