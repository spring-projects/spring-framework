/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.transaction.aspectj;

import java.io.IOException;

import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.tests.transaction.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * @author Stephane Nicoll
 */
@SpringJUnitConfig(JtaTransactionAspectsTests.Config.class)
public class JtaTransactionAspectsTests {

	@Autowired
	private CallCountingTransactionManager txManager;

	@BeforeEach
	public void setUp() {
		this.txManager.clear();
	}

	@Test
	public void commitOnAnnotatedPublicMethod() throws Throwable {
		assertThat(this.txManager.begun).isEqualTo(0);
		new JtaAnnotationPublicAnnotatedMember().echo(null);
		assertThat(this.txManager.commits).isEqualTo(1);
	}

	@Test
	public void matchingRollbackOnApplied() throws Throwable {
		assertThat(this.txManager.begun).isEqualTo(0);
		InterruptedException test = new InterruptedException();
		assertThatExceptionOfType(InterruptedException.class).isThrownBy(() ->
				new JtaAnnotationPublicAnnotatedMember().echo(test))
			.isSameAs(test);
		assertThat(this.txManager.rollbacks).isEqualTo(1);
		assertThat(this.txManager.commits).isEqualTo(0);
	}

	@Test
	public void nonMatchingRollbackOnApplied() throws Throwable {
		assertThat(this.txManager.begun).isEqualTo(0);
		IOException test = new IOException();
		assertThatIOException().isThrownBy(() ->
				new JtaAnnotationPublicAnnotatedMember().echo(test))
			.isSameAs(test);
		assertThat(this.txManager.commits).isEqualTo(1);
		assertThat(this.txManager.rollbacks).isEqualTo(0);
	}

	@Test
	public void commitOnAnnotatedProtectedMethod() {
		assertThat(this.txManager.begun).isEqualTo(0);
		new JtaAnnotationProtectedAnnotatedMember().doInTransaction();
		assertThat(this.txManager.commits).isEqualTo(1);
	}

	@Test
	public void nonAnnotatedMethodCallingProtectedMethod() {
		assertThat(this.txManager.begun).isEqualTo(0);
		new JtaAnnotationProtectedAnnotatedMember().doSomething();
		assertThat(this.txManager.commits).isEqualTo(1);
	}

	@Test
	public void commitOnAnnotatedPrivateMethod() {
		assertThat(this.txManager.begun).isEqualTo(0);
		new JtaAnnotationPrivateAnnotatedMember().doInTransaction();
		assertThat(this.txManager.commits).isEqualTo(1);
	}

	@Test
	public void nonAnnotatedMethodCallingPrivateMethod() {
		assertThat(this.txManager.begun).isEqualTo(0);
		new JtaAnnotationPrivateAnnotatedMember().doSomething();
		assertThat(this.txManager.commits).isEqualTo(1);
	}

	@Test
	public void notTransactional() {
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
