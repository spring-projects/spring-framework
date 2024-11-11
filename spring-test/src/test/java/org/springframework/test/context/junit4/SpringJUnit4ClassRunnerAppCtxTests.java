/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.context.junit4;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.testfixture.beans.Employee;
import org.springframework.beans.testfixture.beans.Pet;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.GenericXmlContextLoader;

import static org.assertj.core.api.Assertions.assertThat;

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
	 * {@link SpringJUnit4ClassRunnerAppCtxTests}: {@value}
	 */
	public static final String DEFAULT_CONTEXT_RESOURCE_PATH =
			"/org/springframework/test/context/junit4/SpringJUnit4ClassRunnerAppCtxTests-context.xml";


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

	private String beanName;

	private ApplicationContext applicationContext;

	private boolean beanInitialized = false;


	@Autowired
	protected void setEmployee(Employee employee) {
		this.employee = employee;
	}

	@Resource
	protected void setBar(String bar) {
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

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() {
		this.beanInitialized = true;
	}


	@Test
	public void verifyBeanNameSet() {
		assertThat(this.beanName).as("The bean name of this test instance should have been set due to BeanNameAware semantics.")
				.startsWith(getClass().getName());
	}

	@Test
	public void verifyApplicationContextSet() {
		assertThat(this.applicationContext).as("The application context should have been set due to ApplicationContextAware semantics.").isNotNull();
	}

	@Test
	public void verifyBeanInitialized() {
		assertThat(this.beanInitialized).as("This test bean should have been initialized due to InitializingBean semantics.").isTrue();
	}

	@Test
	public void verifyAnnotationAutowiredAndInjectedFields() {
		assertThat(this.nonrequiredLong).as("The nonrequiredLong field should NOT have been autowired.").isNull();
		assertThat(this.quux).as("The quux field should have been autowired via @Autowired and @Qualifier.").isEqualTo("Quux");
		assertThat(this.namedQuux).as("The namedFoo field should have been injected via @Inject and @Named.").isEqualTo("Quux");
		assertThat(this.namedQuux).as("@Autowired/@Qualifier and @Inject/@Named quux should be the same object.").isSameAs(this.quux);

		assertThat(this.autowiredPet).as("The pet field should have been autowired.").isNotNull();
		assertThat(this.injectedPet).as("The pet field should have been injected.").isNotNull();
		assertThat(this.autowiredPet.getName()).isEqualTo("Fido");
		assertThat(this.injectedPet.getName()).isEqualTo("Fido");
		assertThat(this.injectedPet).as("@Autowired and @Inject pet should be the same object.").isSameAs(this.autowiredPet);
	}

	@Test
	public void verifyAnnotationAutowiredMethods() {
		assertThat(this.employee).as("The employee setter method should have been autowired.").isNotNull();
		assertThat(this.employee.getName()).isEqualTo("John Smith");
	}

	@Test
	public void verifyAutowiredAtValueFields() {
		assertThat(this.literalFieldValue).as("Literal @Value field should have been autowired").isNotNull();
		assertThat(this.spelFieldValue).as("SpEL @Value field should have been autowired.").isNotNull();
		assertThat(this.literalFieldValue).isEqualTo("enigma");
		assertThat(this.spelFieldValue).isTrue();
	}

	@Test
	public void verifyAutowiredAtValueMethods() {
		assertThat(this.literalParameterValue).as("Literal @Value method parameter should have been autowired.").isNotNull();
		assertThat(this.spelParameterValue).as("SpEL @Value method parameter should have been autowired.").isNotNull();
		assertThat(this.literalParameterValue).isEqualTo("enigma");
		assertThat(this.spelParameterValue).isTrue();
	}

	@Test
	public void verifyResourceAnnotationInjectedFields() {
		assertThat(this.foo).as("The foo field should have been injected via @Resource.").isEqualTo("Foo");
	}

	@Test
	public void verifyResourceAnnotationInjectedMethods() {
		assertThat(this.bar).as("The bar method should have been wired via @Resource.").isEqualTo("Bar");
	}

}
