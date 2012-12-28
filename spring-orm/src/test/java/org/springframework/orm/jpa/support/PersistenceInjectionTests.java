/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.orm.jpa.support;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceProperty;
import javax.persistence.PersistenceUnit;

import org.easymock.MockControl;
import org.hibernate.ejb.HibernateEntityManager;
import org.junit.Ignore;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.SimpleMapScope;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.jndi.ExpectedLookupTemplate;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBeanTests;
import org.springframework.orm.jpa.DefaultJpaDialect;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.SerializationTestUtils;

/**
 * Unit tests for persistence context and persistence unit injection.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class PersistenceInjectionTests extends AbstractEntityManagerFactoryBeanTests {

	public void testPrivatePersistenceContextField() throws Exception {
		GenericApplicationContext gac = new GenericApplicationContext();
		gac.getDefaultListableBeanFactory().registerSingleton("entityManagerFactory", mockEmf);
		gac.registerBeanDefinition("annotationProcessor",
				new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class));
		gac.registerBeanDefinition(DefaultPrivatePersistenceContextField.class.getName(),
				new RootBeanDefinition(DefaultPrivatePersistenceContextField.class));
		gac.registerBeanDefinition(FactoryBeanWithPersistenceContextField.class.getName(),
				new RootBeanDefinition(FactoryBeanWithPersistenceContextField.class));
		gac.refresh();

		DefaultPrivatePersistenceContextField bean = (DefaultPrivatePersistenceContextField) gac.getBean(
				DefaultPrivatePersistenceContextField.class.getName());
		FactoryBeanWithPersistenceContextField bean2 = (FactoryBeanWithPersistenceContextField) gac.getBean(
				"&" + FactoryBeanWithPersistenceContextField.class.getName());
		assertNotNull(bean.em);
		assertNotNull(bean2.em);

		assertNotNull(SerializationTestUtils.serializeAndDeserialize(bean.em));
		assertNotNull(SerializationTestUtils.serializeAndDeserialize(bean2.em));
	}

	public void testPrivateVendorSpecificPersistenceContextField() {
		GenericApplicationContext gac = new GenericApplicationContext();
		gac.getDefaultListableBeanFactory().registerSingleton("entityManagerFactory", mockEmf);
		gac.registerBeanDefinition("annotationProcessor",
				new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class));
		gac.registerBeanDefinition(DefaultVendorSpecificPrivatePersistenceContextField.class.getName(),
				new RootBeanDefinition(DefaultVendorSpecificPrivatePersistenceContextField.class));
		gac.refresh();

		DefaultVendorSpecificPrivatePersistenceContextField bean = (DefaultVendorSpecificPrivatePersistenceContextField)
				gac.getBean(DefaultVendorSpecificPrivatePersistenceContextField.class.getName());
		assertNotNull(bean.em);
	}

	public void testPublicExtendedPersistenceContextSetter() throws Exception {
		EntityManager mockEm = MockControl.createControl(EntityManager.class).getMock();
		mockEmf.createEntityManager();
		emfMc.setReturnValue(mockEm, 1);
		emfMc.replay();

		GenericApplicationContext gac = new GenericApplicationContext();
		gac.getDefaultListableBeanFactory().registerSingleton("entityManagerFactory", mockEmf);
		gac.registerBeanDefinition("annotationProcessor",
				new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class));
		gac.registerBeanDefinition(DefaultPublicPersistenceContextSetter.class.getName(),
				new RootBeanDefinition(DefaultPublicPersistenceContextSetter.class));
		gac.refresh();

		DefaultPublicPersistenceContextSetter bean = (DefaultPublicPersistenceContextSetter) gac.getBean(
				DefaultPublicPersistenceContextSetter.class.getName());
		assertNotNull(bean.em);
		emfMc.verify();
	}

	public void testPublicSpecificExtendedPersistenceContextSetter() throws Exception {
		emfMc.replay();

		MockControl<EntityManagerFactory> emfMc2 = MockControl.createControl(EntityManagerFactory.class);
		EntityManagerFactory mockEmf2 = emfMc2.getMock();
		MockControl<EntityManager> emMc2 = MockControl.createControl(EntityManager.class);
		EntityManager mockEm2 = emMc2.getMock();
		mockEm2.getTransaction();
		emMc2.setReturnValue(null, 1);
		mockEm2.flush();
		emMc2.setVoidCallable(1);
		emMc2.replay();
		mockEmf2.createEntityManager();
		emfMc2.setReturnValue(mockEm2, 1);
		emfMc2.replay();

		GenericApplicationContext gac = new GenericApplicationContext();
		gac.getDefaultListableBeanFactory().registerSingleton("entityManagerFactory", mockEmf);
		gac.getDefaultListableBeanFactory().registerSingleton("unit2", mockEmf2);
		gac.registerBeanDefinition("annotationProcessor",
				new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class));
		gac.registerBeanDefinition(SpecificPublicPersistenceContextSetter.class.getName(),
				new RootBeanDefinition(SpecificPublicPersistenceContextSetter.class));
		gac.refresh();

		SpecificPublicPersistenceContextSetter bean = (SpecificPublicPersistenceContextSetter) gac.getBean(
				SpecificPublicPersistenceContextSetter.class.getName());
		assertNotNull(bean.getEntityManager());
		bean.getEntityManager().flush();
		emfMc.verify();
		emfMc2.verify();
	}

	public void testPublicExtendedPersistenceContextSetterWithSerialization() throws Exception {
		DummyInvocationHandler ih = new DummyInvocationHandler();
		Object mockEm = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {EntityManager.class}, ih);
		mockEmf.createEntityManager();
		emfMc.setReturnValue(mockEm, 1);
		emfMc.replay();

		GenericApplicationContext gac = new GenericApplicationContext();
		SimpleMapScope myScope = new SimpleMapScope();
		gac.getDefaultListableBeanFactory().registerScope("myScope", myScope);
		gac.getDefaultListableBeanFactory().registerSingleton("entityManagerFactory", mockEmf);
		gac.registerBeanDefinition("annotationProcessor",
				new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class));
		RootBeanDefinition bd = new RootBeanDefinition(DefaultPublicPersistenceContextSetter.class);
		bd.setScope("myScope");
		gac.registerBeanDefinition(DefaultPublicPersistenceContextSetter.class.getName(), bd);
		gac.refresh();

		DefaultPublicPersistenceContextSetter bean = (DefaultPublicPersistenceContextSetter) gac.getBean(
				DefaultPublicPersistenceContextSetter.class.getName());
		assertNotNull(bean.em);
		assertNotNull(SerializationTestUtils.serializeAndDeserialize(bean.em));

		SimpleMapScope serialized = (SimpleMapScope) SerializationTestUtils.serializeAndDeserialize(myScope);
		serialized.close();
		assertTrue(DummyInvocationHandler.closed);
		DummyInvocationHandler.closed = false;
		emfMc.verify();
	}

	public void testPublicExtendedPersistenceContextSetterWithEntityManagerInfoAndSerialization() throws Exception {
		Object mockEm = Proxy.newProxyInstance(
				getClass().getClassLoader(), new Class[] {EntityManager.class}, new DummyInvocationHandler());
		MockControl emfMc = MockControl.createControl(EntityManagerFactoryWithInfo.class);
		EntityManagerFactoryWithInfo mockEmf = (EntityManagerFactoryWithInfo) emfMc.getMock();
		mockEmf.getNativeEntityManagerFactory();
		emfMc.setReturnValue(mockEmf);
		mockEmf.getPersistenceUnitInfo();
		emfMc.setReturnValue(null);
		mockEmf.getJpaDialect();
		emfMc.setReturnValue(new DefaultJpaDialect());
		mockEmf.getEntityManagerInterface();
		emfMc.setReturnValue(EntityManager.class);
		mockEmf.getBeanClassLoader();
		emfMc.setReturnValue(getClass().getClassLoader());
		mockEmf.createEntityManager();
		emfMc.setReturnValue(mockEm);
		emfMc.replay();

		GenericApplicationContext gac = new GenericApplicationContext();
		gac.getDefaultListableBeanFactory().registerSingleton("entityManagerFactory", mockEmf);
		gac.registerBeanDefinition("annotationProcessor",
				new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class));
		gac.registerBeanDefinition(DefaultPublicPersistenceContextSetter.class.getName(),
				new RootBeanDefinition(DefaultPublicPersistenceContextSetter.class));
		gac.refresh();

		DefaultPublicPersistenceContextSetter bean = (DefaultPublicPersistenceContextSetter) gac.getBean(
				DefaultPublicPersistenceContextSetter.class.getName());
		assertNotNull(bean.em);
		assertNotNull(SerializationTestUtils.serializeAndDeserialize(bean.em));
		emfMc.verify();
	}

	public void testPublicExtendedPersistenceContextSetterWithOverriding() {
		EntityManager mockEm2 = MockControl.createControl(EntityManager.class).getMock();

		GenericApplicationContext gac = new GenericApplicationContext();
		gac.getDefaultListableBeanFactory().registerSingleton("entityManagerFactory", mockEmf);
		gac.registerBeanDefinition("annotationProcessor",
				new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class));
		RootBeanDefinition bd = new RootBeanDefinition(DefaultPublicPersistenceContextSetter.class);
		bd.getPropertyValues().add("entityManager", mockEm2);
		gac.registerBeanDefinition(DefaultPublicPersistenceContextSetter.class.getName(), bd);
		gac.refresh();

		DefaultPublicPersistenceContextSetter bean = (DefaultPublicPersistenceContextSetter) gac.getBean(
				DefaultPublicPersistenceContextSetter.class.getName());
		assertSame(mockEm2, bean.em);
	}

	public void testPrivatePersistenceUnitField() {
		GenericApplicationContext gac = new GenericApplicationContext();
		gac.getDefaultListableBeanFactory().registerSingleton("entityManagerFactory", mockEmf);
		gac.registerBeanDefinition("annotationProcessor",
				new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class));
		gac.registerBeanDefinition(DefaultPrivatePersistenceUnitField.class.getName(),
				new RootBeanDefinition(DefaultPrivatePersistenceUnitField.class));
		gac.refresh();

		DefaultPrivatePersistenceUnitField bean = (DefaultPrivatePersistenceUnitField) gac.getBean(
				DefaultPrivatePersistenceUnitField.class.getName());
		assertSame(mockEmf, bean.emf);
	}

	public void testPublicPersistenceUnitSetter() {
		GenericApplicationContext gac = new GenericApplicationContext();
		gac.getDefaultListableBeanFactory().registerSingleton("entityManagerFactory", mockEmf);
		gac.registerBeanDefinition("annotationProcessor",
				new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class));
		gac.registerBeanDefinition(DefaultPublicPersistenceUnitSetter.class.getName(),
				new RootBeanDefinition(DefaultPublicPersistenceUnitSetter.class));
		gac.refresh();

		DefaultPublicPersistenceUnitSetter bean = (DefaultPublicPersistenceUnitSetter) gac.getBean(
				DefaultPublicPersistenceUnitSetter.class.getName());
		assertSame(mockEmf, bean.emf);
	}

	public void testPublicPersistenceUnitSetterWithOverriding() {
		EntityManagerFactory mockEmf2 =
				MockControl.createControl(EntityManagerFactory.class).getMock();

		GenericApplicationContext gac = new GenericApplicationContext();
		gac.getDefaultListableBeanFactory().registerSingleton("entityManagerFactory", mockEmf);
		gac.registerBeanDefinition("annotationProcessor",
				new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class));
		RootBeanDefinition bd = new RootBeanDefinition(DefaultPublicPersistenceUnitSetter.class);
		bd.getPropertyValues().add("emf", mockEmf2);
		gac.registerBeanDefinition(DefaultPublicPersistenceUnitSetter.class.getName(), bd);
		gac.refresh();

		DefaultPublicPersistenceUnitSetter bean = (DefaultPublicPersistenceUnitSetter) gac.getBean(
				DefaultPublicPersistenceUnitSetter.class.getName());
		assertSame(mockEmf2, bean.emf);
	}

	public void testPublicPersistenceUnitSetterWithUnitIdentifiedThroughBeanName() {
		EntityManagerFactory mockEmf2 =
				MockControl.createControl(EntityManagerFactory.class).getMock();

		GenericApplicationContext gac = new GenericApplicationContext();
		gac.getDefaultListableBeanFactory().registerSingleton("entityManagerFactory", mockEmf);
		gac.getDefaultListableBeanFactory().registerSingleton("entityManagerFactory2", mockEmf2);
		gac.registerAlias("entityManagerFactory2", "Person");
		RootBeanDefinition processorDef = new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class);
		processorDef.getPropertyValues().add("defaultPersistenceUnitName", "entityManagerFactory");
		gac.registerBeanDefinition("annotationProcessor", processorDef);
		gac.registerBeanDefinition(DefaultPublicPersistenceUnitSetter.class.getName(),
				new RootBeanDefinition(DefaultPublicPersistenceUnitSetter.class));
		gac.registerBeanDefinition(DefaultPublicPersistenceUnitSetterNamedPerson.class.getName(),
				new RootBeanDefinition(DefaultPublicPersistenceUnitSetterNamedPerson.class));
		gac.refresh();

		DefaultPublicPersistenceUnitSetter bean = (DefaultPublicPersistenceUnitSetter)
				gac.getBean(DefaultPublicPersistenceUnitSetter.class.getName());
		DefaultPublicPersistenceUnitSetterNamedPerson bean2 = (DefaultPublicPersistenceUnitSetterNamedPerson)
				gac.getBean(DefaultPublicPersistenceUnitSetterNamedPerson.class.getName());
		assertSame(mockEmf, bean.emf);
		assertSame(mockEmf2, bean2.emf);
	}

	public void testPublicPersistenceUnitSetterWithMultipleUnitsIdentifiedThroughUnitName() {
		MockControl emf2Mc = MockControl.createControl(EntityManagerFactoryWithInfo.class);
		EntityManagerFactoryWithInfo mockEmf2 = (EntityManagerFactoryWithInfo) emf2Mc.getMock();
		mockEmf2.getPersistenceUnitName();
		emf2Mc.setReturnValue("Person", 2);
		emf2Mc.replay();

		GenericApplicationContext gac = new GenericApplicationContext();
		gac.getDefaultListableBeanFactory().registerSingleton("entityManagerFactory", mockEmf);
		gac.getDefaultListableBeanFactory().registerSingleton("entityManagerFactory2", mockEmf2);
		RootBeanDefinition processorDef = new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class);
		processorDef.getPropertyValues().add("defaultPersistenceUnitName", "entityManagerFactory");
		gac.registerBeanDefinition("annotationProcessor", processorDef);
		gac.registerBeanDefinition(DefaultPublicPersistenceUnitSetter.class.getName(),
				new RootBeanDefinition(DefaultPublicPersistenceUnitSetter.class));
		gac.registerBeanDefinition(DefaultPublicPersistenceUnitSetterNamedPerson.class.getName(),
				new RootBeanDefinition(DefaultPublicPersistenceUnitSetterNamedPerson.class));
		gac.refresh();

		DefaultPublicPersistenceUnitSetter bean = (DefaultPublicPersistenceUnitSetter)
				gac.getBean(DefaultPublicPersistenceUnitSetter.class.getName());
		DefaultPublicPersistenceUnitSetterNamedPerson bean2 = (DefaultPublicPersistenceUnitSetterNamedPerson)
				gac.getBean(DefaultPublicPersistenceUnitSetterNamedPerson.class.getName());
		assertSame(mockEmf, bean.emf);
		assertSame(mockEmf2, bean2.emf);

		emf2Mc.verify();
	}

	@Ignore
	public void ignoreTestPersistenceUnitsFromJndi() {
		mockEmf.createEntityManager();
		Object mockEm = MockControl.createControl(EntityManager.class).getMock();
		emfMc.setReturnValue(mockEm, 1);
		emfMc.replay();

		MockControl emf2Mc = MockControl.createControl(EntityManagerFactoryWithInfo.class);
		EntityManagerFactoryWithInfo mockEmf2 = (EntityManagerFactoryWithInfo) emf2Mc.getMock();

		Map<String, String> persistenceUnits = new HashMap<String, String>();
		persistenceUnits.put("", "pu1");
		persistenceUnits.put("Person", "pu2");
		ExpectedLookupTemplate jt = new ExpectedLookupTemplate();
		jt.addObject("java:comp/env/pu1", mockEmf);
		jt.addObject("java:comp/env/pu2", mockEmf2);

		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		PersistenceAnnotationBeanPostProcessor bpp = new PersistenceAnnotationBeanPostProcessor();
		bpp.setPersistenceUnits(persistenceUnits);
		bpp.setJndiTemplate(jt);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition(DefaultPublicPersistenceUnitSetter.class.getName(),
				new RootBeanDefinition(DefaultPublicPersistenceUnitSetter.class));
		bf.registerBeanDefinition(DefaultPublicPersistenceUnitSetterNamedPerson.class.getName(),
				new RootBeanDefinition(DefaultPublicPersistenceUnitSetterNamedPerson.class));
		bf.registerBeanDefinition(DefaultPrivatePersistenceContextField.class.getName(),
				new RootBeanDefinition(DefaultPrivatePersistenceContextField.class));
		bf.registerBeanDefinition(DefaultPublicPersistenceContextSetter.class.getName(),
				new RootBeanDefinition(DefaultPublicPersistenceContextSetter.class));

		DefaultPublicPersistenceUnitSetter bean = (DefaultPublicPersistenceUnitSetter)
				bf.getBean(DefaultPublicPersistenceUnitSetter.class.getName());
		DefaultPublicPersistenceUnitSetterNamedPerson bean2 = (DefaultPublicPersistenceUnitSetterNamedPerson)
				bf.getBean(DefaultPublicPersistenceUnitSetterNamedPerson.class.getName());
		DefaultPrivatePersistenceContextField bean3 = (DefaultPrivatePersistenceContextField)
				bf.getBean(DefaultPrivatePersistenceContextField.class.getName());
		DefaultPublicPersistenceContextSetter bean4 = (DefaultPublicPersistenceContextSetter)
				bf.getBean(DefaultPublicPersistenceContextSetter.class.getName());
		assertSame(mockEmf, bean.emf);
		assertSame(mockEmf2, bean2.emf);
		assertNotNull(bean3.em);
		assertNotNull(bean4.em);

		emfMc.verify();
	}

	public void testPersistenceUnitsFromJndiWithDefaultUnit() {
		MockControl emf2Mc = MockControl.createControl(EntityManagerFactoryWithInfo.class);
		EntityManagerFactoryWithInfo mockEmf2 = (EntityManagerFactoryWithInfo) emf2Mc.getMock();

		Map<String, String> persistenceUnits = new HashMap<String, String>();
		persistenceUnits.put("System", "pu1");
		persistenceUnits.put("Person", "pu2");
		ExpectedLookupTemplate jt = new ExpectedLookupTemplate();
		jt.addObject("java:comp/env/pu1", mockEmf);
		jt.addObject("java:comp/env/pu2", mockEmf2);

		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		PersistenceAnnotationBeanPostProcessor bpp = new PersistenceAnnotationBeanPostProcessor();
		bpp.setPersistenceUnits(persistenceUnits);
		bpp.setDefaultPersistenceUnitName("System");
		bpp.setJndiTemplate(jt);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition(DefaultPublicPersistenceUnitSetter.class.getName(),
				new RootBeanDefinition(DefaultPublicPersistenceUnitSetter.class));
		bf.registerBeanDefinition(DefaultPublicPersistenceUnitSetterNamedPerson.class.getName(),
				new RootBeanDefinition(DefaultPublicPersistenceUnitSetterNamedPerson.class));

		DefaultPublicPersistenceUnitSetter bean = (DefaultPublicPersistenceUnitSetter)
				bf.getBean(DefaultPublicPersistenceUnitSetter.class.getName());
		DefaultPublicPersistenceUnitSetterNamedPerson bean2 = (DefaultPublicPersistenceUnitSetterNamedPerson)
				bf.getBean(DefaultPublicPersistenceUnitSetterNamedPerson.class.getName());
		assertSame(mockEmf, bean.emf);
		assertSame(mockEmf2, bean2.emf);
	}

	public void testSinglePersistenceUnitFromJndi() {
		Map<String, String> persistenceUnits = new HashMap<String, String>();
		persistenceUnits.put("Person", "pu1");
		ExpectedLookupTemplate jt = new ExpectedLookupTemplate();
		jt.addObject("java:comp/env/pu1", mockEmf);

		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		PersistenceAnnotationBeanPostProcessor bpp = new PersistenceAnnotationBeanPostProcessor();
		bpp.setPersistenceUnits(persistenceUnits);
		bpp.setJndiTemplate(jt);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition(DefaultPublicPersistenceUnitSetter.class.getName(),
				new RootBeanDefinition(DefaultPublicPersistenceUnitSetter.class));
		bf.registerBeanDefinition(DefaultPublicPersistenceUnitSetterNamedPerson.class.getName(),
				new RootBeanDefinition(DefaultPublicPersistenceUnitSetterNamedPerson.class));

		DefaultPublicPersistenceUnitSetter bean = (DefaultPublicPersistenceUnitSetter)
				bf.getBean(DefaultPublicPersistenceUnitSetter.class.getName());
		DefaultPublicPersistenceUnitSetterNamedPerson bean2 = (DefaultPublicPersistenceUnitSetterNamedPerson)
				bf.getBean(DefaultPublicPersistenceUnitSetterNamedPerson.class.getName());
		assertSame(mockEmf, bean.emf);
		assertSame(mockEmf, bean2.emf);
	}

	public void testPersistenceContextsFromJndi() {
		Object mockEm = MockControl.createControl(EntityManager.class).getMock();
		Object mockEm2 = MockControl.createControl(EntityManager.class).getMock();
		Object mockEm3 = MockControl.createControl(EntityManager.class).getMock();

		Map<String, String> persistenceContexts = new HashMap<String, String>();
		persistenceContexts.put("", "pc1");
		persistenceContexts.put("Person", "pc2");
		Map<String, String> extendedPersistenceContexts = new HashMap<String, String>();
		extendedPersistenceContexts .put("", "pc3");
		ExpectedLookupTemplate jt = new ExpectedLookupTemplate();
		jt.addObject("java:comp/env/pc1", mockEm);
		jt.addObject("java:comp/env/pc2", mockEm2);
		jt.addObject("java:comp/env/pc3", mockEm3);

		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		PersistenceAnnotationBeanPostProcessor bpp = new PersistenceAnnotationBeanPostProcessor();
		bpp.setPersistenceContexts(persistenceContexts);
		bpp.setExtendedPersistenceContexts(extendedPersistenceContexts);
		bpp.setJndiTemplate(jt);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition(DefaultPrivatePersistenceContextField.class.getName(),
				new RootBeanDefinition(DefaultPrivatePersistenceContextField.class));
		bf.registerBeanDefinition(DefaultPrivatePersistenceContextFieldNamedPerson.class.getName(),
				new RootBeanDefinition(DefaultPrivatePersistenceContextFieldNamedPerson.class));
		bf.registerBeanDefinition(DefaultPublicPersistenceContextSetter.class.getName(),
				new RootBeanDefinition(DefaultPublicPersistenceContextSetter.class));

		DefaultPrivatePersistenceContextField bean1 = (DefaultPrivatePersistenceContextField)
				bf.getBean(DefaultPrivatePersistenceContextField.class.getName());
		DefaultPrivatePersistenceContextFieldNamedPerson bean2 = (DefaultPrivatePersistenceContextFieldNamedPerson)
				bf.getBean(DefaultPrivatePersistenceContextFieldNamedPerson.class.getName());
		DefaultPublicPersistenceContextSetter bean3 = (DefaultPublicPersistenceContextSetter)
				bf.getBean(DefaultPublicPersistenceContextSetter.class.getName());
		assertSame(mockEm, bean1.em);
		assertSame(mockEm2, bean2.em);
		assertSame(mockEm3, bean3.em);
	}

	public void testPersistenceContextsFromJndiWithDefaultUnit() {
		Object mockEm = MockControl.createControl(EntityManager.class).getMock();
		Object mockEm2 = MockControl.createControl(EntityManager.class).getMock();
		Object mockEm3 = MockControl.createControl(EntityManager.class).getMock();

		Map<String, String> persistenceContexts = new HashMap<String, String>();
		persistenceContexts.put("System", "pc1");
		persistenceContexts.put("Person", "pc2");
		Map<String, String> extendedPersistenceContexts = new HashMap<String, String>();
		extendedPersistenceContexts .put("System", "pc3");
		ExpectedLookupTemplate jt = new ExpectedLookupTemplate();
		jt.addObject("java:comp/env/pc1", mockEm);
		jt.addObject("java:comp/env/pc2", mockEm2);
		jt.addObject("java:comp/env/pc3", mockEm3);

		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		PersistenceAnnotationBeanPostProcessor bpp = new PersistenceAnnotationBeanPostProcessor();
		bpp.setPersistenceContexts(persistenceContexts);
		bpp.setExtendedPersistenceContexts(extendedPersistenceContexts);
		bpp.setDefaultPersistenceUnitName("System");
		bpp.setJndiTemplate(jt);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition(DefaultPrivatePersistenceContextField.class.getName(),
				new RootBeanDefinition(DefaultPrivatePersistenceContextField.class));
		bf.registerBeanDefinition(DefaultPrivatePersistenceContextFieldNamedPerson.class.getName(),
				new RootBeanDefinition(DefaultPrivatePersistenceContextFieldNamedPerson.class));
		bf.registerBeanDefinition(DefaultPublicPersistenceContextSetter.class.getName(),
				new RootBeanDefinition(DefaultPublicPersistenceContextSetter.class));

		DefaultPrivatePersistenceContextField bean1 = (DefaultPrivatePersistenceContextField)
				bf.getBean(DefaultPrivatePersistenceContextField.class.getName());
		DefaultPrivatePersistenceContextFieldNamedPerson bean2 = (DefaultPrivatePersistenceContextFieldNamedPerson)
				bf.getBean(DefaultPrivatePersistenceContextFieldNamedPerson.class.getName());
		DefaultPublicPersistenceContextSetter bean3 = (DefaultPublicPersistenceContextSetter)
				bf.getBean(DefaultPublicPersistenceContextSetter.class.getName());
		assertSame(mockEm, bean1.em);
		assertSame(mockEm2, bean2.em);
		assertSame(mockEm3, bean3.em);
	}

	public void testSinglePersistenceContextFromJndi() {
		Object mockEm = MockControl.createControl(EntityManager.class).getMock();
		Object mockEm2 = MockControl.createControl(EntityManager.class).getMock();

		Map<String, String> persistenceContexts = new HashMap<String, String>();
		persistenceContexts.put("System", "pc1");
		Map<String, String> extendedPersistenceContexts = new HashMap<String, String>();
		extendedPersistenceContexts .put("System", "pc2");
		ExpectedLookupTemplate jt = new ExpectedLookupTemplate();
		jt.addObject("java:comp/env/pc1", mockEm);
		jt.addObject("java:comp/env/pc2", mockEm2);

		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		PersistenceAnnotationBeanPostProcessor bpp = new PersistenceAnnotationBeanPostProcessor();
		bpp.setPersistenceContexts(persistenceContexts);
		bpp.setExtendedPersistenceContexts(extendedPersistenceContexts);
		bpp.setJndiTemplate(jt);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition(DefaultPrivatePersistenceContextField.class.getName(),
				new RootBeanDefinition(DefaultPrivatePersistenceContextField.class));
		bf.registerBeanDefinition(DefaultPublicPersistenceContextSetter.class.getName(),
				new RootBeanDefinition(DefaultPublicPersistenceContextSetter.class));

		DefaultPrivatePersistenceContextField bean1 = (DefaultPrivatePersistenceContextField)
				bf.getBean(DefaultPrivatePersistenceContextField.class.getName());
		DefaultPublicPersistenceContextSetter bean2 = (DefaultPublicPersistenceContextSetter)
				bf.getBean(DefaultPublicPersistenceContextSetter.class.getName());
		assertSame(mockEm, bean1.em);
		assertSame(mockEm2, bean2.em);
	}

	public void testFieldOfWrongTypeAnnotatedWithPersistenceUnit() {
		PersistenceAnnotationBeanPostProcessor babpp = new PersistenceAnnotationBeanPostProcessor();
		try {
			babpp.postProcessPropertyValues(null, null, new FieldOfWrongTypeAnnotatedWithPersistenceUnit(), null);
			fail("Can't inject this field");
		}
		catch (IllegalStateException ex) {
			// Ok
		}
	}

	public void testSetterOfWrongTypeAnnotatedWithPersistenceUnit() {
		PersistenceAnnotationBeanPostProcessor babpp = new PersistenceAnnotationBeanPostProcessor();
		try {
			babpp.postProcessPropertyValues(null, null, new SetterOfWrongTypeAnnotatedWithPersistenceUnit(), null);
			fail("Can't inject this setter");
		}
		catch (IllegalStateException ex) {
			// Ok
		}
	}

	public void testSetterWithNoArgs() {
		PersistenceAnnotationBeanPostProcessor babpp = new PersistenceAnnotationBeanPostProcessor();
		try {
			babpp.postProcessPropertyValues(null, null, new SetterWithNoArgs(), null);
			fail("Can't inject this setter");
		}
		catch (IllegalStateException ex) {
			// Ok
		}
	}

	@Ignore
	public void ignoreTestNoPropertiesPassedIn() {
		mockEmf.createEntityManager();
		emfMc.setReturnValue(MockControl.createControl(EntityManager.class).getMock(), 1);
		emfMc.replay();

		PersistenceAnnotationBeanPostProcessor babpp = new MockPersistenceAnnotationBeanPostProcessor();
		DefaultPrivatePersistenceContextFieldExtended dppcf = new DefaultPrivatePersistenceContextFieldExtended();
		babpp.postProcessAfterInstantiation(dppcf, "bean name does not matter");
		assertNotNull(dppcf.em);
		emfMc.verify();
	}

	@Ignore
	public void ignoreTestPropertiesPassedIn() {
		Properties props = new Properties();
		props.put("foo", "bar");
		mockEmf.createEntityManager(props);
		emfMc.setReturnValue(MockControl.createControl(EntityManager.class).getMock(), 1);
		emfMc.replay();

		PersistenceAnnotationBeanPostProcessor babpp = new MockPersistenceAnnotationBeanPostProcessor();
		DefaultPrivatePersistenceContextFieldExtendedWithProps dppcf =
				new DefaultPrivatePersistenceContextFieldExtendedWithProps();
		babpp.postProcessAfterInstantiation(dppcf, "bean name does not matter");
		assertNotNull(dppcf.em);
		emfMc.verify();
	}

	public void testPropertiesForTransactionalEntityManager() {
		Properties props = new Properties();
		props.put("foo", "bar");
		MockControl emC = MockControl.createControl(EntityManager.class);
		EntityManager em = (EntityManager) emC.getMock();
		emfMc.expectAndReturn(mockEmf.createEntityManager(props), em);
		emC.expectAndReturn(em.getDelegate(), new Object());
		emC.expectAndReturn(em.isOpen(), true);
		em.close();

		emfMc.replay();
		emC.replay();

		PersistenceAnnotationBeanPostProcessor babpp = new MockPersistenceAnnotationBeanPostProcessor();
		DefaultPrivatePersistenceContextFieldWithProperties transactionalField =
				new DefaultPrivatePersistenceContextFieldWithProperties();
		babpp.postProcessPropertyValues(null, null, transactionalField, null);

		assertNotNull(transactionalField.em);
		assertNotNull(transactionalField.em.getDelegate());

		emfMc.verify();
		emC.verify();
	}

	/**
	 * Binds an EMF to the thread and tests if EM with different properties
	 * generate new EMs or not.
	 */
	public void testPropertiesForSharedEntityManager1() {
		Properties props = new Properties();
		props.put("foo", "bar");
		MockControl emC = MockControl.createControl(EntityManager.class);
		EntityManager em = (EntityManager) emC.getMock();
		// only one call made  - the first EM definition wins (in this case the one w/ the properties)
		emfMc.expectAndReturn(mockEmf.createEntityManager(props), em);
		emC.expectAndReturn(em.getDelegate(), new Object(), 2);
		emC.expectAndReturn(em.isOpen(), true);
		em.close();

		emfMc.replay();
		emC.replay();

		PersistenceAnnotationBeanPostProcessor babpp = new MockPersistenceAnnotationBeanPostProcessor();
		DefaultPrivatePersistenceContextFieldWithProperties transactionalFieldWithProperties =
				new DefaultPrivatePersistenceContextFieldWithProperties();
		DefaultPrivatePersistenceContextField transactionalField = new DefaultPrivatePersistenceContextField();

		babpp.postProcessPropertyValues(null, null, transactionalFieldWithProperties, null);
		babpp.postProcessPropertyValues(null, null, transactionalField, null);

		assertNotNull(transactionalFieldWithProperties.em);
		assertNotNull(transactionalField.em);
		// the EM w/ properties will be created
		assertNotNull(transactionalFieldWithProperties.em.getDelegate());
		// bind em to the thread now since it's created
		try {
			TransactionSynchronizationManager.bindResource(mockEmf, new EntityManagerHolder(em));
			assertNotNull(transactionalField.em.getDelegate());
			emfMc.verify();
			emC.verify();
		}
		finally {
			TransactionSynchronizationManager.unbindResource(mockEmf);
		}
	}

	public void testPropertiesForSharedEntityManager2() {
		Properties props = new Properties();
		props.put("foo", "bar");
		MockControl emC = MockControl.createControl(EntityManager.class);
		EntityManager em = (EntityManager) emC.getMock();
		// only one call made  - the first EM definition wins (in this case the one w/o the properties)
		emfMc.expectAndReturn(mockEmf.createEntityManager(), em);
		emC.expectAndReturn(em.getDelegate(), new Object(), 2);
		emC.expectAndReturn(em.isOpen(), true);
		em.close();

		emfMc.replay();
		emC.replay();

		PersistenceAnnotationBeanPostProcessor babpp = new MockPersistenceAnnotationBeanPostProcessor();
		DefaultPrivatePersistenceContextFieldWithProperties transactionalFieldWithProperties =
				new DefaultPrivatePersistenceContextFieldWithProperties();
		DefaultPrivatePersistenceContextField transactionalField = new DefaultPrivatePersistenceContextField();

		babpp.postProcessPropertyValues(null, null, transactionalFieldWithProperties, null);
		babpp.postProcessPropertyValues(null, null, transactionalField, null);

		assertNotNull(transactionalFieldWithProperties.em);
		assertNotNull(transactionalField.em);
		// the EM w/o properties will be created
		assertNotNull(transactionalField.em.getDelegate());
		// bind em to the thread now since it's created
		try {
			TransactionSynchronizationManager.bindResource(mockEmf, new EntityManagerHolder(em));
			assertNotNull(transactionalFieldWithProperties.em.getDelegate());
			emfMc.verify();
			emC.verify();
		}
		finally {
			TransactionSynchronizationManager.unbindResource(mockEmf);
		}
	}


	@SuppressWarnings("serial")
	private static class MockPersistenceAnnotationBeanPostProcessor extends PersistenceAnnotationBeanPostProcessor {

		@Override
		protected EntityManagerFactory findEntityManagerFactory(String emfName, String requestingBeanName) {
			return mockEmf;
		}
	}


	public static class DefaultPrivatePersistenceContextField {

		@PersistenceContext
		private EntityManager em;
	}


	public static class DefaultVendorSpecificPrivatePersistenceContextField {

		@PersistenceContext
		private HibernateEntityManager em;
	}


	public static class FactoryBeanWithPersistenceContextField implements FactoryBean {

		@PersistenceContext
		private EntityManager em;

		@Override
		public Object getObject() throws Exception {
			return null;
		}

		@Override
		public Class getObjectType() {
			return null;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}
	}


	public static class DefaultPrivatePersistenceContextFieldNamedPerson {

		@PersistenceContext(unitName = "Person")
		private EntityManager em;
	}


	public static class DefaultPrivatePersistenceContextFieldWithProperties {

		@PersistenceContext(properties = { @PersistenceProperty(name = "foo", value = "bar") })
		private EntityManager em;
	}


	@Repository
	@SuppressWarnings("serial")
	public static class DefaultPublicPersistenceContextSetter implements Serializable {

		private EntityManager em;

		@PersistenceContext(type = PersistenceContextType.EXTENDED)
		public void setEntityManager(EntityManager em) {
			if (this.em != null) {
				throw new IllegalStateException("Already called");
			}
			this.em = em;
		}

		public EntityManager getEntityManager() {
			return em;
		}
	}


	@SuppressWarnings("serial")
	public static class SpecificPublicPersistenceContextSetter extends DefaultPublicPersistenceContextSetter {

		@Override
		@PersistenceContext(unitName="unit2", type = PersistenceContextType.EXTENDED)
		public void setEntityManager(EntityManager em) {
			super.setEntityManager(em);
		}
	}


	public static class DefaultPrivatePersistenceUnitField {

		@PersistenceUnit
		private EntityManagerFactory emf;
	}


	public static class DefaultPublicPersistenceUnitSetter {

		private EntityManagerFactory emf;

		@PersistenceUnit
		public void setEmf(EntityManagerFactory emf) {
			if (this.emf != null) {
				throw new IllegalStateException("Already called");
			}
			this.emf = emf;
		}

		public EntityManagerFactory getEmf() {
			return emf;
		}
	}


	@Repository
	public static class DefaultPublicPersistenceUnitSetterNamedPerson {

		private EntityManagerFactory emf;

		@PersistenceUnit(unitName = "Person")
		public void setEmf(EntityManagerFactory emf) {
			this.emf = emf;
		}

		public EntityManagerFactory getEntityManagerFactory() {
			return emf;
		}
	}


	public static class FieldOfWrongTypeAnnotatedWithPersistenceUnit {

		@PersistenceUnit
		public String thisFieldIsOfTheWrongType;
	}


	public static class SetterOfWrongTypeAnnotatedWithPersistenceUnit {

		@PersistenceUnit
		public void setSomething(Comparable c) {
		}
	}


	public static class SetterWithNoArgs {

		@PersistenceUnit
		public void setSomething() {
		}
	}


	public static class DefaultPrivatePersistenceContextFieldExtended {

		@PersistenceContext(type = PersistenceContextType.EXTENDED)
		private EntityManager em;
	}


	public static class DefaultPrivatePersistenceContextFieldExtendedWithProps {

		@PersistenceContext(type = PersistenceContextType.EXTENDED, properties = { @PersistenceProperty(name = "foo", value = "bar") })
		private EntityManager em;
	}


	private interface EntityManagerFactoryWithInfo extends EntityManagerFactory, EntityManagerFactoryInfo {

	}


	@SuppressWarnings("serial")
	private static class DummyInvocationHandler implements InvocationHandler, Serializable {

		public static boolean closed;

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if ("isOpen".equals(method.getName())) {
				return true;
			}
			if ("close".equals(method.getName())) {
				closed = true;
				return null;
			}
			if ("toString".equals(method.getName())) {
				return "";
			}
			throw new IllegalStateException();
		}
	}

}
