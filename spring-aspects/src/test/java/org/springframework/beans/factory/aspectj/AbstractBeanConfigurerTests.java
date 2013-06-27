/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.beans.factory.aspectj;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

import junit.framework.TestCase;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * @author Adrian Colyer
 * @author Rod Johnson
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public abstract class AbstractBeanConfigurerTests extends TestCase {

	protected ConfigurableApplicationContext context;

	@Override
	protected void setUp() throws Exception {
		this.context = createContext();
	}

	protected abstract ConfigurableApplicationContext createContext();

	public void testConfigurableWithExplicitBeanName() {
		ShouldBeConfiguredBySpring myObject = new ShouldBeConfiguredBySpring();
		assertEquals("Rod", myObject.getName());
	}

	public void testWithoutAnnotation() {
		ShouldNotBeConfiguredBySpring myObject = new ShouldNotBeConfiguredBySpring();
		assertNull("Name should not have been set", myObject.getName());
	}

	public void testConfigurableWithImplicitBeanName() {
		ShouldBeConfiguredBySpringUsingTypeNameAsBeanName myObject =
				new ShouldBeConfiguredBySpringUsingTypeNameAsBeanName();
		assertEquals("Rob", myObject.getName());
	}

	public void testConfigurableUsingAutowireByType() {
		ShouldBeConfiguredBySpringUsingAutowireByType myObject =
				new ShouldBeConfiguredBySpringUsingAutowireByType();
		assertNotNull(myObject.getFriend());
		assertEquals("Ramnivas", myObject.getFriend().getName());
	}

	public void testConfigurableUsingAutowireByName() {
		ValidAutowireByName myObject = new ValidAutowireByName();
		assertNotNull(myObject.getRamnivas());
		assertEquals("Ramnivas", myObject.getRamnivas().getName());
	}

	public void testInvalidAutowireByName() {
		try {
			new InvalidAutowireByName();
			fail("Autowire by name cannot work");
		}
		catch (UnsatisfiedDependencyException ex) {
			// Ok
		}
	}

	public void testNewAspectAppliesToArbitraryNonAnnotatedPojo() {
		ArbitraryExistingPojo aep = new ArbitraryExistingPojo();
		assertNotNull(aep.friend);
		assertEquals("Ramnivas", aep.friend.getName());
	}

	public void testNewAspectThatWasNotAddedToSpringContainer() {
		try{
			new ClassThatWillNotActuallyBeWired();
		}
		catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().indexOf("BeanFactory") != -1);
		}
	}

	public void testInjectionOnDeserialization() throws Exception {
		ShouldBeConfiguredBySpring domainObject = new ShouldBeConfiguredBySpring();
		domainObject.setName("Anonymous");
		ShouldBeConfiguredBySpring deserializedDomainObject =
				serializeAndDeserialize(domainObject);
		assertEquals("Dependency injected on deserialization","Rod",deserializedDomainObject.getName());
	}

	public void testInjectionOnDeserializationForClassesThatContainsPublicReadResolve() throws Exception {
		ShouldBeConfiguredBySpringContainsPublicReadResolve domainObject = new ShouldBeConfiguredBySpringContainsPublicReadResolve();
		domainObject.setName("Anonymous");
		ShouldBeConfiguredBySpringContainsPublicReadResolve deserializedDomainObject =
				serializeAndDeserialize(domainObject);
		assertEquals("Dependency injected on deserialization","Rod",deserializedDomainObject.getName());
		assertEquals("User readResolve should take precedence", 1, deserializedDomainObject.readResolveInvocationCount);
	}

	// See ShouldBeConfiguredBySpringContainsPrivateReadResolve
