/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.orm.hibernate3;

import javax.sql.DataSource;

import org.hibernate.cfg.Configuration;
import org.springframework.context.annotation.Bean;

/**
 * Hibernate {@link Configuration} builder suitable for use within Spring
 * {@link org.springframework.context.annotation.Configuration @Configuration}
 * class {@link Bean @Bean} methods. For complete details on features, see the
 * JavaDoc for the {@link SessionFactoryBuilderSupport} superclass. For use in
 * Spring XML configuration, see the {@link LocalSessionFactoryBean} subclass.
 *
 * <p>As noted in {@code SessionFactoryBuilderSupport} JavaDoc, this class requires
 * Hibernate 3.2 or later; it additionally requires that the Java Persistence API
 * and Hibernate Annotations add-ons are present.
 *
 * <p>Setter methods return the builder instance in order to facilitate
 * a concise and convenient method-chaining style. For example:
 * <pre class="code">
 * {@code @Configuration}
 * public class DataConfig {
 *     {@code @Bean}
 *     public SessionFactory sessionFactory() {
 *         return new SessionFactoryBean()
 *             .setDataSource(dataSource())
 *             .setMappingLocations("classpath:com/myco/*.hbm.xml"})
 *             .buildSessionFactory();
 *     }
 * }
 * </pre>
 *
 * <p>Most Hibernate configuration operations can be performed directly against
 * this API; however you may also access access and configure the underlying
 * {@code Configuration} object by using the {@link #doWithConfiguration} method
 * and providing a {@code HibernateConfigurationCallback} as follows:
 * <pre class="code">
 * SessionFactory sessionFactory =
 *     new SessionFactoryBuilder()
 *         // ...
 *         .doWithConfiguration(new HibernateConfigurationCallback&lt;Configuration&gt;() {
 *             public void configure(Configuration cfg) {
 *                 cfg.setNamingStrategy(MyNamingStrategy.class);
 *             }
 *          })
 *         .buildSessionFactory();
 * </pre>
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.1
 * @see SessionFactoryBuilderSupport
 * @see LocalSessionFactoryBean
 * @see org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBuilder
 * @see org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean
 */
public class SessionFactoryBuilder extends SessionFactoryBuilderSupport<SessionFactoryBuilder> {

	/**
	 * Construct a new {@code SessionFactoryBuilder}
	 */
	public SessionFactoryBuilder() {
		super();
	}

	/**
	 * Construct a new {@code SessionFactoryBuilder} with the given
	 * Spring-managed {@code DataSource} instance.
	 * @see #setDataSource
	 */
	public SessionFactoryBuilder(DataSource dataSource) {
		super(dataSource);
	}

	/**
	 * {@inheritDoc}
	 * <p>This implementation returns {@code org.hibernate.cfg.Configuration}
	 */
	@Override
	protected Class<? extends Configuration> getDefaultConfigurationClass() {
		return Configuration.class;
	}

}
