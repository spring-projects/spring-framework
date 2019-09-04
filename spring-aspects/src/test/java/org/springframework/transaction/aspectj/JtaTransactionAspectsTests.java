/*
 * Copyright 2002-2015 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.tests.transaction.CallCountingTransactionManager;

import static org.junit.Assert.*;

/**
 * @author Stephane Nicoll
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = JtaTransactionAspectsTests.Config.class)
public class JtaTransactionAspectsTests {

	@Autowired
	private CallCountingTransactionManager txManager;

	@Before
	public void setUp() {
		this.txManager.clear();
	}

	@Test
	public void commitOnAnnotatedPublicMethod() throws Throwable {
		assertEquals(0, this.txManager.begun);
		new JtaAnnotationPublicAnnotatedMember().echo(null);
		assertEquals(1, this.txManager.commits);
	}

	@Test
	public void matchingRollbackOnApplied() throws Throwable {
		assertEquals(0, this.txManager.begun);
		InterruptedException test = new InterruptedException();
		try {
			new JtaAnnotationPublicAnnotatedMember().echo(test);
			fail("Should have thrown an exception");
		}
		catch (Throwable throwable) {
			assertEquals("wrong exception", test, throwable);
		}
		assertEquals(1, this.txManager.rollbacks);
		assertEquals(0, this.txManager.commits);
	}

	@Test
	public void nonMatchingRollbackOnApplied() throws Throwable {
		assertEquals(0, this.txManager.begun);
		IOException test = new IOException();
		try {
			new JtaAnnotationPublicAnnotatedMember().echo(test);
			fail("Should have thrown an exception");
		}
		catch (Throwable throwable) {
			assertEquals("wrong exception", test, throwable);
		}
		assertEquals(1, this.txManager.commits);
		assertEquals(0, this.txManager.rollbacks);
	}

	@Test
	public void commitOnAnnotatedProtectedMethod() {
		assertEquals(0, this.txManager.begun);
		new JtaAnnotationProtectedAnnotatedMember().doInTransaction();
		assertEquals(1, this.txManager.commits);
	}

	@Test
	public void nonAnnotatedMethodCallingProtectedMethod() {
		assertEquals(0, this.txManager.begun);
		new JtaAnnotationProtectedAnnotatedMember().doSomething();
		assertEquals(1, this.txManager.commits);
	}

	@Test
	public void commitOnAnnotatedPrivateMethod() {
		assertEquals(0, this.txManager.begun);
		new JtaAnnotationPrivateAnnotatedMember().doInTransaction();
		assertEquals(1, this.txManager.commits);
	}

	@Test
	public void nonAnnotatedMethodCallingPrivateMethod() {
		assertEquals(0, this.txManager.begun);
		new JtaAnnotationPrivateAnnotatedMember().doSomething();
		assertEquals(1, this.txManager.commits);
	}

	@Test
	public void notTransactional() {
		assertEquals(0, this.txManager.begun);
		new TransactionAspectTests.NotTransactional().noop();
		assertEquals(0, this.txManager.begun);
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