//	public void testInjectionOnDeserializationForClassesThatContainsPrivateReadResolve() throws Exception {
//		ShouldBeConfiguredBySpringContainsPrivateReadResolve domainObject = new ShouldBeConfiguredBySpringContainsPrivateReadResolve();
//		domainObject.setName("Anonymous");
//		ShouldBeConfiguredBySpringContainsPrivateReadResolve deserializedDomainObject =
//				serializeAndDeserialize(domainObject);
//		assertEquals("Dependency injected on deserialization","Rod",deserializedDomainObject.getName());
//	}

	public void testNonInjectionOnDeserializationForSerializedButNotConfigured() throws Exception {
		SerializableThatShouldNotBeConfiguredBySpring domainObject = new SerializableThatShouldNotBeConfiguredBySpring();
		domainObject.setName("Anonymous");
		SerializableThatShouldNotBeConfiguredBySpring deserializedDomainObject =
				serializeAndDeserialize(domainObject);
		assertEquals("Dependency injected on deserialization","Anonymous",deserializedDomainObject.getName());
	}

	public void testSubBeanConfiguredOnlyOnce() throws Exception {
		SubBean subBean = new SubBean();
		assertEquals("Property injected more than once", 1, subBean.setterCount);
	}

	public void testSubBeanConfiguredOnlyOnceForPreConstruction() throws Exception {
		SubBeanPreConstruction subBean = new SubBeanPreConstruction();
		assertEquals("Property injected more than once", 1, subBean.setterCount);
	}

	public void testSubSerializableBeanConfiguredOnlyOnce() throws Exception {
		SubSerializableBean subBean = new SubSerializableBean();
		assertEquals("Property injected more than once", 1, subBean.setterCount);
		subBean.setterCount = 0;

		SubSerializableBean deserializedSubBean = serializeAndDeserialize(subBean);
		assertEquals("Property injected more than once", 1, deserializedSubBean.setterCount);
	}

	public void testPreConstructionConfiguredBean() {
		PreConstructionConfiguredBean bean = new PreConstructionConfiguredBean();
		assertTrue("Injection didn't occur before construction", bean.preConstructionConfigured);
	}

	public void testPreConstructionConfiguredBeanDeserializationReinjection() throws Exception {
		PreConstructionConfiguredBean bean = new PreConstructionConfiguredBean();
		PreConstructionConfiguredBean deserialized = serializeAndDeserialize(bean);
		assertEquals("Injection didn't occur upon deserialization", "ramnivas", deserialized.getName());
	}

	public void testPostConstructionConfiguredBean() {
		PostConstructionConfiguredBean bean = new PostConstructionConfiguredBean();
		assertFalse("Injection occurred before construction", bean.preConstructionConfigured);
	}

	public void testPostConstructionConfiguredBeanDeserializationReinjection() throws Exception {
		PostConstructionConfiguredBean bean = new PostConstructionConfiguredBean();
		PostConstructionConfiguredBean deserialized = serializeAndDeserialize(bean);
		assertEquals("Injection didn't occur upon deserialization", "ramnivas", deserialized.getName());
	}

	public void testInterfaceDrivenDependencyInjection() {
		MailClientDependencyInjectionAspect.aspectOf().setMailSender(new JavaMailSenderImpl());
		Order testOrder = new Order();
		assertNotNull("Interface driven injection didn't occur for direct construction", testOrder.mailSender);
	}

	public void testGenericInterfaceDrivenDependencyInjection() {
		PricingStrategy injectedPricingStrategy = new PricingStrategy();
		PricingStrategyDependencyInjectionAspect.aspectOf().setPricingStrategy(injectedPricingStrategy);
		LineItem testLineItem = new LineItem();
		assertSame("Generic interface driven injection didn't occur for direct construction", injectedPricingStrategy, testLineItem.pricingStrategy);
	}

	public void testInterfaceDrivenDependencyInjectionMultipleInterfaces() {
		MailClientDependencyInjectionAspect.aspectOf().setMailSender(new JavaMailSenderImpl());
		PaymentProcessorDependencyInjectionAspect.aspectOf().setPaymentProcessor(new PaymentProcessor());

		ShoppingCart testCart = new ShoppingCart();

		assertNotNull("Interface driven injection didn't occur for direct construction", testCart.mailSender);
		assertNotNull("Interface driven injection didn't occur for direct construction", testCart.paymentProcessor);
	}

	public void testInterfaceDrivenDependencyInjectionUponDeserialization() throws Exception {
		MailClientDependencyInjectionAspect.aspectOf().setMailSender(new JavaMailSenderImpl());
		Order testOrder = new Order();
		Order deserializedOrder = serializeAndDeserialize(testOrder);
		assertNotNull(deserializedOrder);
		assertNotNull("Interface driven injection didn't occur for deserialization", testOrder.mailSender);
	}

	public void testFieldAutoWiredAnnotationInjection() {
		FieldAutoWiredServiceBean bean = new FieldAutoWiredServiceBean();
		assertNotNull(bean.testService);
	}

	public void testMethodAutoWiredAnnotationInjection() {
		MethodAutoWiredServiceBean bean = new MethodAutoWiredServiceBean();
		assertNotNull(bean.testService);
	}

	public void testMultiArgumentMethodAutoWiredAnnotationInjection() {
		MultiArgumentMethodAutoWiredServiceBean bean = new MultiArgumentMethodAutoWiredServiceBean();
		assertNotNull(bean.testService);
		assertNotNull(bean.paymentService);
	}

	public void testGenericParameterConfigurableBean() {
		GenericParameterConfigurableBean bean = new GenericParameterConfigurableBean();
		assertNotNull(bean.testService);
	}

	@SuppressWarnings("unchecked")
	private <T> T serializeAndDeserialize(T serializable) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(serializable);
		oos.close();
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);
		return (T)ois.readObject();
	}


	@Configurable("beanOne")
	@SuppressWarnings("serial")
	protected static class ShouldBeConfiguredBySpring implements Serializable {

		private String name;

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}
	}


	@Configurable("beanOne")
	@SuppressWarnings("serial")
	private static class ShouldBeConfiguredBySpringContainsPublicReadResolve implements Serializable {

		private String name;

		private int readResolveInvocationCount = 0;

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public Object readResolve() throws ObjectStreamException {
			readResolveInvocationCount++;
			return this;
		}
	}

