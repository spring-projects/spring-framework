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

package org.springframework.beans.factory.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.util.ClassUtils;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
public class QualifierAnnotationAutowireBeanFactoryTests {

	private static final String JUERGEN = "juergen";

	private static final String MARK = "mark";


	@Test
	public void testAutowireCandidateDefaultWithIrrelevantDescriptor() throws Exception {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition rbd = new RootBeanDefinition(Person.class, cavs, null);
		lbf.registerBeanDefinition(JUERGEN, rbd);
		assertTrue(lbf.isAutowireCandidate(JUERGEN, null));
		assertTrue(lbf.isAutowireCandidate(JUERGEN,
				new DependencyDescriptor(Person.class.getDeclaredField("name"), false)));
		assertTrue(lbf.isAutowireCandidate(JUERGEN,
				new DependencyDescriptor(Person.class.getDeclaredField("name"), true)));
	}

	@Test
	public void testAutowireCandidateExplicitlyFalseWithIrrelevantDescriptor() throws Exception {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition rbd = new RootBeanDefinition(Person.class, cavs, null);
		rbd.setAutowireCandidate(false);
		lbf.registerBeanDefinition(JUERGEN, rbd);
		assertFalse(lbf.isAutowireCandidate(JUERGEN, null));
		assertFalse(lbf.isAutowireCandidate(JUERGEN,
				new DependencyDescriptor(Person.class.getDeclaredField("name"), false)));
		assertFalse(lbf.isAutowireCandidate(JUERGEN,
				new DependencyDescriptor(Person.class.getDeclaredField("name"), true)));
	}

	@Ignore
	@Test
	public void testAutowireCandidateWithFieldDescriptor() throws Exception {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
		cavs1.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
		person1.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
		lbf.registerBeanDefinition(JUERGEN, person1);
		ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
		cavs2.addGenericArgumentValue(MARK);
		RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
		lbf.registerBeanDefinition(MARK, person2);
		DependencyDescriptor qualifiedDescriptor = new DependencyDescriptor(
				QualifiedTestBean.class.getDeclaredField("qualified"), false);
		DependencyDescriptor nonqualifiedDescriptor = new DependencyDescriptor(
				QualifiedTestBean.class.getDeclaredField("nonqualified"), false);
		assertTrue(lbf.isAutowireCandidate(JUERGEN, null));
		assertTrue(lbf.isAutowireCandidate(JUERGEN, nonqualifiedDescriptor));
		assertTrue(lbf.isAutowireCandidate(JUERGEN, qualifiedDescriptor));
		assertTrue(lbf.isAutowireCandidate(MARK, null));
		assertTrue(lbf.isAutowireCandidate(MARK, nonqualifiedDescriptor));
		assertFalse(lbf.isAutowireCandidate(MARK, qualifiedDescriptor));
	}

	@Test
	public void testAutowireCandidateExplicitlyFalseWithFieldDescriptor() throws Exception {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person = new RootBeanDefinition(Person.class, cavs, null);
		person.setAutowireCandidate(false);
		person.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
		lbf.registerBeanDefinition(JUERGEN, person);
		DependencyDescriptor qualifiedDescriptor = new DependencyDescriptor(
				QualifiedTestBean.class.getDeclaredField("qualified"), false);
		DependencyDescriptor nonqualifiedDescriptor = new DependencyDescriptor(
				QualifiedTestBean.class.getDeclaredField("nonqualified"), false);
		assertFalse(lbf.isAutowireCandidate(JUERGEN, null));
		assertFalse(lbf.isAutowireCandidate(JUERGEN, nonqualifiedDescriptor));
		assertFalse(lbf.isAutowireCandidate(JUERGEN, qualifiedDescriptor));
	}

	@Test
	public void testAutowireCandidateWithShortClassName() throws Exception {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person = new RootBeanDefinition(Person.class, cavs, null);
		person.addQualifier(new AutowireCandidateQualifier(ClassUtils.getShortName(TestQualifier.class)));
		lbf.registerBeanDefinition(JUERGEN, person);
		DependencyDescriptor qualifiedDescriptor = new DependencyDescriptor(
				QualifiedTestBean.class.getDeclaredField("qualified"), false);
		DependencyDescriptor nonqualifiedDescriptor = new DependencyDescriptor(
				QualifiedTestBean.class.getDeclaredField("nonqualified"), false);
		assertTrue(lbf.isAutowireCandidate(JUERGEN, null));
		assertTrue(lbf.isAutowireCandidate(JUERGEN, nonqualifiedDescriptor));
		assertTrue(lbf.isAutowireCandidate(JUERGEN, qualifiedDescriptor));
	}

