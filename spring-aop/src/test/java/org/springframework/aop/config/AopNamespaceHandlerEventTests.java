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

package org.springframework.aop.config;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.ComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.Resource;
import org.springframework.tests.beans.CollectingReaderEventListener;

import static org.junit.Assert.*;
import static org.springframework.tests.TestResourceUtils.*;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public final class AopNamespaceHandlerEventTests {

	private static final Class<?> CLASS = AopNamespaceHandlerEventTests.class;

	private static final Resource CONTEXT =  qualifiedResource(CLASS, "context.xml");
	private static final Resource POINTCUT_EVENTS_CONTEXT =  qualifiedResource(CLASS, "pointcutEvents.xml");
	private static final Resource POINTCUT_REF_CONTEXT = qualifiedResource(CLASS, "pointcutRefEvents.xml");
	private static final Resource DIRECT_POINTCUT_EVENTS_CONTEXT = qualifiedResource(CLASS, "directPointcutEvents.xml");

	private CollectingReaderEventListener eventListener = new CollectingReaderEventListener();

	private XmlBeanDefinitionReader reader;

	private DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();



	@Before
	public void setUp() throws Exception {
		this.reader = new XmlBeanDefinitionReader(this.beanFactory);
		this.reader.setEventListener(this.eventListener);
	}

	@Test
	public void testPointcutEvents() throws Exception {
		this.reader.loadBeanDefinitions(POINTCUT_EVENTS_CONTEXT);
		ComponentDefinition[] componentDefinitions = this.eventListener.getComponentDefinitions();
		assertEquals("Incorrect number of events fired", 1, componentDefinitions.length);
		assertTrue("No holder with nested components", componentDefinitions[0] instanceof CompositeComponentDefinition);

		CompositeComponentDefinition compositeDef = (CompositeComponentDefinition) componentDefinitions[0];
		assertEquals("aop:config", compositeDef.getName());

		ComponentDefinition[] nestedComponentDefs = compositeDef.getNestedComponents();
		assertEquals("Incorrect number of inner components", 2, nestedComponentDefs.length);
		PointcutComponentDefinition pcd = null;
		for (int i = 0; i < nestedComponentDefs.length; i++) {
			ComponentDefinition componentDefinition = nestedComponentDefs[i];
			if (componentDefinition instanceof PointcutComponentDefinition) {
				pcd = (PointcutComponentDefinition) componentDefinition;
				break;
			}
		}
		assertNotNull("PointcutComponentDefinition not found", pcd);
		assertEquals("Incorrect number of BeanDefinitions", 1, pcd.getBeanDefinitions().length);
	}

	@Test
	public void testAdvisorEventsWithPointcutRef() throws Exception {
		this.reader.loadBeanDefinitions(POINTCUT_REF_CONTEXT);
		ComponentDefinition[] componentDefinitions = this.eventListener.getComponentDefinitions();
		assertEquals("Incorrect number of events fired", 2, componentDefinitions.length);

		assertTrue("No holder with nested components", componentDefinitions[0] instanceof CompositeComponentDefinition);
		CompositeComponentDefinition compositeDef = (CompositeComponentDefinition) componentDefinitions[0];
		assertEquals("aop:config", compositeDef.getName());

		ComponentDefinition[] nestedComponentDefs = compositeDef.getNestedComponents();
		assertEquals("Incorrect number of inner components", 3, nestedComponentDefs.length);
		AdvisorComponentDefinition acd = null;
		for (int i = 0; i < nestedComponentDefs.length; i++) {
			ComponentDefinition componentDefinition = nestedComponentDefs[i];
			if (componentDefinition instanceof AdvisorComponentDefinition) {
				acd = (AdvisorComponentDefinition) componentDefinition;
				break;
			}
		}
		assertNotNull("AdvisorComponentDefinition not found", acd);
		assertEquals(1, acd.getBeanDefinitions().length);
		assertEquals(2, acd.getBeanReferences().length);

		assertTrue("No advice bean found", componentDefinitions[1] instanceof BeanComponentDefinition);
		BeanComponentDefinition adviceDef = (BeanComponentDefinition) componentDefinitions[1];
		assertEquals("countingAdvice", adviceDef.getBeanName());
	}

	@Test
	public void testAdvisorEventsWithDirectPointcut() throws Exception {
		this.reader.loadBeanDefinitions(DIRECT_POINTCUT_EVENTS_CONTEXT);
		ComponentDefinition[] componentDefinitions = this.eventListener.getComponentDefinitions();
		assertEquals("Incorrect number of events fired", 2, componentDefinitions.length);

		assertTrue("No holder with nested components", componentDefinitions[0] instanceof CompositeComponentDefinition);
		CompositeComponentDefinition compositeDef = (CompositeComponentDefinition) componentDefinitions[0];
		assertEquals("aop:config", compositeDef.getName());

		ComponentDefinition[] nestedComponentDefs = compositeDef.getNestedComponents();
		assertEquals("Incorrect number of inner components", 2, nestedComponentDefs.length);
		AdvisorComponentDefinition acd = null;
		for (int i = 0; i < nestedComponentDefs.length; i++) {
			ComponentDefinition componentDefinition = nestedComponentDefs[i];
			if (componentDefinition instanceof AdvisorComponentDefinition) {
				acd = (AdvisorComponentDefinition) componentDefinition;
				break;
			}
		}
		assertNotNull("AdvisorComponentDefinition not found", acd);
		assertEquals(2, acd.getBeanDefinitions().length);
		assertEquals(1, acd.getBeanReferences().length);

		assertTrue("No advice bean found", componentDefinitions[1] instanceof BeanComponentDefinition);
		BeanComponentDefinition adviceDef = (BeanComponentDefinition) componentDefinitions[1];
		assertEquals("countingAdvice", adviceDef.getBeanName());
	}

	@Test
	public void testAspectEvent() throws Exception {
		this.reader.loadBeanDefinitions(CONTEXT);
		ComponentDefinition[] componentDefinitions = this.eventListener.getComponentDefinitions();
		assertEquals("Incorrect number of events fired", 5, componentDefinitions.length);

		assertTrue("No holder with nested components", componentDefinitions[0] instanceof CompositeComponentDefinition);
		CompositeComponentDefinition compositeDef = (CompositeComponentDefinition) componentDefinitions[0];
		assertEquals("aop:config", compositeDef.getName());

		ComponentDefinition[] nestedComponentDefs = compositeDef.getNestedComponents();
		assertEquals("Incorrect number of inner components", 2, nestedComponentDefs.length);
		AspectComponentDefinition acd = null;
		for (int i = 0; i < nestedComponentDefs.length; i++) {
			ComponentDefinition componentDefinition = nestedComponentDefs[i];
			if (componentDefinition instanceof AspectComponentDefinition) {
				acd = (AspectComponentDefinition) componentDefinition;
				break;
			}
		}

		assertNotNull("AspectComponentDefinition not found", acd);
		BeanDefinition[] beanDefinitions = acd.getBeanDefinitions();
		assertEquals(5, beanDefinitions.length);
		BeanReference[] beanReferences = acd.getBeanReferences();
		assertEquals(6, beanReferences.length);

		Set<String> expectedReferences = new HashSet<>();
		expectedReferences.add("pc");
		expectedReferences.add("countingAdvice");
		for (int i = 0; i < beanReferences.length; i++) {
			BeanReference beanReference = beanReferences[i];
			expectedReferences.remove(beanReference.getBeanName());
		}
		assertEquals("Incorrect references found", 0, expectedReferences.size());

		for (int i = 1; i < componentDefinitions.length; i++) {
			assertTrue(componentDefinitions[i] instanceof BeanComponentDefinition);
		}

		ComponentDefinition[] nestedComponentDefs2 = acd.getNestedComponents();
		assertEquals("Inner PointcutComponentDefinition not found", 1, nestedComponentDefs2.length);
		assertTrue(nestedComponentDefs2[0] instanceof PointcutComponentDefinition);
		PointcutComponentDefinition pcd = (PointcutComponentDefinition) nestedComponentDefs2[0];
		assertEquals("Incorrect number of BeanDefinitions", 1, pcd.getBeanDefinitions().length);
	}

}
