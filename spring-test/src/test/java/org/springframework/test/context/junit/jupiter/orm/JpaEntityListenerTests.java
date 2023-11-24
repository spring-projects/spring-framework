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

package org.springframework.test.context.junit.jupiter.orm;

import java.util.List;

import javax.sql.DataSource;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.junit.jupiter.orm.domain.JpaPersonRepository;
import org.springframework.test.context.junit.jupiter.orm.domain.Person;
import org.springframework.test.context.junit.jupiter.orm.domain.PersonListener;
import org.springframework.test.context.junit.jupiter.orm.domain.PersonRepository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Transactional tests for JPA entity listener support (a.k.a. lifecycle callback
 * methods).
 *
 * @author Sam Brannen
 * @since 5.3.18
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/28228">issue gh-28228</a>
 * @see org.springframework.test.context.junit4.orm.HibernateSessionFlushingTests
 */
@SpringJUnitConfig
@Transactional
@Sql(statements = "insert into person(id, name) values(0, 'Jane')")
class JpaEntityListenerTests {

	@PersistenceContext
	EntityManager entityManager;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	PersonRepository repo;


	@BeforeEach
	void setUp() {
		assertPeople("Jane");
		PersonListener.methodsInvoked.clear();
	}

	@Test
	void find() {
		Person jane = repo.findByName("Jane");
		assertCallbacks("@PostLoad: Jane");

		// Does not cause an additional @PostLoad
		repo.findById(jane.getId());
		assertCallbacks("@PostLoad: Jane");

		// Clear to cause a new @PostLoad
		entityManager.clear();
		repo.findById(jane.getId());
		assertCallbacks("@PostLoad: Jane", "@PostLoad: Jane");
	}

	@Test
	void save() {
		Person john = repo.save(new Person("John"));
		assertCallbacks("@PrePersist: John");

		// Flush to cause a @PostPersist
		entityManager.flush();
		assertPeople("Jane", "John");
		assertCallbacks("@PrePersist: John", "@PostPersist: John");

		// Does not cause a @PostLoad
		repo.findById(john.getId());
		assertCallbacks("@PrePersist: John", "@PostPersist: John");

		// Clear to cause a @PostLoad
		entityManager.clear();
		repo.findById(john.getId());
		assertCallbacks("@PrePersist: John", "@PostPersist: John", "@PostLoad: John");
	}

	@Test
	void update() {
		Person jane = repo.findByName("Jane");
		assertCallbacks("@PostLoad: Jane");

		jane.setName("Jane Doe");
		// Does not cause a @PreUpdate or @PostUpdate
		repo.save(jane);
		assertCallbacks("@PostLoad: Jane");

		// Flush to cause a @PreUpdate and @PostUpdate
		entityManager.flush();
		assertPeople("Jane Doe");
		assertCallbacks("@PostLoad: Jane", "@PreUpdate: Jane Doe", "@PostUpdate: Jane Doe");
	}

	@Test
	void remove() {
		Person jane = repo.findByName("Jane");
		assertCallbacks("@PostLoad: Jane");

		// Does not cause a @PostRemove
		repo.remove(jane);
		assertCallbacks("@PostLoad: Jane", "@PreRemove: Jane");

		// Flush to cause a @PostRemove
		entityManager.flush();
		assertPeople();
		assertCallbacks("@PostLoad: Jane", "@PreRemove: Jane", "@PostRemove: Jane");
	}

	private void assertCallbacks(String... callbacks) {
		assertThat(PersonListener.methodsInvoked).containsExactly(callbacks);
	}

	private void assertPeople(String... expectedNames) {
		List<String> names = this.jdbcTemplate.queryForList("select name from person", String.class);
		if (expectedNames.length == 0) {
			assertThat(names).isEmpty();
		}
		else {
			assertThat(names).containsExactlyInAnyOrder(expectedNames);
		}
	}


	@Configuration(proxyBeanMethods = false)
	@EnableTransactionManagement
	static class Config {

		@Bean
		PersonRepository personRepository() {
			return new JpaPersonRepository();
		}

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().generateUniqueName(true).build();
		}

		@Bean
		JdbcTemplate jdbcTemplate(DataSource dataSource) {
			return new JdbcTemplate(dataSource);
		}

		@Bean
		LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
			LocalContainerEntityManagerFactoryBean emfb = new LocalContainerEntityManagerFactoryBean();
			emfb.setDataSource(dataSource);
			emfb.setPackagesToScan(Person.class.getPackage().getName());
			HibernateJpaVendorAdapter hibernateJpaVendorAdapter = new HibernateJpaVendorAdapter();
			hibernateJpaVendorAdapter.setGenerateDdl(true);
			hibernateJpaVendorAdapter.setDatabase(Database.HSQL);
			emfb.setJpaVendorAdapter(hibernateJpaVendorAdapter);
			return emfb;
		}

		@Bean
		JpaTransactionManager transactionManager(EntityManagerFactory emf) {
			return new JpaTransactionManager(emf);
		}

	}

}
