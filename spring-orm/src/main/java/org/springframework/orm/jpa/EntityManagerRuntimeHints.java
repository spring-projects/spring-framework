/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa;

import java.util.Collections;

import jakarta.persistence.Query;
import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.util.ClassUtils;

/**
 * {@link RuntimeHintsRegistrar} implementation that makes sure that hints related to
 * {@link AbstractEntityManagerFactoryBean} and {@link SharedEntityManagerCreator} are registered.
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 6.0
 */
class EntityManagerRuntimeHints implements RuntimeHintsRegistrar {

	private static final String HIBERNATE_SESSION_FACTORY_CLASS_NAME = "org.hibernate.SessionFactory";

	private static final String ENTITY_MANAGER_FACTORY_CLASS_NAME = "jakarta.persistence.EntityManagerFactory";

	// Up to Hibernate 7.0
	private static final String QUERY_SQM_IMPL_CLASS_NAME = "org.hibernate.query.sqm.internal.QuerySqmImpl";

	// As of Hibernate 7.1
	private static final String SQM_QUERY_IMPL_CLASS_NAME = "org.hibernate.query.sqm.internal.SqmQueryImpl";

	// As of Hibernate 8.0
	private static final String SELECTION_QUERY_IMPL_CLASS_NAME = "org.hibernate.query.internal.SelectionQueryImpl";

	// As of Hibernate 8.0
	private static final String MUTATION_QUERY_IMPL_CLASS_NAME = "org.hibernate.query.internal.MutationQueryImpl";

	// As of Hibernate 8.0
	private static final String MUTATION_OR_SELECTION_QUERY_IMPL_CLASS_NAME =
			"org.hibernate.query.internal.MutationOrSelectionQueryImpl";

	private static final String NATIVE_QUERY_IMPL_CLASS_NAME = "org.hibernate.query.sql.internal.NativeQueryImpl";

	private static final String STATELESS_SESSION_CLASS_NAME = "org.hibernate.StatelessSession";

	private static final String PERSISTENCE_UNIT_INFO_DESCRIPTOR_CLASS_NAME =
			"org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor";

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
			// Fallback proxy for Hibernate 8.0 query implementation types like NativeMutationOrSelectionQueryImpl,
			// matching the single-interface fallback in SharedEntityManagerCreator. See gh-36878
			hints.proxies().registerJdkProxy(TypeReference.of(Query.class));
		}
		registerJdkProxyFor(hints, classLoader, QUERY_SQM_IMPL_CLASS_NAME);
		registerJdkProxyFor(hints, classLoader, SQM_QUERY_IMPL_CLASS_NAME);
		registerJdkProxyFor(hints, classLoader, SELECTION_QUERY_IMPL_CLASS_NAME);
		registerJdkProxyFor(hints, classLoader, MUTATION_QUERY_IMPL_CLASS_NAME);
		registerJdkProxyFor(hints, classLoader, MUTATION_OR_SELECTION_QUERY_IMPL_CLASS_NAME);
		registerJdkProxyFor(hints, classLoader, NATIVE_QUERY_IMPL_CLASS_NAME);
		registerJdkProxyFor(hints, classLoader, STATELESS_SESSION_CLASS_NAME);
		if (ClassUtils.isPresent(PERSISTENCE_UNIT_INFO_DESCRIPTOR_CLASS_NAME, classLoader)) {
			hints.reflection().registerType(TypeReference.of(PERSISTENCE_UNIT_INFO_DESCRIPTOR_CLASS_NAME),
					MemberCategory.INVOKE_PUBLIC_METHODS);
		}
	}

	private static void registerJdkProxyFor(RuntimeHints hints, @Nullable ClassLoader classLoader, String className) {
		try {
			Class<?> clazz = ClassUtils.forName(className, classLoader);
			hints.proxies().registerJdkProxy(ClassUtils.getAllInterfacesForClass(clazz, classLoader));
		}
		catch (ClassNotFoundException ignored) {
		}
	}

}
