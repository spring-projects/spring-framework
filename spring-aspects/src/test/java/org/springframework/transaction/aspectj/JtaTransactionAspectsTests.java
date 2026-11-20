/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.aspectj;

import java.io.IOException;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * @author Stephane Nicoll
 */
@SpringJUnitConfig(JtaTransactionAspectsTests.Config.class)
class JtaTransactionAspectsTests {

	@Autowired
	private CallCountingTransactionManager txManager;

	@BeforeEach
	void setUp() {
		this.txManager.clear();
	}

	@Test
	void commitOnAnnotatedPublicMethod() throws Throwable {
		assertThat(this.txManager.begun).isEqualTo(0);
		new JtaAnnotationPublicAnnotatedMember().echo(null);
		assertThat(this.txManager.commits).isEqualTo(1);
	}

	@Test
	void matchingRollbackOnApplied() {
		assertThat(this.txManager.begun).isEqualTo(0);
		InterruptedException test = new InterruptedException();
		assertThatExceptionOfType(InterruptedException.class).isThrownBy(() ->
				new JtaAnnotationPublicAnnotatedMember().echo(test))
			.isSameAs(test);
		assertThat(this.txManager.rollbacks).isEqualTo(1);
		assertThat(this.txManager.commits).isEqualTo(0);
	}

	@Test
	void nonMatchingRollbackOnApplied() {
		assertThat(this.txManager.begun).isEqualTo(0);
		IOException test = new IOException();
		assertThatIOException().isThrownBy(() ->
				new JtaAnnotationPublicAnnotatedMember().echo(test))
			.isSameAs(test);
		assertThat(this.txManager.commits).isEqualTo(1);
		assertThat(this.txManager.rollbacks).isEqualTo(0);
	}

	@Test
	void commitOnAnnotatedProtectedMethod() {
		assertThat(this.txManager.begun).isEqualTo(0);
		new JtaAnnotationProtectedAnnotatedMember().doInTransaction();
		assertThat(this.txManager.commits).isEqualTo(1);
	}

	@Test
	void nonAnnotatedMethodCallingProtectedMethod() {
		assertThat(this.txManager.begun).isEqualTo(0);
		new JtaAnnotationProtectedAnnotatedMember().doSomething();
		assertThat(this.txManager.commits).isEqualTo(1);
	}

	@Test
	void commitOnAnnotatedPrivateMethod() {
		assertThat(this.txManager.begun).isEqualTo(0);
		new JtaAnnotationPrivateAnnotatedMember().doInTransaction();
		assertThat(this.txManager.commits).isEqualTo(1);
	}

	@Test
	void nonAnnotatedMethodCallingPrivateMethod() {
		assertThat(this.txManager.begun).isEqualTo(0);
		new JtaAnnotationPrivateAnnotatedMember().doSomething();
		assertThat(this.txManager.commits).isEqualTo(1);
	}

	@Test
	void notTransactional() {
		assertThat(this.txManager.begun).isEqualTo(0);
		new TransactionAspectTests.NotTransactional().noop();
		assertThat(this.txManager.begun).isEqualTo(0);
	}


	public static class JtaAnnotationPublicAnnotatedMember {

		@Transactional(rollbackOn = InterruptedException.class)
		public void echo(Throwable t) throws Throwable {
			if (t != null) {
				throw t;
			}
		}

	}


	protected static class JtaAnnotationProtectedAnnotatedMember {

		public void doSomething() {
			doInTransaction();
		}

		@Transactional
		protected void doInTransaction() {
		}
	}


	protected static class JtaAnnotationPrivateAnnotatedMember {

		public void doSomething() {
			doInTransaction();
		}

		@Transactional
		private void doInTransaction() {
		}
	}


	@Configuration
	protected static class Config {

		@Bean
		public CallCountingTransactionManager transactionManager() {
			return new CallCountingTransactionManager();
		}

		@Bean
		public JtaAnnotationTransactionAspect transactionAspect() {
			JtaAnnotationTransactionAspect aspect = JtaAnnotationTransactionAspect.aspectOf();
			aspect.setTransactionManager(transactionManager());
			return aspect;
		}
	}

}
