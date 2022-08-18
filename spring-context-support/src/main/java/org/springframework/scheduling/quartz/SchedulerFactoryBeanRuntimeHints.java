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

package org.springframework.scheduling.quartz;

import java.util.function.Consumer;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeHint.Builder;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.annotation.ReflectiveRuntimeHintsRegistrar;
import org.springframework.util.ClassUtils;

/**
 * {@link RuntimeHintsRegistrar} implementation that makes sure {@link SchedulerFactoryBean}
 * reflection entries are registered.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @since 6.0
 */
class SchedulerFactoryBeanRuntimeHints implements RuntimeHintsRegistrar {

	private static final String SCHEDULER_FACTORY_CLASS_NAME = "org.quartz.impl.StdSchedulerFactory";

	private static final TypeReference FACTORY_BEAN_TYPE_REFERENCE = TypeReference.of(SchedulerFactoryBean.class);

	private final ReflectiveRuntimeHintsRegistrar reflectiveRegistrar = new ReflectiveRuntimeHintsRegistrar();

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		if (!ClassUtils.isPresent(SCHEDULER_FACTORY_CLASS_NAME, classLoader)) {
			return;
		}
		Consumer<Builder> typeHint = type -> type
				.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
				.onReachableType(FACTORY_BEAN_TYPE_REFERENCE);
		hints.reflection()
				.registerType(TypeReference.of(SCHEDULER_FACTORY_CLASS_NAME), typeHint)
				.registerTypes(TypeReference.listOf(ResourceLoaderClassLoadHelper.class,
						LocalTaskExecutorThreadPool.class, LocalDataSourceJobStore.class), typeHint);
		this.reflectiveRegistrar.registerRuntimeHints(hints, LocalTaskExecutorThreadPool.class);
	}
}
