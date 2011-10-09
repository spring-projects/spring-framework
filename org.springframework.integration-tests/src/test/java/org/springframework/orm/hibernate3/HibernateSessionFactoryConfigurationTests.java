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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.classic.Session;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBuilder;
import org.springframework.orm.hibernate3.scannable.Foo;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for configuring Hibernate SessionFactory types
 * without using a FactoryBean, e.g., within a {@link Configuration} class.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class HibernateSessionFactoryConfigurationTests {

	@Test
	public void usingLocalSessionFactoryBean() {
		saveAndRetriveEntity(LocalSessionFactoryBeanXmlConfig.class);
	}

	@Test
	public void usingAnnotationSessionFactoryBean() {
		saveAndRetriveEntity(AnnotationSessionFactoryBeanXmlConfig.class);
	}

	@Ignore @Test
	public void usingNativeHibernateConfiguration() {
		saveAndRetriveEntity(NativeHibernateConfig.class);
	}

	@Test
	public void usingSessionFactoryBuilder_withConfigurationCallback() {
		saveAndRetriveEntity(SessionFactoryConfig_withConfigurationCallback.class);
	}

	@Test
	public void usingAnnotationSessionFactoryBuilder() {
		saveAndRetriveEntity(AnnotationSessionFactoryConfig.class);
	}

	@Test
	public void usingAnnotationSessionFactoryBuilder_withConfigurationCallback() {
		saveAndRetriveEntity(AnnotationSessionFactoryConfig_withConfigurationCallback.class);
	}

	@Test
	public void usingAnnotationSessionFactoryBuilder_withPackagesToScan() {
		saveAndRetriveEntity(AnnotationSessionFactoryConfig_withPackagesToScan.class);
	}

	@Test
	public void usingAnnotationSessionFactoryBuilder_withAnnotatedClasses() {
		saveAndRetriveEntity(AnnotationSessionFactoryConfig_withAnnotatedClasses.class);
	}


	@Test(expected=DataAccessException.class)
	public void exceptionTranslation_withLocalSessionFactoryBean() {
		causeException(LocalSessionFactoryBeanXmlConfig.class, RepositoryConfig.class);
	}

	@Test(expected=DataAccessException.class)
	public void exceptionTranslation_withSessionFactoryBuilder() {
		causeException(SessionFactoryConfig_withConfigurationCallback.class,
				RepositoryConfig.class);
	}

	@Test
	public void usingSessionFactoryBuilder_withCustomConfigurationClass() {
		saveAndRetriveEntity(SessionFactoryConfig_withCustomConfigurationClass.class);
	}

	@Test(expected=IllegalStateException.class)
	public void usingSessionFactoryBuilder_withLateCustomConfigurationClass() throws Throwable {
		try {
			saveAndRetriveEntity(SessionFactoryConfig_withLateCustomConfigurationClass.class);
		} catch (BeanCreationException ex) {
			Throwable cause = ex.getRootCause();
			assertThat(cause.getMessage().startsWith("setConfigurationClass() must be called before"), is(true));
			throw cause;
		}
	}

	@Test
	public void builtSessionFactoryIsDisposableBeanProxy() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(AnnotationSessionFactoryConfig.class);
		SessionFactory sessionFactory = ctx.getBean(SessionFactory.class);
		assertThat(sessionFactory, instanceOf(DisposableBean.class));
		assertThat(sessionFactory.toString(), startsWith("DisposableBean proxy for SessionFactory"));
		ctx.close();
		assertTrue("SessionFactory was not closed as expected", sessionFactory.isClosed());
	}


	private void saveAndRetriveEntity(Class<?> configClass) {
		SessionFactory sessionFactory = new AnnotationConfigApplicationContext(configClass).getBean(SessionFactory.class);
		Session session = sessionFactory.openSession();
		Transaction tx = session.beginTransaction();
		Foo foo = new Foo();
		foo.setName("f1");
		session.save(foo);

		Foo f1 = (Foo) session.createQuery("from Foo where name = 'f1'").uniqueResult();
		assertThat("No foo with name='f1' found!", f1, notNullValue());
		assertThat(f1.getName(), is("f1"));

		tx.rollback();
	}

	private void causeException(Class<?>... configClasses) {
		FooRepository fooRepository = new AnnotationConfigApplicationContext(configClasses).getBean(FooRepository.class);
		fooRepository.findAll(); // will throw
	}


	@Configuration
	static class DataConfig {
		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder()
				.setType(EmbeddedDatabaseType.HSQL)
				.build();
		}
	}


	@Configuration
	@ImportResource("org/springframework/orm/hibernate3/LocalSessionFactoryBeanXmlConfig-context.xml")
	static class LocalSessionFactoryBeanXmlConfig extends DataConfig {
	}


	interface FooRepository {
		List<Foo> findAll();
	}

	@Repository
	public static class HibernateFooRepository implements FooRepository {
		private final SessionFactory sessionFactory;
		public HibernateFooRepository(SessionFactory sessionFactory) {
			this.sessionFactory = sessionFactory;
		}
		@Transactional
		@SuppressWarnings("unchecked")
		public List<Foo> findAll() {
			return sessionFactory.getCurrentSession().createQuery("from Bogus").list();
		}
	}


	@Configuration
	@EnableTransactionManagement
	static class RepositoryConfig {
		@Inject SessionFactory sessionFactory;

		@Bean
		FooRepository fooRepository() {
			return new HibernateFooRepository(sessionFactory);
		}

		@Bean
		PlatformTransactionManager txManager() {
			return new HibernateTransactionManager(sessionFactory);
		}

		@Bean
		PersistenceExceptionTranslationPostProcessor exceptionTranslationPostProcessor() {
			return new PersistenceExceptionTranslationPostProcessor();
		}

		@Bean
		PersistenceExceptionTranslator exceptionTranslator() {
			return new HibernateExceptionTranslator();
		}
	}


	@Configuration
	@ImportResource("org/springframework/orm/hibernate3/AnnotationSessionFactoryBeanXmlConfig-context.xml")
	static class AnnotationSessionFactoryBeanXmlConfig extends DataConfig {
	}


	@Configuration
	static class NativeHibernateConfig {
		@Bean
		SessionFactory sessionFactory() {
			org.hibernate.cfg.Configuration cfg = new AnnotationConfiguration();
			return cfg.buildSessionFactory();
		}
	}


	@Configuration
	static class AnnotationSessionFactoryConfig extends DataConfig {
		@Bean
		SessionFactory sessionFactory() throws Exception {
			return new AnnotationSessionFactoryBuilder(dataSource())
				.setSchemaUpdate(true)
				.doWithConfiguration(new HibernateConfigurationCallback<AnnotationConfiguration>() {
					public void configure(AnnotationConfiguration configuration) {
						configuration.addAnnotatedClass(Foo.class);
					}
				})
				.buildSessionFactory();
		}
	}


	@Configuration
	static class AnnotationSessionFactoryConfig_withPackagesToScan extends DataConfig {
		@Bean
		SessionFactory sessionFactory() throws Exception {
			return new AnnotationSessionFactoryBuilder(dataSource())
				.setSchemaUpdate(true)
				.setPackagesToScan(Foo.class.getPackage().getName())
				.buildSessionFactory();
		}
	}

	@Configuration
	static class AnnotationSessionFactoryConfig_withAnnotatedClasses extends DataConfig {
		@Bean
		SessionFactory sessionFactory() throws Exception {
			return new AnnotationSessionFactoryBuilder(dataSource())
				.setSchemaUpdate(true)
				.setAnnotatedClasses(Foo.class, Foo.class)
				.buildSessionFactory();
		}
	}


	@Configuration
	static class AnnotationSessionFactoryConfig_withConfigurationCallback extends DataConfig {
		@Bean
		SessionFactory sessionFactory() throws Exception {
			return new AnnotationSessionFactoryBuilder(dataSource())
				.setSchemaUpdate(true)
				.doWithConfiguration(new HibernateConfigurationCallback<AnnotationConfiguration>() {
					public void configure(AnnotationConfiguration configuration) throws Exception {
						configuration.addAnnotatedClass(Foo.class);
					}
				})
				.buildSessionFactory();
		}
	}


	@Configuration
	static class SessionFactoryConfig_withConfigurationCallback extends DataConfig {
		@Bean
		SessionFactory sessionFactory() throws Exception {
			return new SessionFactoryBuilder(dataSource())
				.setSchemaUpdate(true)
				.doWithConfiguration(new HibernateConfigurationCallback<org.hibernate.cfg.Configuration>() {
					public void configure(org.hibernate.cfg.Configuration configuration) throws Exception {
						configuration.addFile(new File(this.getClass().getClassLoader().getResource("org/springframework/orm/hibernate3/scannable/FooMapping.hbm.xml").toURI()));
					}
				})
				.buildSessionFactory();
		}
	}


	@Configuration
	static class SessionFactoryConfig_withCustomConfigurationClass extends DataConfig {
		@Bean
		SessionFactory sessionFactory() throws Exception {
			SessionFactoryBuilder sfb  = new SessionFactoryBuilder(dataSource())
				.setSchemaUpdate(true)
				.setConfigurationClass(CustomHibernateConfiguration.class)
				.doWithConfiguration(new HibernateConfigurationCallback<org.hibernate.cfg.Configuration>() {
					public void configure(org.hibernate.cfg.Configuration configuration) throws Exception {
						configuration.addFile(new File(this.getClass().getClassLoader().getResource("org/springframework/orm/hibernate3/scannable/FooMapping.hbm.xml").toURI()));
					}
				});
			assertThat(sfb.getConfiguration(), instanceOf(CustomHibernateConfiguration.class));
			return sfb.buildSessionFactory();
		}
	}


	@Configuration
	static class SessionFactoryConfig_withLateCustomConfigurationClass extends DataConfig {
		@Bean
		SessionFactory sessionFactory() throws Exception {
			return new SessionFactoryBuilder(dataSource())
				.setSchemaUpdate(true)
				.doWithConfiguration(new HibernateConfigurationCallback<org.hibernate.cfg.Configuration>() {
					public void configure(org.hibernate.cfg.Configuration configuration) throws Exception {
						configuration.addFile(new File(this.getClass().getClassLoader().getResource("org/springframework/orm/hibernate3/scannable/FooMapping.hbm.xml").toURI()));
					}
				})
				.setConfigurationClass(CustomHibernateConfiguration.class)
				.buildSessionFactory();
		}
	}


	@SuppressWarnings("serial")
	static class CustomHibernateConfiguration extends org.hibernate.cfg.Configuration {
	}

}
