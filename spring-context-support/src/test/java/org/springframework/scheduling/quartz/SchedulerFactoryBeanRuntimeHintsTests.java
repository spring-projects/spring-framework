/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
