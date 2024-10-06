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

package org.springframework.orm.jpa;

import java.util.Collections;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * {@link RuntimeHintsRegistrar} implementation that makes sure that hints related to
 * {@link AbstractEntityManagerFactoryBean} and {@link SharedEntityManagerCreator} are registered.
 *
 * @author Sebastien Deleuze
 * @since 6.0
 */
class EntityManagerRuntimeHints implements RuntimeHintsRegistrar {

	private static final String HIBERNATE_SESSION_FACTORY_CLASS_NAME = "org.hibernate.SessionFactory";

	private static final String ENTITY_MANAGER_FACTORY_CLASS_NAME = "jakarta.persistence.EntityManagerFactory";

	private static final String QUERY_SQM_IMPL_CLASS_NAME = "org.hibernate.query.sqm.internal.QuerySqmImpl";

	private static final String NATIVE_QUERY_IMPL_CLASS_NAME = "org.hibernate.query.sql.internal.NativeQueryImpl";


	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		if (ClassUtils.isPresent(HIBERNATE_SESSION_FACTORY_CLASS_NAME, classLoader)) {
			hints.proxies().registerJdkProxy(TypeReference.of(HIBERNATE_SESSION_FACTORY_CLASS_NAME),
					TypeReference.of(EntityManagerFactoryInfo.class));
			hints.proxies().registerJdkProxy(TypeReference.of("org.hibernate.Session"),
					TypeReference.of(EntityManagerProxy.class));
		}
		if (ClassUtils.isPresent(ENTITY_MANAGER_FACTORY_CLASS_NAME, classLoader)) {
			hints.reflection().registerType(TypeReference.of(ENTITY_MANAGER_FACTORY_CLASS_NAME), builder -> {
				builder.onReachableType(SharedEntityManagerCreator.class).withMethod("getCriteriaBuilder",
						Collections.emptyList(), ExecutableMode.INVOKE);
				builder.onReachableType(SharedEntityManagerCreator.class).withMethod("getMetamodel",
						Collections.emptyList(), ExecutableMode.INVOKE);
			});
		}
		try {
			Class<?> clazz = ClassUtils.forName(QUERY_SQM_IMPL_CLASS_NAME, classLoader);
			hints.proxies().registerJdkProxy(ClassUtils.getAllInterfacesForClass(clazz, classLoader));
		}
		catch (ClassNotFoundException ignored) {
		}
		try {
			Class<?> clazz = ClassUtils.forName(NATIVE_QUERY_IMPL_CLASS_NAME, classLoader);
			hints.proxies().registerJdkProxy(ClassUtils.getAllInterfacesForClass(clazz, classLoader));
		}
		catch (ClassNotFoundException ignored) {
		}
	}

}
