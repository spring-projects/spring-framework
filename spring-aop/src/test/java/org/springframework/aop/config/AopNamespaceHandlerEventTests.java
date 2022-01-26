/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.Arrays;
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
import org.springframework.beans.testfixture.beans.CollectingReaderEventListener;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.testfixture.io.ResourceTestUtils.qualifiedResource;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class AopNamespaceHandlerEventTests {

	private static final Class<?> CLASS = AopNamespaceHandlerEventTests.class;

	private static final Resource CONTEXT =  qualifiedResource(CLASS, "context.xml");
	private static final Resource POINTCUT_EVENTS_CONTEXT =  qualifiedResource(CLASS, "pointcutEvents.xml");
	private static final Resource POINTCUT_REF_CONTEXT = qualifiedResource(CLASS, "pointcutRefEvents.xml");
	private static final Resource DIRECT_POINTCUT_EVENTS_CONTEXT = qualifiedResource(CLASS, "directPointcutEvents.xml");

	private CollectingReaderEventListener eventListener = new CollectingReaderEventListener();

	private DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private XmlBeanDefinitionReader reader;


	@BeforeEach
	void setup() {
		this.reader = new XmlBeanDefinitionReader(this.beanFactory);
		this.reader.setEventListener(this.eventListener);
	}


	@Test
	void pointcutEvents() {
		this.reader.loadBeanDefinitions(POINTCUT_EVENTS_CONTEXT);
		ComponentDefinition[] componentDefinitions = this.eventListener.getComponentDefinitions();
		assertThat(componentDefinitions).as("Incorrect number of events fired").hasSize(1);
		assertThat(componentDefinitions[0]).as("No holder with nested components").isInstanceOf(CompositeComponentDefinition.class);

		CompositeComponentDefinition compositeDef = (CompositeComponentDefinition) componentDefinitions[0];
		assertThat(compositeDef.getName()).isEqualTo("aop:config");

		ComponentDefinition[] nestedComponentDefs = compositeDef.getNestedComponents();
		assertThat(nestedComponentDefs).as("Incorrect number of inner components").hasSize(2);
		PointcutComponentDefinition pcd = null;
		for (ComponentDefinition componentDefinition : nestedComponentDefs) {
			if (componentDefinition instanceof PointcutComponentDefinition) {
				pcd = (PointcutComponentDefinition) componentDefinition;
				break;
			}
		}
		assertThat(pcd).as("PointcutComponentDefinition not found").isNotNull();
		assertThat(pcd.getBeanDefinitions()).as("Incorrect number of BeanDefinitions").hasSize(1);
	}

	@Test
	void advisorEventsWithPointcutRef() {
		this.reader.loadBeanDefinitions(POINTCUT_REF_CONTEXT);
		ComponentDefinition[] componentDefinitions = this.eventListener.getComponentDefinitions();
		assertThat(componentDefinitions).as("Incorrect number of events fired").hasSize(2);

		assertThat(componentDefinitions[0]).as("No holder with nested components").isInstanceOf(CompositeComponentDefinition.class);
		CompositeComponentDefinition compositeDef = (CompositeComponentDefinition) componentDefinitions[0];
		assertThat(compositeDef.getName()).isEqualTo("aop:config");

		ComponentDefinition[] nestedComponentDefs = compositeDef.getNestedComponents();
		assertThat(nestedComponentDefs).as("Incorrect number of inner components").hasSize(3);
		AdvisorComponentDefinition acd = null;
		for (ComponentDefinition componentDefinition : nestedComponentDefs) {
			if (componentDefinition instanceof AdvisorComponentDefinition) {
				acd = (AdvisorComponentDefinition) componentDefinition;
				break;
			}
		}
		assertThat(acd).as("AdvisorComponentDefinition not found").isNotNull();
		assertThat(acd.getBeanDefinitions()).hasSize(1);
		assertThat(acd.getBeanReferences()).hasSize(2);

		assertThat(componentDefinitions[1]).as("No advice bean found").isInstanceOf(BeanComponentDefinition.class);
		BeanComponentDefinition adviceDef = (BeanComponentDefinition) componentDefinitions[1];
		assertThat(adviceDef.getBeanName()).isEqualTo("countingAdvice");
	}

	@Test
	void advisorEventsWithDirectPointcut() {
		this.reader.loadBeanDefinitions(DIRECT_POINTCUT_EVENTS_CONTEXT);
		ComponentDefinition[] componentDefinitions = this.eventListener.getComponentDefinitions();
		assertThat(componentDefinitions).as("Incorrect number of events fired").hasSize(2);

		assertThat(componentDefinitions[0]).as("No holder with nested components").isInstanceOf(CompositeComponentDefinition.class);
		CompositeComponentDefinition compositeDef = (CompositeComponentDefinition) componentDefinitions[0];
		assertThat(compositeDef.getName()).isEqualTo("aop:config");

		ComponentDefinition[] nestedComponentDefs = compositeDef.getNestedComponents();
		assertThat(nestedComponentDefs).as("Incorrect number of inner components").hasSize(2);
		AdvisorComponentDefinition acd = null;
		for (ComponentDefinition componentDefinition : nestedComponentDefs) {
			if (componentDefinition instanceof AdvisorComponentDefinition) {
				acd = (AdvisorComponentDefinition) componentDefinition;
				break;
			}
		}
		assertThat(acd).as("AdvisorComponentDefinition not found").isNotNull();
		assertThat(acd.getBeanDefinitions()).hasSize(2);
		assertThat(acd.getBeanReferences()).hasSize(1);

		assertThat(componentDefinitions[1]).as("No advice bean found").isInstanceOf(BeanComponentDefinition.class);
		BeanComponentDefinition adviceDef = (BeanComponentDefinition) componentDefinitions[1];
		assertThat(adviceDef.getBeanName()).isEqualTo("countingAdvice");
	}

	@Test
	void aspectEvent() {
		this.reader.loadBeanDefinitions(CONTEXT);
		ComponentDefinition[] componentDefinitions = this.eventListener.getComponentDefinitions();
		assertThat(componentDefinitions).as("Incorrect number of events fired").hasSize(2);

		assertThat(componentDefinitions[0]).as("No holder with nested components").isInstanceOf(CompositeComponentDefinition.class);
		CompositeComponentDefinition compositeDef = (CompositeComponentDefinition) componentDefinitions[0];
		assertThat(compositeDef.getName()).isEqualTo("aop:config");

		ComponentDefinition[] nestedComponentDefs = compositeDef.getNestedComponents();
		assertThat(nestedComponentDefs).as("Incorrect number of inner components").hasSize(2);
		AspectComponentDefinition acd = null;
		for (ComponentDefinition componentDefinition : nestedComponentDefs) {
			if (componentDefinition instanceof AspectComponentDefinition) {
				acd = (AspectComponentDefinition) componentDefinition;
				break;
			}
		}

		assertThat(acd).as("AspectComponentDefinition not found").isNotNull();
		BeanDefinition[] beanDefinitions = acd.getBeanDefinitions();
		assertThat(beanDefinitions).hasSize(5);
		BeanReference[] beanReferences = acd.getBeanReferences();
		assertThat(beanReferences).hasSize(6);

		Set<String> expectedReferences = new HashSet<>();
		expectedReferences.add("pc");
		expectedReferences.add("countingAdvice");
		for (BeanReference beanReference : beanReferences) {
			expectedReferences.remove(beanReference.getBeanName());
		}
		assertThat(expectedReferences).as("Incorrect references found").isEmpty();

		Arrays.stream(componentDefinitions).skip(1).forEach(definition ->
			assertThat(definition).isInstanceOf(BeanComponentDefinition.class));

		ComponentDefinition[] nestedComponentDefs2 = acd.getNestedComponents();
		assertThat(nestedComponentDefs2).as("Inner PointcutComponentDefinition not found").hasSize(1);
		assertThat(nestedComponentDefs2[0]).isInstanceOf(PointcutComponentDefinition.class);
		PointcutComponentDefinition pcd = (PointcutComponentDefinition) nestedComponentDefs2[0];
		assertThat(pcd.getBeanDefinitions()).as("Incorrect number of BeanDefinitions").hasSize(1);
	}

}