	@Ignore
	@Test
	public void testAutowireCandidateWithConstructorDescriptor() throws Exception {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
		cavs1.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
		person1.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
		lbf.registerBeanDefinition(JUERGEN, person1);
		ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
		cavs2.addGenericArgumentValue(MARK);
		RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
		lbf.registerBeanDefinition(MARK, person2);
		MethodParameter param = new MethodParameter(QualifiedTestBean.class.getDeclaredConstructor(Person.class), 0);
		DependencyDescriptor qualifiedDescriptor = new DependencyDescriptor(param, false);
		param.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
		assertEquals("tpb", param.getParameterName());
		assertTrue(lbf.isAutowireCandidate(JUERGEN, null));
		assertTrue(lbf.isAutowireCandidate(JUERGEN, qualifiedDescriptor));
		assertFalse(lbf.isAutowireCandidate(MARK, qualifiedDescriptor));
	}

	@Ignore
	@Test
	public void testAutowireCandidateWithMethodDescriptor() throws Exception {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
		cavs1.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
		person1.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
		lbf.registerBeanDefinition(JUERGEN, person1);
		ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
		cavs2.addGenericArgumentValue(MARK);
		RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
		lbf.registerBeanDefinition(MARK, person2);
		MethodParameter qualifiedParam =
				new MethodParameter(QualifiedTestBean.class.getDeclaredMethod("autowireQualified", Person.class), 0);
		MethodParameter nonqualifiedParam =
				new MethodParameter(QualifiedTestBean.class.getDeclaredMethod("autowireNonqualified", Person.class), 0);
		DependencyDescriptor qualifiedDescriptor = new DependencyDescriptor(qualifiedParam, false);
		DependencyDescriptor nonqualifiedDescriptor = new DependencyDescriptor(nonqualifiedParam, false);
		qualifiedParam.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
		assertEquals("tpb", qualifiedParam.getParameterName());
		nonqualifiedParam.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
		assertEquals("tpb", nonqualifiedParam.getParameterName());
		assertTrue(lbf.isAutowireCandidate(JUERGEN, null));
		assertTrue(lbf.isAutowireCandidate(JUERGEN, nonqualifiedDescriptor));
		assertTrue(lbf.isAutowireCandidate(JUERGEN, qualifiedDescriptor));
		assertTrue(lbf.isAutowireCandidate(MARK, null));
		assertTrue(lbf.isAutowireCandidate(MARK, nonqualifiedDescriptor));
		assertFalse(lbf.isAutowireCandidate(MARK, qualifiedDescriptor));
	}

	@Test
	public void testAutowireCandidateWithMultipleCandidatesDescriptor() throws Exception {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
		cavs1.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
		person1.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
		lbf.registerBeanDefinition(JUERGEN, person1);
		ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
		cavs2.addGenericArgumentValue(MARK);
		RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
		person2.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
		lbf.registerBeanDefinition(MARK, person2);
		DependencyDescriptor qualifiedDescriptor = new DependencyDescriptor(
				new MethodParameter(QualifiedTestBean.class.getDeclaredConstructor(Person.class), 0),
				false);
		assertTrue(lbf.isAutowireCandidate(JUERGEN, qualifiedDescriptor));
		assertTrue(lbf.isAutowireCandidate(MARK, qualifiedDescriptor));
	}


	public static class QualifiedTestBean {

		@TestQualifier
		@SuppressWarnings("unused")
		private Person qualified;

		@SuppressWarnings("unused")
		private Person nonqualified;

		public QualifiedTestBean(@TestQualifier Person tpb) {
		}

		public void autowireQualified(@TestQualifier Person tpb) {
		}

		public void autowireNonqualified(Person tpb) {
		}
	}


	public static class Person {

		private String name;

		public Person(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}
	}


	@Target({ElementType.FIELD, ElementType.PARAMETER})
	@Retention(RetentionPolicy.RUNTIME)
	@Qualifier
	private static @interface TestQualifier {
	}

}
