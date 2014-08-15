/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.orm.jpa.vendor;

import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.HibernatePersistence;

import org.springframework.orm.jpa.persistenceunit.SmartPersistenceUnitInfo;

/**
 * Spring-specific subclass of the standard {@link HibernatePersistence}
 * provider from the {@code org.hibernate.ejb} package, adding support for
 * {@link SmartPersistenceUnitInfo#getManagedPackages()}.
 *
 * <p>Compatible with Hibernate 3.6 as well as 4.0-4.2.
 *
 * @author Juergen Hoeller
 * @author Joris Kuipers
 * @since 4.1
 * @see Ejb3Configuration#addPackage
 */
class SpringHibernateEjbPersistenceProvider extends HibernatePersistence {

	@SuppressWarnings("rawtypes")
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
		Ejb3Configuration cfg = new Ejb3Configuration();
		if (info instanceof SmartPersistenceUnitInfo) {
			for (String managedPackage : ((SmartPersistenceUnitInfo) info).getManagedPackages()) {
				cfg.addPackage(managedPackage);
			}
		}
		Ejb3Configuration configured = cfg.configure(info, properties);
		return (configured != null ? configured.buildEntityManagerFactory() : null);
	}

}
