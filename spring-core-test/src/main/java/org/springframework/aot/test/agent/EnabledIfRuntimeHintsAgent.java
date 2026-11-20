/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.test.agent;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIf;

import org.springframework.aot.agent.RuntimeHintsAgent;

/**
 * {@code @EnabledIfRuntimeHintsAgent} signals that the annotated test class or test method
 * is only enabled if the {@link RuntimeHintsAgent} is loaded on the current JVM.
 * <p>This is meta-annotated with {@code @Tag("RuntimeHintsTests")} so that test suites
 * can choose to target or ignore those tests.
 *
 * <pre class="code">
 * &#064;EnabledIfRuntimeHintsAgent
 * class MyTestCases {
 *
 *     &#064;Test
 *     void hintsForMethodsReflectionShouldMatch() {
 *         RuntimeHints hints = new RuntimeHints();
 *         hints.reflection().registerType(String.class,
 *             hint -> hint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS));
 *
 *         RuntimeHintsInvocations invocations = RuntimeHintsRecorder.record(() -> {
 *             Method[] methods = String.class.getMethods();
 *         });
 *         assertThat(invocations).match(hints);
 *     }
 *
 * }
 * </pre>
 *
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 6.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EnabledIf(value = "org.springframework.aot.agent.RuntimeHintsAgent#isLoaded",
		disabledReason = "RuntimeHintsAgent is not loaded on the current JVM")
@Tag("RuntimeHintsTests")
public @interface EnabledIfRuntimeHintsAgent {
}
