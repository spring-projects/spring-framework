/*
 * Copyright 2002-present the original author or authors.
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
				// @Override on Hibernate 8.0
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
