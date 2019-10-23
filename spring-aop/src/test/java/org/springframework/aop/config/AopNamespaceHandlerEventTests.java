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

package org.springframework.aop.config;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.ComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.Resource;
import org.springframework.tests.beans.CollectingReaderEventListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.tests.TestResourceUtils.qualifiedResource;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class AopNamespaceHandlerEventTests {

	private static final Class<?> CLASS = AopNamespaceHandlerEventTests.class;

	private static final Resource CONTEXT =  qualifiedResource(CLASS, "context.xml");
	private static final Resource POINTCUT_EVENTS_CONTEXT =  qualifiedResource(CLASS, "pointcutEvents.xml");
	private static final Resource POINTCUT_REF_CONTEXT = qualifiedResource(CLASS, "pointcutRefEvents.xml");
	private static final Resource DIRECT_POINTCUT_EVENTS_CONTEXT = qualifiedResource(CLASS, "directPointcutEvents.xml");

	private CollectingReaderEventListener eventListener = new CollectingReaderEventListener();

	private DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private XmlBeanDefinitionReader reader;


	@BeforeEach
	public void setup() {
		this.reader = new XmlBeanDefinitionReader(this.beanFactory);
		this.reader.setEventListener(this.eventListener);
	}


	@Test
	public void testPointcutEvents() {
		this.reader.loadBeanDefinitions(POINTCUT_EVENTS_CONTEXT);
		ComponentDefinition[] componentDefinitions = this.eventListener.getComponentDefinitions();
		assertThat(componentDefinitions.length).as("Incorrect number of events fired").isEqualTo(1);
		boolean condition = componentDefinitions[0] instanceof CompositeComponentDefinition;
		assertThat(condition).as("No holder with nested components").isTrue();

		CompositeComponentDefinition compositeDef = (CompositeComponentDefinition) componentDefinitions[0];
		assertThat(compositeDef.getName()).isEqualTo("aop:config");

		ComponentDefinition[] nestedComponentDefs = compositeDef.getNestedComponents();
		assertThat(nestedComponentDefs.length).as("Incorrect number of inner components").isEqualTo(2);
		PointcutComponentDefinition pcd = null;
		for (ComponentDefinition componentDefinition : nestedComponentDefs) {
			if (componentDefinition instanceof PointcutComponentDefinition) {
				pcd = (PointcutComponentDefinition) componentDefinition;
				break;
			}
		}
		assertThat(pcd).as("PointcutComponentDefinition not found").isNotNull();
		assertThat(pcd.getBeanDefinitions().length).as("Incorrect number of BeanDefinitions").isEqualTo(1);
	}

	@Test
	public void testAdvisorEventsWithPointcutRef() {
		this.reader.loadBeanDefinitions(POINTCUT_REF_CONTEXT);
		ComponentDefinition[] componentDefinitions = this.eventListener.getComponentDefinitions();
		assertThat(componentDefinitions.length).as("Incorrect number of events fired").isEqualTo(2);

		boolean condition1 = componentDefinitions[0] instanceof CompositeComponentDefinition;
		assertThat(condition1).as("No holder with nested components").isTrue();
		CompositeComponentDefinition compositeDef = (CompositeComponentDefinition) componentDefinitions[0];
		assertThat(compositeDef.getName()).isEqualTo("aop:config");

		ComponentDefinition[] nestedComponentDefs = compositeDef.getNestedComponents();
		assertThat(nestedComponentDefs.length).as("Incorrect number of inner components").isEqualTo(3);
		AdvisorComponentDefinition acd = null;
		for (int i = 0; i < nestedComponentDefs.length; i++) {
			ComponentDefinition componentDefinition = nestedComponentDefs[i];
			if (componentDefinition instanceof AdvisorComponentDefinition) {
				acd = (AdvisorComponentDefinition) componentDefinition;
				break;
			}
		}
		assertThat(acd).as("AdvisorComponentDefinition not found").isNotNull();
		assertThat(acd.getBeanDefinitions().length).isEqualTo(1);
		assertThat(acd.getBeanReferences().length).isEqualTo(2);

		boolean condition = componentDefinitions[1] instanceof BeanComponentDefinition;
		assertThat(condition).as("No advice bean found").isTrue();
		BeanComponentDefinition adviceDef = (BeanComponentDefinition) componentDefinitions[1];
		assertThat(adviceDef.getBeanName()).isEqualTo("countingAdvice");
	}

	@Test
	public void testAdvisorEventsWithDirectPointcut() {
		this.reader.loadBeanDefinitions(DIRECT_POINTCUT_EVENTS_CONTEXT);
		ComponentDefinition[] componentDefinitions = this.eventListener.getComponentDefinitions();
		assertThat(componentDefinitions.length).as("Incorrect number of events fired").isEqualTo(2);

		boolean condition1 = componentDefinitions[0] instanceof CompositeComponentDefinition;
		assertThat(condition1).as("No holder with nested components").isTrue();
		CompositeComponentDefinition compositeDef = (CompositeComponentDefinition) componentDefinitions[0];
		assertThat(compositeDef.getName()).isEqualTo("aop:config");

		ComponentDefinition[] nestedComponentDefs = compositeDef.getNestedComponents();
		assertThat(nestedComponentDefs.length).as("Incorrect number of inner components").isEqualTo(2);
		AdvisorComponentDefinition acd = null;
		for (int i = 0; i < nestedComponentDefs.length; i++) {
			ComponentDefinition componentDefinition = nestedComponentDefs[i];
			if (componentDefinition instanceof AdvisorComponentDefinition) {
				acd = (AdvisorComponentDefinition) componentDefinition;
				break;
			}
		}
		assertThat(acd).as("AdvisorComponentDefinition not found").isNotNull();
		assertThat(acd.getBeanDefinitions().length).isEqualTo(2);
		assertThat(acd.getBeanReferences().length).isEqualTo(1);

		boolean condition = componentDefinitions[1] instanceof BeanComponentDefinition;
		assertThat(condition).as("No advice bean found").isTrue();
		BeanComponentDefinition adviceDef = (BeanComponentDefinition) componentDefinitions[1];
		assertThat(adviceDef.getBeanName()).isEqualTo("countingAdvice");
	}

	@Test
	public void testAspectEvent() {
		this.reader.loadBeanDefinitions(CONTEXT);
		ComponentDefinition[] componentDefinitions = this.eventListener.getComponentDefinitions();
		assertThat(componentDefinitions.length).as("Incorrect number of events fired").isEqualTo(5);

		boolean condition = componentDefinitions[0] instanceof CompositeComponentDefinition;
		assertThat(condition).as("No holder with nested components").isTrue();
		CompositeComponentDefinition compositeDef = (CompositeComponentDefinition) componentDefinitions[0];
		assertThat(compositeDef.getName()).isEqualTo("aop:config");

		ComponentDefinition[] nestedComponentDefs = compositeDef.getNestedComponents();
		assertThat(nestedComponentDefs.length).as("Incorrect number of inner components").isEqualTo(2);
		AspectComponentDefinition acd = null;
		for (ComponentDefinition componentDefinition : nestedComponentDefs) {
			if (componentDefinition instanceof AspectComponentDefinition) {
				acd = (AspectComponentDefinition) componentDefinition;
				break;
			}
		}

		assertThat(acd).as("AspectComponentDefinition not found").isNotNull();
		BeanDefinition[] beanDefinitions = acd.getBeanDefinitions();
		assertThat(beanDefinitions.length).isEqualTo(5);
		BeanReference[] beanReferences = acd.getBeanReferences();
		assertThat(beanReferences.length).isEqualTo(6);

		Set<String> expectedReferences = new HashSet<>();
		expectedReferences.add("pc");
		expectedReferences.add("countingAdvice");
		for (BeanReference beanReference : beanReferences) {
			expectedReferences.remove(beanReference.getBeanName());
		}
		assertThat(expectedReferences.size()).as("Incorrect references found").isEqualTo(0);

		for (int i = 1; i < componentDefinitions.length; i++) {
			boolean condition1 = componentDefinitions[i] instanceof BeanComponentDefinition;
			assertThat(condition1).isTrue();
		}

		ComponentDefinition[] nestedComponentDefs2 = acd.getNestedComponents();
		assertThat(nestedComponentDefs2.length).as("Inner PointcutComponentDefinition not found").isEqualTo(1);
		boolean condition1 = nestedComponentDefs2[0] instanceof PointcutComponentDefinition;
		assertThat(condition1).isTrue();
		PointcutComponentDefinition pcd = (PointcutComponentDefinition) nestedComponentDefs2[0];
		assertThat(pcd.getBeanDefinitions().length).as("Incorrect number of BeanDefinitions").isEqualTo(1);
	}

}
