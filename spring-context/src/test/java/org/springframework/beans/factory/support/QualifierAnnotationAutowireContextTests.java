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

package org.springframework.beans.factory.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for handling {@link Qualifier} annotations.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 */
class QualifierAnnotationAutowireContextTests {

	private static final String JUERGEN = "juergen";

	private static final String MARK = "mark";


	@Test
	void autowiredFieldWithSingleNonQualifiedCandidate() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person = new RootBeanDefinition(Person.class, cavs, null);
		context.registerBeanDefinition(JUERGEN, person);
		context.registerBeanDefinition("autowired",
				new RootBeanDefinition(QualifiedFieldTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);

		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(context::refresh)
				.satisfies(ex -> {
					assertThat(ex.getRootCause()).isInstanceOf(NoSuchBeanDefinitionException.class);
					assertThat(ex.getBeanName()).isEqualTo("autowired");
				});
	}

	@Test
	void autowiredMethodParameterWithSingleNonQualifiedCandidate() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person = new RootBeanDefinition(Person.class, cavs, null);
		context.registerBeanDefinition(JUERGEN, person);
		context.registerBeanDefinition("autowired",
				new RootBeanDefinition(QualifiedMethodParameterTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);

		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(context::refresh)
				.satisfies(ex -> {
					assertThat(ex.getRootCause()).isInstanceOf(NoSuchBeanDefinitionException.class);
					assertThat(ex.getBeanName()).isEqualTo("autowired");
				});

	}

	@Test
	void autowiredConstructorArgumentWithSingleNonQualifiedCandidate() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person = new RootBeanDefinition(Person.class, cavs, null);
		context.registerBeanDefinition(JUERGEN, person);
		context.registerBeanDefinition("autowired",
				new RootBeanDefinition(QualifiedConstructorArgumentTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);

		assertThatExceptionOfType(UnsatisfiedDependencyException.class)
				.isThrownBy(context::refresh)
				.satisfies(ex -> assertThat(ex.getBeanName()).isEqualTo("autowired"));
	}

