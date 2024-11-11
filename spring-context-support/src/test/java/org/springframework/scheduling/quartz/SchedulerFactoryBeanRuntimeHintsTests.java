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

package org.springframework.scheduling.quartz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SchedulerFactoryBeanRuntimeHints}.
 *
 * @author Sebastien Deleuze
 */
class SchedulerFactoryBeanRuntimeHintsTests {

	private final RuntimeHints hints = new RuntimeHints();

	@BeforeEach
	void setup() {
		AotServices.factories().load(RuntimeHintsRegistrar.class)
				.forEach(registrar -> registrar.registerHints(this.hints,
						ClassUtils.getDefaultClassLoader()));
	}

	@Test
	void stdSchedulerFactoryHasHints() {
		assertThat(RuntimeHintsPredicates.reflection().onType(TypeReference.of("org.quartz.impl.StdSchedulerFactory"))
				.withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(this.hints);
	}

	@Test
	void defaultClassLoadHelperHasHints() {
		assertThat(RuntimeHintsPredicates.reflection().onType(ResourceLoaderClassLoadHelper.class)
				.withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(this.hints);
	}

	@Test
	void defaultThreadPoolHasHints() {
		assertThat(RuntimeHintsPredicates.reflection().onType(LocalTaskExecutorThreadPool.class)
				.withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS))
				.accepts(this.hints);
	}

	@Test
	void defaultJobStoreHasHints() {
		assertThat(RuntimeHintsPredicates.reflection().onType(LocalDataSourceJobStore.class)
				.withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS))
				.accepts(this.hints);
	}
}
