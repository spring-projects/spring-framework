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

import java.util.List;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.util.ClassUtils;

/**
 * {@link RuntimeHintsRegistrar} implementation that makes sure {@link SchedulerFactoryBean}
 * reflection entries are registered.
 *
 * @author Sebastien Deleuze
 * @since 6.0
 */
public class SchedulerFactoryBeanRuntimeHints implements RuntimeHintsRegistrar {

	private static String SCHEDULER_FACTORY_CLASS_NAME = "org.quartz.impl.StdSchedulerFactory";

	private static TypeReference FACTORY_BEAN_TYPE_REFERENCE = TypeReference.of(SchedulerFactoryBean.class);

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		if (ClassUtils.isPresent(SCHEDULER_FACTORY_CLASS_NAME, classLoader)) {
			hints.reflection().registerType(TypeReference.of(SCHEDULER_FACTORY_CLASS_NAME),
					builder -> builder
							.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
							.onReachableType(FACTORY_BEAN_TYPE_REFERENCE));
			hints.reflection().registerType(ResourceLoaderClassLoadHelper.class,
					builder -> builder
							.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
							.onReachableType(FACTORY_BEAN_TYPE_REFERENCE));
			hints.reflection().registerType(LocalTaskExecutorThreadPool.class,
					builder -> builder
							.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
							.withMethod("setInstanceId", List.of(TypeReference.of(String.class)), b -> {})
							.withMethod("setInstanceName", List.of(TypeReference.of(String.class)), b -> {})
							.onReachableType(FACTORY_BEAN_TYPE_REFERENCE));
			hints.reflection().registerType(LocalDataSourceJobStore.class,
					builder -> builder
							.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
							.onReachableType(FACTORY_BEAN_TYPE_REFERENCE));

		}
	}
}