	@Test
	void autowiredFieldWithSingleQualifiedCandidate() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person = new RootBeanDefinition(Person.class, cavs, null);
		person.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
		context.registerBeanDefinition(JUERGEN, person);
		context.registerBeanDefinition("autowired", new RootBeanDefinition(QualifiedFieldTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		context.refresh();
		QualifiedFieldTestBean bean = (QualifiedFieldTestBean) context.getBean("autowired");
		assertThat(bean.getPerson().getName()).isEqualTo(JUERGEN);
	}

	@Test
	void autowiredMethodParameterWithSingleQualifiedCandidate() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person = new RootBeanDefinition(Person.class, cavs, null);
		person.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
		context.registerBeanDefinition(JUERGEN, person);
		context.registerBeanDefinition("autowired",
				new RootBeanDefinition(QualifiedMethodParameterTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		context.refresh();
		QualifiedMethodParameterTestBean bean =
				(QualifiedMethodParameterTestBean) context.getBean("autowired");
		assertThat(bean.getPerson().getName()).isEqualTo(JUERGEN);
	}

	@Test
	void autowiredMethodParameterWithStaticallyQualifiedCandidate() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person = new RootBeanDefinition(QualifiedPerson.class, cavs, null);
		context.registerBeanDefinition(JUERGEN,
				ScopedProxyUtils.createScopedProxy(new BeanDefinitionHolder(person, JUERGEN), context, true).getBeanDefinition());
		context.registerBeanDefinition("autowired",
				new RootBeanDefinition(QualifiedMethodParameterTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		context.refresh();
		QualifiedMethodParameterTestBean bean =
				(QualifiedMethodParameterTestBean) context.getBean("autowired");
		assertThat(bean.getPerson().getName()).isEqualTo(JUERGEN);
	}

	@Test
	void autowiredMethodParameterWithStaticallyQualifiedCandidateAmongOthers() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person = new RootBeanDefinition(QualifiedPerson.class, cavs, null);
		ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
		cavs2.addGenericArgumentValue(MARK);
		RootBeanDefinition person2 = new RootBeanDefinition(DefaultValueQualifiedPerson.class, cavs2, null);
		context.registerBeanDefinition(JUERGEN, person);
		context.registerBeanDefinition(MARK, person2);
		context.registerBeanDefinition("autowired",
				new RootBeanDefinition(QualifiedMethodParameterTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		context.refresh();
		QualifiedMethodParameterTestBean bean =
				(QualifiedMethodParameterTestBean) context.getBean("autowired");
		assertThat(bean.getPerson().getName()).isEqualTo(JUERGEN);
	}

	@Test
	void autowiredConstructorArgumentWithSingleQualifiedCandidate() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person = new RootBeanDefinition(Person.class, cavs, null);
		person.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
		context.registerBeanDefinition(JUERGEN, person);
		context.registerBeanDefinition("autowired",
				new RootBeanDefinition(QualifiedConstructorArgumentTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		context.refresh();
		QualifiedConstructorArgumentTestBean bean =
				(QualifiedConstructorArgumentTestBean) context.getBean("autowired");
		assertThat(bean.getPerson().getName()).isEqualTo(JUERGEN);
	}

	@Test
	void autowiredFieldWithMultipleNonQualifiedCandidates() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
		cavs1.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
		ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
		cavs2.addGenericArgumentValue(MARK);
		RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
		context.registerBeanDefinition(JUERGEN, person1);
		context.registerBeanDefinition(MARK, person2);
		context.registerBeanDefinition("autowired",
				new RootBeanDefinition(QualifiedFieldTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);

		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(context::refresh)
				.satisfies(ex -> {
					assertThat(ex.getRootCause()).isInstanceOf(NoSuchBeanDefinitionException.class);
					assertThat(ex.getBeanName()).isEqualTo("autowired");
				});
	}

	@Test
	void autowiredMethodParameterWithMultipleNonQualifiedCandidates() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
		cavs1.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
		ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
		cavs2.addGenericArgumentValue(MARK);
		RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
		context.registerBeanDefinition(JUERGEN, person1);
		context.registerBeanDefinition(MARK, person2);
		context.registerBeanDefinition("autowired",
				new RootBeanDefinition(QualifiedMethodParameterTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);

		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(context::refresh)
				.satisfies(ex -> {
					assertThat(ex.getRootCause()).isInstanceOf(NoSuchBeanDefinitionException.class);
					assertThat(ex.getBeanName()).isEqualTo("autowired");
				});
	}

	@Test
	void autowiredConstructorArgumentWithMultipleNonQualifiedCandidates() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
		cavs1.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
		ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
		cavs2.addGenericArgumentValue(MARK);
		RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
		context.registerBeanDefinition(JUERGEN, person1);
		context.registerBeanDefinition(MARK, person2);
		context.registerBeanDefinition("autowired",
				new RootBeanDefinition(QualifiedConstructorArgumentTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);

		assertThatExceptionOfType(UnsatisfiedDependencyException.class)
				.isThrownBy(context::refresh)
				.satisfies(ex -> assertThat(ex.getBeanName()).isEqualTo("autowired"));
	}

	@Test
	void autowiredFieldResolvesQualifiedCandidate() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
		cavs1.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
		person1.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
		ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
		cavs2.addGenericArgumentValue(MARK);
		RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
		context.registerBeanDefinition(JUERGEN, person1);
		context.registerBeanDefinition(MARK, person2);
		context.registerBeanDefinition("autowired",
				new RootBeanDefinition(QualifiedFieldTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		context.refresh();
		QualifiedFieldTestBean bean = (QualifiedFieldTestBean) context.getBean("autowired");
		assertThat(bean.getPerson().getName()).isEqualTo(JUERGEN);
	}

	@Test
	void autowiredFieldResolvesMetaQualifiedCandidate() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
		cavs1.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
		person1.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
		ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
		cavs2.addGenericArgumentValue(MARK);
		RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
		context.registerBeanDefinition(JUERGEN, person1);
		context.registerBeanDefinition(MARK, person2);
		context.registerBeanDefinition("autowired",
				new RootBeanDefinition(MetaQualifiedFieldTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		context.refresh();
		MetaQualifiedFieldTestBean bean = (MetaQualifiedFieldTestBean) context.getBean("autowired");
		assertThat(bean.getPerson().getName()).isEqualTo(JUERGEN);
	}

	@Test
	void autowiredMethodParameterResolvesQualifiedCandidate() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
		cavs1.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
		person1.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
		ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
		cavs2.addGenericArgumentValue(MARK);
		RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
		context.registerBeanDefinition(JUERGEN, person1);
		context.registerBeanDefinition(MARK, person2);
		context.registerBeanDefinition("autowired",
				new RootBeanDefinition(QualifiedMethodParameterTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		context.refresh();
		QualifiedMethodParameterTestBean bean =
				(QualifiedMethodParameterTestBean) context.getBean("autowired");
		assertThat(bean.getPerson().getName()).isEqualTo(JUERGEN);
	}

	@Test
	void autowiredConstructorArgumentResolvesQualifiedCandidate() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
		cavs1.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
		person1.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
		ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
		cavs2.addGenericArgumentValue(MARK);
		RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
		context.registerBeanDefinition(JUERGEN, person1);
		context.registerBeanDefinition(MARK, person2);
		context.registerBeanDefinition("autowired",
				new RootBeanDefinition(QualifiedConstructorArgumentTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		context.refresh();
		QualifiedConstructorArgumentTestBean bean =
				(QualifiedConstructorArgumentTestBean) context.getBean("autowired");
		assertThat(bean.getPerson().getName()).isEqualTo(JUERGEN);
	}

	@Test
	void autowiredFieldResolvesQualifiedCandidateWithDefaultValueAndNoValueOnBeanDefinition() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
		cavs1.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
		// qualifier added, but includes no value
		person1.addQualifier(new AutowireCandidateQualifier(TestQualifierWithDefaultValue.class));
		ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
		cavs2.addGenericArgumentValue(MARK);
		RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
		context.registerBeanDefinition(JUERGEN, person1);
		context.registerBeanDefinition(MARK, person2);
		context.registerBeanDefinition("autowired",
				new RootBeanDefinition(QualifiedFieldWithDefaultValueTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		context.refresh();
		QualifiedFieldWithDefaultValueTestBean bean =
				(QualifiedFieldWithDefaultValueTestBean) context.getBean("autowired");
		assertThat(bean.getPerson().getName()).isEqualTo(JUERGEN);
	}

	@Test
	void autowiredFieldDoesNotResolveCandidateWithDefaultValueAndConflictingValueOnBeanDefinition() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
		cavs1.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
		// qualifier added, and non-default value specified
		person1.addQualifier(new AutowireCandidateQualifier(TestQualifierWithDefaultValue.class, "not the default"));
		ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
		cavs2.addGenericArgumentValue(MARK);
		RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
		context.registerBeanDefinition(JUERGEN, person1);
		context.registerBeanDefinition(MARK, person2);
		context.registerBeanDefinition("autowired",
				new RootBeanDefinition(QualifiedFieldWithDefaultValueTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);

		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(context::refresh)
				.satisfies(ex -> {
					assertThat(ex.getRootCause()).isInstanceOf(NoSuchBeanDefinitionException.class);
					assertThat(ex.getBeanName()).isEqualTo("autowired");
				});
	}

	@Test
	void autowiredFieldResolvesWithDefaultValueAndExplicitDefaultValueOnBeanDefinition() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
		cavs1.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
		// qualifier added, and value matches the default
		person1.addQualifier(new AutowireCandidateQualifier(TestQualifierWithDefaultValue.class, "default"));
		ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
		cavs2.addGenericArgumentValue(MARK);
		RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
		context.registerBeanDefinition(JUERGEN, person1);
		context.registerBeanDefinition(MARK, person2);
		context.registerBeanDefinition("autowired",
				new RootBeanDefinition(QualifiedFieldWithDefaultValueTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		context.refresh();
		QualifiedFieldWithDefaultValueTestBean bean =
				(QualifiedFieldWithDefaultValueTestBean) context.getBean("autowired");
		assertThat(bean.getPerson().getName()).isEqualTo(JUERGEN);
	}

	@Test
	void autowiredFieldResolvesWithMultipleQualifierValues() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
		cavs1.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
		AutowireCandidateQualifier qualifier = new AutowireCandidateQualifier(TestQualifierWithMultipleAttributes.class);
		qualifier.setAttribute("number", 456);
		person1.addQualifier(qualifier);
		ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
		cavs2.addGenericArgumentValue(MARK);
		RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
		AutowireCandidateQualifier qualifier2 = new AutowireCandidateQualifier(TestQualifierWithMultipleAttributes.class);
		qualifier2.setAttribute("number", 123);
		person2.addQualifier(qualifier2);
		context.registerBeanDefinition(JUERGEN, person1);
		context.registerBeanDefinition(MARK, person2);
		context.registerBeanDefinition("autowired",
				new RootBeanDefinition(QualifiedFieldWithMultipleAttributesTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		context.refresh();
		QualifiedFieldWithMultipleAttributesTestBean bean =
				(QualifiedFieldWithMultipleAttributesTestBean) context.getBean("autowired");
		assertThat(bean.getPerson().getName()).isEqualTo(MARK);
	}

	@Test
	void autowiredFieldDoesNotResolveWithMultipleQualifierValuesAndConflictingDefaultValue() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
		cavs1.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
		AutowireCandidateQualifier qualifier = new AutowireCandidateQualifier(TestQualifierWithMultipleAttributes.class);
		qualifier.setAttribute("number", 456);
		person1.addQualifier(qualifier);
		ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
		cavs2.addGenericArgumentValue(MARK);
		RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
		AutowireCandidateQualifier qualifier2 = new AutowireCandidateQualifier(TestQualifierWithMultipleAttributes.class);
		qualifier2.setAttribute("number", 123);
		qualifier2.setAttribute("value", "not the default");
		person2.addQualifier(qualifier2);
		context.registerBeanDefinition(JUERGEN, person1);
		context.registerBeanDefinition(MARK, person2);
		context.registerBeanDefinition("autowired",
				new RootBeanDefinition(QualifiedFieldWithMultipleAttributesTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);

		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(context::refresh)
				.satisfies(ex -> {
					assertThat(ex.getRootCause()).isInstanceOf(NoSuchBeanDefinitionException.class);
					assertThat(ex.getBeanName()).isEqualTo("autowired");
				});
	}

	@Test
	void autowiredFieldResolvesWithMultipleQualifierValuesAndExplicitDefaultValue() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
		cavs1.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
		AutowireCandidateQualifier qualifier = new AutowireCandidateQualifier(TestQualifierWithMultipleAttributes.class);
		qualifier.setAttribute("number", 456);
		person1.addQualifier(qualifier);
		ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
		cavs2.addGenericArgumentValue(MARK);
		RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
		AutowireCandidateQualifier qualifier2 = new AutowireCandidateQualifier(TestQualifierWithMultipleAttributes.class);
		qualifier2.setAttribute("number", 123);
		qualifier2.setAttribute("value", "default");
		person2.addQualifier(qualifier2);
		context.registerBeanDefinition(JUERGEN, person1);
		context.registerBeanDefinition(MARK, person2);
		context.registerBeanDefinition("autowired",
				new RootBeanDefinition(QualifiedFieldWithMultipleAttributesTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		context.refresh();
		QualifiedFieldWithMultipleAttributesTestBean bean =
				(QualifiedFieldWithMultipleAttributesTestBean) context.getBean("autowired");
		assertThat(bean.getPerson().getName()).isEqualTo(MARK);
	}

	@Test
	void autowiredFieldDoesNotResolveWithMultipleQualifierValuesAndMultipleMatchingCandidates() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
		cavs1.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
		AutowireCandidateQualifier qualifier = new AutowireCandidateQualifier(TestQualifierWithMultipleAttributes.class);
		qualifier.setAttribute("number", 123);
		person1.addQualifier(qualifier);
		ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
		cavs2.addGenericArgumentValue(MARK);
		RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
		AutowireCandidateQualifier qualifier2 = new AutowireCandidateQualifier(TestQualifierWithMultipleAttributes.class);
		qualifier2.setAttribute("number", 123);
		qualifier2.setAttribute("value", "default");
		person2.addQualifier(qualifier2);
		context.registerBeanDefinition(JUERGEN, person1);
		context.registerBeanDefinition(MARK, person2);
		context.registerBeanDefinition("autowired",
				new RootBeanDefinition(QualifiedFieldWithMultipleAttributesTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);

		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(context::refresh)
				.satisfies(ex -> {
					assertThat(ex.getRootCause()).isInstanceOf(NoSuchBeanDefinitionException.class);
					assertThat(ex.getBeanName()).isEqualTo("autowired");
				});
	}

	@Test
	void autowiredFieldResolvesWithBaseQualifierAndDefaultValue() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
		cavs1.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
		ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
		cavs2.addGenericArgumentValue(MARK);
		RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
		person2.addQualifier(new AutowireCandidateQualifier(Qualifier.class));
		context.registerBeanDefinition(JUERGEN, person1);
		context.registerBeanDefinition(MARK, person2);
		context.registerBeanDefinition("autowired",
				new RootBeanDefinition(QualifiedFieldWithBaseQualifierDefaultValueTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		context.refresh();
		QualifiedFieldWithBaseQualifierDefaultValueTestBean bean =
				(QualifiedFieldWithBaseQualifierDefaultValueTestBean) context.getBean("autowired");
		assertThat(bean.getPerson().getName()).isEqualTo(MARK);
	}

	@Test
	void autowiredFieldResolvesWithBaseQualifierAndNonDefaultValue() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
		cavs1.addGenericArgumentValue("the real juergen");
		RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
		person1.addQualifier(new AutowireCandidateQualifier(Qualifier.class, "juergen"));
		ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
		cavs2.addGenericArgumentValue("juergen imposter");
		RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
		person2.addQualifier(new AutowireCandidateQualifier(Qualifier.class, "not really juergen"));
		context.registerBeanDefinition("juergen1", person1);
		context.registerBeanDefinition("juergen2", person2);
		context.registerBeanDefinition("autowired",
				new RootBeanDefinition(QualifiedConstructorArgumentWithBaseQualifierNonDefaultValueTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		context.refresh();
		QualifiedConstructorArgumentWithBaseQualifierNonDefaultValueTestBean bean =
				(QualifiedConstructorArgumentWithBaseQualifierNonDefaultValueTestBean) context.getBean("autowired");
		assertThat(bean.getPerson().getName()).isEqualTo("the real juergen");
	}

	@Test
	void autowiredFieldDoesNotResolveWithBaseQualifierAndNonDefaultValueAndMultipleMatchingCandidates() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
		cavs1.addGenericArgumentValue("the real juergen");
		RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
		person1.addQualifier(new AutowireCandidateQualifier(Qualifier.class, "juergen"));
		ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
		cavs2.addGenericArgumentValue("juergen imposter");
		RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
		person2.addQualifier(new AutowireCandidateQualifier(Qualifier.class, "juergen"));
		context.registerBeanDefinition("juergen1", person1);
		context.registerBeanDefinition("juergen2", person2);
		context.registerBeanDefinition("autowired",
				new RootBeanDefinition(QualifiedConstructorArgumentWithBaseQualifierNonDefaultValueTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);

		assertThatExceptionOfType(UnsatisfiedDependencyException.class)
				.isThrownBy(context::refresh)
				.satisfies(ex -> assertThat(ex.getBeanName()).isEqualTo("autowired"));
	}


	private static class QualifiedFieldTestBean {

		@Autowired
		@TestQualifier
		private Person person;

		public Person getPerson() {
			return this.person;
		}
	}


	private static class MetaQualifiedFieldTestBean {

		@MyAutowired
		private Person person;

		public Person getPerson() {
			return this.person;
		}
	}


	@Autowired
	@TestQualifier
	@Retention(RetentionPolicy.RUNTIME)
	@interface MyAutowired {
	}


	private static class QualifiedMethodParameterTestBean {

		private Person person;

		@Autowired
		public void setPerson(@TestQualifier Person person) {
			this.person = person;
		}

		public Person getPerson() {
			return this.person;
		}
	}


	private static class QualifiedConstructorArgumentTestBean {

		private Person person;

		@Autowired
		public QualifiedConstructorArgumentTestBean(@TestQualifier Person person) {
			this.person = person;
		}

		public Person getPerson() {
			return this.person;
		}

	}


	private static class QualifiedFieldWithDefaultValueTestBean {

		@Autowired
		@TestQualifierWithDefaultValue
		private Person person;

		public Person getPerson() {
			return this.person;
		}
	}


	private static class QualifiedFieldWithMultipleAttributesTestBean {

		@Autowired
		@TestQualifierWithMultipleAttributes(number=123)
		private Person person;

		public Person getPerson() {
			return this.person;
		}
	}


	private static class QualifiedFieldWithBaseQualifierDefaultValueTestBean {

		@Autowired
		@Qualifier
		private Person person;

		public Person getPerson() {
			return this.person;
		}
	}


	private static class QualifiedConstructorArgumentWithBaseQualifierNonDefaultValueTestBean {

		private Person person;

		@Autowired
		public QualifiedConstructorArgumentWithBaseQualifierNonDefaultValueTestBean(
				@Qualifier("juergen") Person person) {
			this.person = person;
		}

		public Person getPerson() {
			return this.person;
		}
	}


	private static class Person {

		private String name;

		public Person(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}
	}


	@TestQualifier
	private static class QualifiedPerson extends Person {

		public QualifiedPerson() {
			super(null);
		}

		public QualifiedPerson(String name) {
			super(name);
		}
	}


	@TestQualifierWithDefaultValue
	private static class DefaultValueQualifiedPerson extends Person {

		public DefaultValueQualifiedPerson() {
			super(null);
		}

		public DefaultValueQualifiedPerson(String name) {
			super(name);
		}
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Qualifier
	@interface TestQualifier {
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Qualifier
	@interface TestQualifierWithDefaultValue {

		String value() default "default";
	}


	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@Qualifier
	@interface TestQualifierWithMultipleAttributes {

		String[] value() default "default";

		int number();
	}

}
