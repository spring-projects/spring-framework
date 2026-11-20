/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.vendor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceUnitInfo;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.cfg.Configuration;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;

import org.springframework.core.NativeDetector;
import org.springframework.orm.jpa.persistenceunit.SmartPersistenceUnitInfo;
import org.springframework.util.ClassUtils;

/**
 * Spring-specific subclass of the standard {@link HibernatePersistenceProvider}
 * from the {@code org.hibernate.jpa} package, adding support for
 * {@link SmartPersistenceUnitInfo#getManagedPackages()}.
 *
 * @author Juergen Hoeller
 * @author Joris Kuipers
 * @author Sebastien Deleuze
 * @since 4.1
 * @see Configuration#addPackage
 */
class SpringHibernateJpaPersistenceProvider extends HibernatePersistenceProvider {

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
		final List<String> mergedClassesAndPackages = new ArrayList<>(info.getManagedClassNames());
		if (info instanceof SmartPersistenceUnitInfo smartInfo) {
			mergedClassesAndPackages.addAll(smartInfo.getManagedPackages());
		}

		PersistenceUnitInfoDescriptor descriptor;
		if (!NativeDetector.inNativeImage()) {
			// No ClassTransformer adaptation necessary
			descriptor = new PersistenceUnitInfoDescriptor(info) {
				@Override
				public List<String> getManagedClassNames() {
					return mergedClassesAndPackages;
				}
			};
		}
		else if (ClassUtils.hasMethod(PersistenceUnitInfoDescriptor.class, "isClassTransformerRegistrationDisabled")) {
			// Hibernate 8.0: no pushClassTransformer override necessary
			descriptor = new PersistenceUnitInfoDescriptor(info) {
				@Override
				public List<String> getManagedClassNames() {
					return mergedClassesAndPackages;
				}
				@SuppressWarnings("unused")  // @Override on Hibernate 8.0
				public boolean isClassTransformerRegistrationDisabled() {
					return true;
				}
			};
		}
		else {
			// Hibernate 7.x: pushClassTransformer no-op in native image
			descriptor = new PersistenceUnitInfoDescriptor(info) {
				@Override
				public List<String> getManagedClassNames() {
					return mergedClassesAndPackages;
				}
				@Override
				public void pushClassTransformer(EnhancementContext enhancementContext) {
					// no-op
				}
			};
		}

		return new EntityManagerFactoryBuilderImpl(descriptor, properties).build();
	}

}
