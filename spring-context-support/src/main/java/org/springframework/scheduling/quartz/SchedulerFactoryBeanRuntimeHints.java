/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.quartz;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeHint.Builder;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.annotation.ReflectiveRuntimeHintsRegistrar;
import org.springframework.util.ClassUtils;

/**
 * {@link RuntimeHintsRegistrar} implementation that makes sure {@link SchedulerFactoryBean}
 * reflection hints are registered.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @since 6.0
 */
class SchedulerFactoryBeanRuntimeHints implements RuntimeHintsRegistrar {

	private static final String SCHEDULER_FACTORY_CLASS_NAME = "org.quartz.impl.StdSchedulerFactory";

	private static final ReflectiveRuntimeHintsRegistrar registrar = new ReflectiveRuntimeHintsRegistrar();


	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		if (!ClassUtils.isPresent(SCHEDULER_FACTORY_CLASS_NAME, classLoader)) {
			return;
		}
		hints.reflection()
				.registerType(TypeReference.of(SCHEDULER_FACTORY_CLASS_NAME), this::typeHint)
				.registerTypes(TypeReference.listOf(ResourceLoaderClassLoadHelper.class,
						LocalTaskExecutorThreadPool.class, LocalDataSourceJobStore.class), this::typeHint);
		registrar.registerRuntimeHints(hints, LocalTaskExecutorThreadPool.class);
	}

	private void typeHint(Builder typeHint) {
		typeHint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS).onReachableType(SchedulerFactoryBean.class);
	}

}
