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
	@SuppressWarnings({"rawtypes", "unchecked"})  // on Hibernate 6
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
		final List<String> mergedClassesAndPackages = new ArrayList<>(info.getManagedClassNames());
		if (info instanceof SmartPersistenceUnitInfo smartInfo) {
			mergedClassesAndPackages.addAll(smartInfo.getManagedPackages());
		}
		return new EntityManagerFactoryBuilderImpl(
				new PersistenceUnitInfoDescriptor(info) {
					@Override
					public List<String> getManagedClassNames() {
						return mergedClassesAndPackages;
					}
					@Override
					public void pushClassTransformer(EnhancementContext enhancementContext) {
						if (!NativeDetector.inNativeImage()) {
							super.pushClassTransformer(enhancementContext);
						}
					}
				}, properties).build();
	}

}
