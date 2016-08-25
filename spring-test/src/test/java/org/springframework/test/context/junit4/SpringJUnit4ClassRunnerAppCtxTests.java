/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.context.junit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.GenericXmlContextLoader;
import org.springframework.tests.sample.beans.Employee;
import org.springframework.tests.sample.beans.Pet;

import static org.junit.Assert.*;

/**
 * SpringJUnit4ClassRunnerAppCtxTests serves as a <em>proof of concept</em>
 * JUnit 4 based test class, which verifies the expected functionality of
 * {@link SpringRunner} in conjunction with the following:
 *
 * <ul>
 * <li>{@link ContextConfiguration @ContextConfiguration}</li>
 * <li>{@link Autowired @Autowired}</li>
 * <li>{@link Qualifier @Qualifier}</li>
 * <li>{@link Resource @Resource}</li>
 * <li>{@link Value @Value}</li>
 * <li>{@link Inject @Inject}</li>
 * <li>{@link Named @Named}</li>
 * <li>{@link ApplicationContextAware}</li>
 * <li>{@link BeanNameAware}</li>
 * <li>{@link InitializingBean}</li>
 * </ul>
 *
 * <p>Since no application context resource
 * {@link ContextConfiguration#locations() locations} are explicitly declared
 * and since the {@link ContextConfiguration#loader() ContextLoader} is left set
 * to the default value of {@link GenericXmlContextLoader}, this test class's
 * dependencies will be injected via {@link Autowired @Autowired},
 * {@link Inject @Inject}, and {@link Resource @Resource} from beans defined in
 * the {@link ApplicationContext} loaded from the default classpath resource:
 * {@value #DEFAULT_CONTEXT_RESOURCE_PATH}.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see AbsolutePathSpringJUnit4ClassRunnerAppCtxTests
 * @see RelativePathSpringJUnit4ClassRunnerAppCtxTests
 * @see InheritedConfigSpringJUnit4ClassRunnerAppCtxTests
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
public class SpringJUnit4ClassRunnerAppCtxTests implements ApplicationContextAware, BeanNameAware, InitializingBean {

	/**
	 * Default resource path for the application context configuration for
	 * {@link SpringJUnit4ClassRunnerAppCtxTests}: {@value #DEFAULT_CONTEXT_RESOURCE_PATH}
	 */
	public static final String DEFAULT_CONTEXT_RESOURCE_PATH = "/org/springframework/test/context/junit4/SpringJUnit4ClassRunnerAppCtxTests-context.xml";

	private ApplicationContext applicationContext;

	private boolean beanInitialized = false;

	private String beanName = "replace me with [" + getClass().getName() + "]";

	private Employee employee;

	@Autowired
	private Pet autowiredPet;

	@Inject
	private Pet injectedPet;

	@Autowired(required = false)
	protected Long nonrequiredLong;

	@Resource
	protected String foo;

	protected String bar;

	@Value("enigma")
	private String literalFieldValue;

	@Value("#{2 == (1+1)}")
	private Boolean spelFieldValue;

	private String literalParameterValue;

	private Boolean spelParameterValue;

	@Autowired
	@Qualifier("quux")
	protected String quux;

	@Inject
	@Named("quux")
	protected String namedQuux;


	// ------------------------------------------------------------------------|

	@Override
	public final void afterPropertiesSet() throws Exception {
		this.beanInitialized = true;
	}

	@Override
	public final void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public final void setBeanName(final String beanName) {
		this.beanName = beanName;
	}

	@Autowired
	protected final void setEmployee(final Employee employee) {
		this.employee = employee;
	}

	@Resource
	protected final void setBar(final String bar) {
		this.bar = bar;
	}

	@Autowired
	public void setLiteralParameterValue(@Value("enigma") String literalParameterValue) {
		this.literalParameterValue = literalParameterValue;
	}

	@Autowired
	public void setSpelParameterValue(@Value("#{2 == (1+1)}") Boolean spelParameterValue) {
		this.spelParameterValue = spelParameterValue;
	}

	// ------------------------------------------------------------------------|

	@Test
	public final void verifyApplicationContextSet() {
		assertNotNull("The application context should have been set due to ApplicationContextAware semantics.",
			this.applicationContext);
	}

	@Test
	public final void verifyBeanInitialized() {
		assertTrue("This test bean should have been initialized due to InitializingBean semantics.",
			this.beanInitialized);
	}

	@Test
	public final void verifyBeanNameSet() {
		assertEquals("The bean name of this test instance should have been set due to BeanNameAware semantics.",
			getClass().getName(), this.beanName);
	}

	@Test
	public final void verifyAnnotationAutowiredAndInjectedFields() {
		assertNull("The nonrequiredLong field should NOT have been autowired.", this.nonrequiredLong);
		assertEquals("The quux field should have been autowired via @Autowired and @Qualifier.", "Quux", this.quux);
		assertEquals("The namedFoo field should have been injected via @Inject and @Named.", "Quux", this.namedQuux);
		assertSame("@Autowired/@Qualifier and @Inject/@Named quux should be the same object.", this.quux,
			this.namedQuux);

		assertNotNull("The pet field should have been autowired.", this.autowiredPet);
		assertNotNull("The pet field should have been injected.", this.injectedPet);
		assertEquals("Fido", this.autowiredPet.getName());
		assertEquals("Fido", this.injectedPet.getName());
		assertSame("@Autowired and @Inject pet should be the same object.", this.autowiredPet, this.injectedPet);
	}

	@Test
	public final void verifyAnnotationAutowiredMethods() {
		assertNotNull("The employee setter method should have been autowired.", this.employee);
		assertEquals("John Smith", this.employee.getName());
	}

	@Test
	public final void verifyAutowiredAtValueFields() {
		assertNotNull("Literal @Value field should have been autowired", this.literalFieldValue);
		assertNotNull("SpEL @Value field should have been autowired.", this.spelFieldValue);
		assertEquals("enigma", this.literalFieldValue);
		assertEquals(Boolean.TRUE, this.spelFieldValue);
	}

	@Test
	public final void verifyAutowiredAtValueMethods() {
		assertNotNull("Literal @Value method parameter should have been autowired.", this.literalParameterValue);
		assertNotNull("SpEL @Value method parameter should have been autowired.", this.spelParameterValue);
		assertEquals("enigma", this.literalParameterValue);
		assertEquals(Boolean.TRUE, this.spelParameterValue);
	}

	@Test
	public final void verifyResourceAnnotationInjectedFields() {
		assertEquals("The foo field should have been injected via @Resource.", "Foo", this.foo);
	}

	@Test
	public final void verifyResourceAnnotationInjectedMethods() {
		assertEquals("The bar method should have been wired via @Resource.", "Bar", this.bar);
	}

	@Test
	public final void verifyMultipleThreadsHaveDifferentTestContextManager() throws Exception {

		final SpringJUnit4ClassRunner runner = new SpringJUnit4ClassRunner(getClass());
		final Set<TestContextManager> managers = new HashSet<>();
		class SimpleRunner implements Runnable {

			private volatile TestContextManager tcm;

			@Override
			public void run() {
				try {
					managers.add(runner.getTestContextManager());
				} catch (Exception e) {
					fail("Exception not expected.");
				}
			}
		}

		SimpleRunner r1 = new SimpleRunner();
		SimpleRunner r2 = new SimpleRunner();
		SimpleRunner r3 = new SimpleRunner();

		runConcurrent("Running tests concurrently.", Arrays.asList(r1, r2, r3), 25);

		assertEquals(3, managers.size());


	}

	/*
	 * This code was borrowed from https://github.com/junit-team/junit4/wiki/multithreaded-code-and-concurrency
	 */
	private void runConcurrent(final String message, final List<? extends Runnable> runnables, final int maxTimeoutSeconds) throws InterruptedException {
		final int numThreads = runnables.size();
		final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>());
		final ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
		try {
			final CountDownLatch allExecutorThreadsReady = new CountDownLatch(numThreads);
			final CountDownLatch afterInitBlocker = new CountDownLatch(1);
			final CountDownLatch allDone = new CountDownLatch(numThreads);
			runnables.forEach(runnable ->
				threadPool.submit(() -> {
                    allExecutorThreadsReady.countDown();
                    try {
                        afterInitBlocker.await();
	                    runnable.run();
                    } catch (final Throwable e) {
                        exceptions.add(e);
                    } finally {
                        allDone.countDown();
                    }
                }));
			// wait until all threads are ready
			assertTrue("Timeout initializing threads! Perform long lasting initializations before passing runnables to runConcurrent", allExecutorThreadsReady.await(runnables.size() * 10, TimeUnit.MILLISECONDS));
			// start all test runners
			afterInitBlocker.countDown();
			assertTrue(message +" timeout! More than" + maxTimeoutSeconds + "seconds", allDone.await(maxTimeoutSeconds, TimeUnit.SECONDS));
		} finally {
			threadPool.shutdownNow();
		}
		assertTrue(message + "failed with exception(s)" + exceptions, exceptions.isEmpty());
	}

}