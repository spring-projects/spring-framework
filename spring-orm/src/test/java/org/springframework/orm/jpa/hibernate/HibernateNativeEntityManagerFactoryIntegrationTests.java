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

package org.springframework.orm.jpa.hibernate;

import java.util.List;

import javax.sql.DataSource;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.query.Query;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.AbstractContainerEntityManagerFactoryIntegrationTests;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.orm.jpa.domain.Person;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hibernate-specific JPA tests with native SessionFactory setup and getCurrentSession interaction.
 *
 * @author Juergen Hoeller
 * @since 5.1
 */
class HibernateNativeEntityManagerFactoryIntegrationTests extends AbstractContainerEntityManagerFactoryIntegrationTests {

	@Autowired
	private SessionFactory sessionFactory;

	@Autowired
	private Session sharedSession;

	@Autowired
	private StatelessSession statelessSession;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private ApplicationContext applicationContext;


	@Override
	protected String[] getConfigLocations() {
		return new String[] {"/org/springframework/orm/jpa/hibernate/hibernate-manager-native.xml",
				"/org/springframework/orm/jpa/memdb.xml", "/org/springframework/orm/jpa/inject.xml"};
	}


	@Test
	@Override
	protected void testEntityManagerFactoryImplementsEntityManagerFactoryInfo() {
		assertThat(entityManagerFactory).as("Must not have introduced config interface")
				.isNotInstanceOf(EntityManagerFactoryInfo.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testEntityListener() {
		String firstName = "Tony";
		insertPerson(firstName);

		List<Person> people = sharedEntityManager.createQuery("select p from Person as p", Person.class).getResultList();
		assertThat(people).hasSize(1);
		assertThat(people.get(0).getFirstName()).isEqualTo(firstName);
		assertThat(people.get(0).postLoaded).isSameAs(applicationContext);
	}

	@Test
	public void testCurrentSession() {
		String firstName = "Tony";
		insertPerson(firstName);

		Query<Person> q = sessionFactory.getCurrentSession().createQuery("select p from Person as p", Person.class);
		assertThat(q.getResultList()).hasSize(1);
		assertThat(q.getResultList().get(0).getFirstName()).isEqualTo(firstName);
		assertThat(q.getResultList().get(0).postLoaded).isSameAs(applicationContext);
	}

	@Test
	public void testSharedSession() {
		String firstName = "Tony";
		insertPerson(firstName);

		Query<Person> q = sharedSession.createQuery("select p from Person as p", Person.class);
		assertThat(q.getResultList()).hasSize(1);
		assertThat(q.getResultList().get(0).getFirstName()).isEqualTo(firstName);
		assertThat(q.getResultList().get(0).postLoaded).isSameAs(applicationContext);

		endTransaction();

		DataSourceTransactionManager dstm = new DataSourceTransactionManager(dataSource);
		new TransactionTemplate(dstm).execute(status -> {
			insertPerson(firstName);
			Query<Person> q2 = sharedSession.createQuery("select p from Person as p", Person.class);
			assertThat(q2.getResultList()).hasSize(1);
			assertThat(q2.getResultList().get(0).getFirstName()).isEqualTo(firstName);
			assertThat(q2.getResultList().get(0).postLoaded).isSameAs(applicationContext);
			Query<Person> q3 = statelessSession.createQuery("select p from Person as p", Person.class);
			assertThat(q3.getResultList()).hasSize(1);
			assertThat(q3.getResultList().get(0).getFirstName()).isEqualTo(firstName);
			status.setRollbackOnly();
			return null;
		});
	}

	@Test
	public void testStatelessSession() {
		String firstName = "Tony";
		insertPerson(firstName);

		Query<Person> q = statelessSession.createQuery("select p from Person as p", Person.class);
		assertThat(q.getResultList()).hasSize(1);
		assertThat(q.getResultList().get(0).getFirstName()).isEqualTo(firstName);

		endTransaction();

		DataSourceTransactionManager dstm = new DataSourceTransactionManager(dataSource);
		new TransactionTemplate(dstm).execute(status -> {
			insertPerson(firstName);
			Query<Person> q2 = statelessSession.createQuery("select p from Person as p", Person.class);
			assertThat(q2.getResultList()).hasSize(1);
			assertThat(q2.getResultList().get(0).getFirstName()).isEqualTo(firstName);
			Query<Person> q3 = sharedSession.createQuery("select p from Person as p", Person.class);
			assertThat(q3.getResultList()).hasSize(1);
			assertThat(q3.getResultList().get(0).getFirstName()).isEqualTo(firstName);
			status.setRollbackOnly();
			return null;
		});
	}

	@Test  // SPR-16956
	public void testReadOnly() {
		assertThat(sessionFactory.getCurrentSession().getHibernateFlushMode()).isSameAs(FlushMode.AUTO);
		assertThat(sessionFactory.getCurrentSession().isDefaultReadOnly()).isFalse();
		endTransaction();

		this.transactionDefinition.setReadOnly(true);
		startNewTransaction();
		assertThat(sessionFactory.getCurrentSession().getHibernateFlushMode()).isSameAs(FlushMode.MANUAL);
		assertThat(sessionFactory.getCurrentSession().isDefaultReadOnly()).isTrue();
	}

}
