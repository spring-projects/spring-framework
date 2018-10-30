/*
 * Copyright 2002-2017 the original author or authors.
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

import java.lang.reflect.Method;
import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.orm.jpa.EntityManagerFactoryAccessor;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Simple {@code FactoryBean} that exposes the underlying {@link SessionFactory}
 * behind a Hibernate-backed JPA {@link EntityManagerFactory}.
 *
 * <p>Primarily available for resolving a SessionFactory by JPA persistence unit name
 * via the {@link #setPersistenceUnitName "persistenceUnitName"} bean property.
 *
 * <p>Note that, for straightforward cases, you could also simply declare a factory method:
 *
 * <pre class="code">
 * &lt;bean id="sessionFactory" factory-bean="entityManagerFactory" factory-method="getSessionFactory"/&gt;
 * </pre>
 *
 * <p>And as of JPA 2.1, {@link EntityManagerFactory#unwrap} provides a nice approach as well,
 * in particular within configuration class arrangements:
 *
 * <pre class="code">
 * &#064;Bean
 * public SessionFactory sessionFactory(@Qualifier("entityManagerFactory") EntityManagerFactory emf) {
 *     return emf.unwrap(SessionFactory.class);
 * }
 * </pre>
 *
 * Please note: Since Hibernate 5.2 changed its {@code SessionFactory} interface to extend JPA's
 * {@code EntityManagerFactory}, you may get conflicts when injecting by type, with both the
 * original factory and your custom {@code SessionFactory} matching {@code EntityManagerFactory}.
 * An explicit qualifier for the original factory (as indicated above) is recommended here.
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see #setPersistenceUnitName
 * @see #setEntityManagerFactory
 * @deprecated as of Spring Framework 4.3.12 against Hibernate 5.2, in favor of a custom solution
 * based on {@link EntityManagerFactory#unwrap} with explicit qualifiers and/or primary markers
 */
@Deprecated
public class HibernateJpaSessionFactoryBean extends EntityManagerFactoryAccessor implements FactoryBean<SessionFactory> {

	@Override
	@Nullable
	public SessionFactory getObject() {
		EntityManagerFactory emf = getEntityManagerFactory();
		Assert.state(emf != null, "EntityManagerFactory must not be null");
		try {
			Method getSessionFactory = emf.getClass().getMethod("getSessionFactory");
			return (SessionFactory) ReflectionUtils.invokeMethod(getSessionFactory, emf);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException("No compatible Hibernate EntityManagerFactory found: " + ex);
		}
	}

	@Override
	public Class<?> getObjectType() {
		return SessionFactory.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
