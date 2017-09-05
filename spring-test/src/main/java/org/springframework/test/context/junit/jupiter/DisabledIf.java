/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.context.junit.jupiter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.core.annotation.AliasFor;

/**
 * {@code @DisabledIf} is used to signal that the annotated test class or test
 * method is <em>disabled</em> and should not be executed if the supplied
 * {@link #expression} evaluates to {@code true}.
 *
 * <p>When applied at the class level, all test methods within that class
 * are automatically disabled as well.
 *
 * <p>For basic examples, see the Javadoc for {@link #expression}.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create
 * custom <em>composed annotations</em>. For example, a custom
 * {@code @DisabledOnMac} annotation can be created as follows.
 *
 * <pre style="code">
 * {@literal @}Target({ ElementType.TYPE, ElementType.METHOD })
 * {@literal @}Retention(RetentionPolicy.RUNTIME)
 * {@literal @}DisabledIf(
 *     expression = "#{systemProperties['os.name'].toLowerCase().contains('mac')}",
 *     reason = "Disabled on Mac OS"
 * )
 * public {@literal @}interface DisabledOnMac {}
 * </pre>
 *
 * @author Sam Brannen
 * @author Tadaya Tsuyukubo
 * @since 5.0
 * @see SpringExtension
 * @see EnabledIf
 * @see org.junit.jupiter.api.Disabled
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(DisabledIfCondition.class)
public @interface DisabledIf {

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
	 * class or test method is <em>disabled</em>.
	 *
	 * <p>If the expression evaluates to {@link Boolean#TRUE} or a {@link String}
	 * equal to {@code "true"} (ignoring case), the test will be disabled.
	 *
	 * <p>Expressions can be any of the following.
	 *
	 * <ul>
	 * <li>Spring Expression Language (SpEL) expression &mdash; for example:
	 * <pre style="code">@DisabledIf("#{systemProperties['os.name'].toLowerCase().contains('mac')}")</pre>
	 * <li>Placeholder for a property available in the Spring
	 * {@link org.springframework.core.env.Environment Environment} &mdash; for example:
	 * <pre style="code">@DisabledIf("${smoke.tests.enabled}")</pre>
	 * <li>Text literal &mdash; for example:
	 * <pre style="code">@DisabledIf("true")</pre>
	 * </ul>
	 *
	 * <p>Note, however, that a <em>text literal</em> which is not the result of
	 * dynamic resolution of a property placeholder is of zero practical value
	 * since {@code @DisabledIf("true")} is equivalent to {@code @Disabled}
	 * and {@code @DisabledIf("false")} is logically meaningless.
	 *
	 * @see #reason
	 * @see #loadContext
	 * @see #value
	 */
	@AliasFor("value")
	String expression() default "";

	/**
	 * The reason this test is disabled.
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
