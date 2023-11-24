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