// 	Won't work until we use hasmethod() experimental pointcut in AspectJ.
//	@Configurable("beanOne")
//	private static class ShouldBeConfiguredBySpringContainsPrivateReadResolve implements Serializable {
//
//		private String name;
//
//		public void setName(String name) {
//			this.name = name;
//		}
//
//		public String getName() {
//			return this.name;
//		}
//
//		private Object readResolve() throws ObjectStreamException {
//			return this;
//		}
//	}

	@SuppressWarnings("unused")
	private static class ShouldNotBeConfiguredBySpring {

		private String name;

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}
	}


	@SuppressWarnings("serial")
	private static class SerializableThatShouldNotBeConfiguredBySpring implements Serializable {

		private String name;

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}
	}


	@Configurable
	@SuppressWarnings("unused")
	private static class ShouldBeConfiguredBySpringUsingTypeNameAsBeanName {

		private String name;

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}
	}


	@Configurable(autowire=Autowire.BY_TYPE)
	@SuppressWarnings("unused")
	private static class ShouldBeConfiguredBySpringUsingAutowireByType {

		private TestBean friend = null;

		public TestBean getFriend() {
			return friend;
		}

		public void setFriend(TestBean friend) {
			this.friend = friend;
		}
	}


	@Configurable(autowire=Autowire.BY_NAME)
	@SuppressWarnings("unused")
	private static class ValidAutowireByName {

		private TestBean friend = null;

		public TestBean getRamnivas() {
			return friend;
		}

		public void setRamnivas(TestBean friend) {
			this.friend = friend;
		}
	}


	@Configurable(autowire=Autowire.BY_NAME, dependencyCheck=true)
	@SuppressWarnings("unused")
	private static class InvalidAutowireByName {

		private TestBean friend;

		public TestBean getFriend() {
			return friend;
		}

		public void setFriend(TestBean friend) {
			this.friend = friend;
		}
	}

	@SuppressWarnings("unused")
	private static class ArbitraryExistingPojo {

		private TestBean friend;

		public void setFriend(TestBean f) {
			this.friend = f;
		}
	}


	public static class CircularFactoryBean implements FactoryBean{

		public CircularFactoryBean() {
			ValidAutowireByName autowired = new ValidAutowireByName();
			assertNull(autowired.getRamnivas());
		}

		public Object getObject() throws Exception {
			return new TestBean();
		}

		public Class getObjectType() {
			return TestBean.class;
		}

		public boolean isSingleton() {
			return false;
		}
	}


	@Configurable
	@SuppressWarnings("unused")
	private static class BaseBean {

		public int setterCount;

		private String name;

		public void setName(String name) {
			this.name = name;
			setterCount++;
		}
	}


	private static class SubBean extends BaseBean {
	}

	@Configurable(preConstruction=true)
	private static class SubBeanPreConstruction extends BaseBean {
	}

	@Configurable
	@SuppressWarnings({"serial", "unused"})
	private static class BaseSerializableBean implements Serializable {

		public int setterCount;

		private String name;

		public void setName(String name) {
			this.name = name;
			setterCount++;
		}
	}


	@SuppressWarnings("serial")
	private static class SubSerializableBean extends BaseSerializableBean {
	}


	@Aspect
	@SuppressWarnings("unused")
	private static class WireArbitraryExistingPojo extends AbstractBeanConfigurerAspect {

		@Pointcut("initialization(ArbitraryExistingPojo.new(..)) && this(beanInstance)")
		protected void beanCreation(Object beanInstance){

		}
	}


	@Aspect
	@SuppressWarnings("unused")
	private static class AspectThatWillNotBeUsed extends AbstractBeanConfigurerAspect {

		@Pointcut("initialization(ClassThatWillNotActuallyBeWired.new(..)) && this(beanInstance)")
		protected void beanCreation(Object beanInstance){
		}
	}

	private static aspect MailClientDependencyInjectionAspect extends AbstractInterfaceDrivenDependencyInjectionAspect {
		private MailSender mailSender;

		public pointcut inConfigurableBean() : within(MailSenderClient+);

		public void configureBean(Object bean) {
			((MailSenderClient)bean).setMailSender(this.mailSender);
		}

		declare parents: MailSenderClient implements ConfigurableObject;

		public void setMailSender(MailSender mailSender) {
			this.mailSender = mailSender;
		}
	}

	private static aspect PaymentProcessorDependencyInjectionAspect extends AbstractInterfaceDrivenDependencyInjectionAspect {
		private PaymentProcessor paymentProcessor;

		public pointcut inConfigurableBean() : within(PaymentProcessorClient+);

		public void configureBean(Object bean) {
			((PaymentProcessorClient)bean).setPaymentProcessor(this.paymentProcessor);
		}

		declare parents: PaymentProcessorClient implements ConfigurableObject;

		public void setPaymentProcessor(PaymentProcessor paymentProcessor) {
			this.paymentProcessor = paymentProcessor;
		}
	}

	private static aspect PricingStrategyDependencyInjectionAspect extends GenericInterfaceDrivenDependencyInjectionAspect<PricingStrategyClient> {
		private PricingStrategy pricingStrategy;

		public void configure(PricingStrategyClient bean) {
			bean.setPricingStrategy(pricingStrategy);
		}

		public void setPricingStrategy(PricingStrategy pricingStrategy) {
			this.pricingStrategy = pricingStrategy;
		}
	}

	public static interface MailSenderClient {
		public void setMailSender(MailSender mailSender);
	}

	public static interface PaymentProcessorClient {
		public void setPaymentProcessor(PaymentProcessor paymentProcessor);
	}

	public static class PaymentProcessor {

	}

	public static interface PricingStrategyClient {
		public void setPricingStrategy(PricingStrategy pricingStrategy);
	}

	public static class PricingStrategy {

	}

	public static class LineItem implements PricingStrategyClient {
		private PricingStrategy pricingStrategy;

		public void setPricingStrategy(PricingStrategy pricingStrategy) {
			this.pricingStrategy = pricingStrategy;
		}
	}

	@SuppressWarnings("serial")
	public static class Order implements MailSenderClient, Serializable {
		private transient MailSender mailSender;

		public void setMailSender(MailSender mailSender) {
			this.mailSender = mailSender;
		}
	}

	public static class ShoppingCart implements MailSenderClient, PaymentProcessorClient {
		private transient MailSender mailSender;
		private transient PaymentProcessor paymentProcessor;

		public void setMailSender(MailSender mailSender) {
			this.mailSender = mailSender;
		}

		public void setPaymentProcessor(PaymentProcessor paymentProcessor) {
			this.paymentProcessor = paymentProcessor;
		}
	}

	private static class ClassThatWillNotActuallyBeWired {

	}

	@Configurable
	@SuppressWarnings("serial")
	private static class PreOrPostConstructionConfiguredBean implements Serializable {
		private transient String name;
		protected transient boolean preConstructionConfigured;
		transient int count;

		public PreOrPostConstructionConfiguredBean() {
			preConstructionConfigured = (this.name != null);
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}
	}


	@Configurable(preConstruction=true)
	@SuppressWarnings("serial")
	public static class PreConstructionConfiguredBean extends PreOrPostConstructionConfiguredBean {
	}


	@Configurable(preConstruction=false)
	@SuppressWarnings("serial")
	private static class PostConstructionConfiguredBean extends PreOrPostConstructionConfiguredBean {
	}

	@Configurable
	public static class FieldAutoWiredServiceBean {
		@Autowired transient private TestService testService;
	}

	@Configurable
	public static class MethodAutoWiredServiceBean {
		transient private TestService testService;

		@Autowired
		public void setTestService(TestService testService) {
			this.testService = testService;
		}
	}

	@Configurable
	public static class MultiArgumentMethodAutoWiredServiceBean {
		transient private TestService testService;
		transient private PaymentService paymentService;

		@Autowired
		public void setDependencies(TestService testService, PaymentService paymentService) {
			this.testService = testService;
			this.paymentService = paymentService;
		}
	}

	@Configurable
	public static class GenericParameterConfigurableBean {
		private TestService testService;

		public void setTestService(TestService testService) {
			this.testService = testService;
		}
	}

	public static class TestService {

	}

	public static class PaymentService {

	}

}
