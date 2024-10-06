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

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeHint.Builder;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.annotation.ReflectiveRuntimeHintsRegistrar;
import org.springframework.lang.Nullable;
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
