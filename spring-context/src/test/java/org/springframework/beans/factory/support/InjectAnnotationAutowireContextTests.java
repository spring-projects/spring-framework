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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Qualifier;
import org.junit.jupiter.api.Test;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for handling JSR-330 {@link jakarta.inject.Qualifier} and
 * {@link javax.inject.Qualifier} annotations.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
class InjectAnnotationAutowireContextTests {

	private static final String PERSON1 = "person1";

	private static final String PERSON2 = "person2";

	private static final String JUERGEN = "juergen";

	private static final String MARK = "mark";


	@Test
	void autowiredFieldWithSingleNonQualifiedCandidate() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addGenericArgumentValue(JUERGEN);
		RootBeanDefinition person = new RootBeanDefinition(Person.class, cavs, null);
		context.registerBeanDefinition(PERSON1, person);
		context.registerBeanDefinition("autowired", new RootBeanDefinition(QualifiedFieldTestBean.class));
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
		context.registerBeanDefinition(PERSON1, person);
		context.registerBeanDefinition("autowired", new RootBeanDefinition(QualifiedMethodParameterTestBean.class));
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
		context.registerBeanDefinition(PERSON1, person);
		context.registerBeanDefinition("autowired", new RootBeanDefinition(QualifiedConstructorArgumentTestBean.class));
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
		context.registerBeanDefinition(PERSON1, person);
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
		context.registerBeanDefinition(PERSON1, person);
		context.registerBeanDefinition("autowired", new RootBeanDefinition(QualifiedMethodParameterTestBean.class));
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
		context.registerBeanDefinition(PERSON1,
				ScopedProxyUtils.createScopedProxy(new BeanDefinitionHolder(person, JUERGEN), context, true).getBeanDefinition());
		context.registerBeanDefinition("autowired", new RootBeanDefinition(QualifiedMethodParameterTestBean.class));
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
		RootBeanDefinition person1 = new RootBeanDefinition(QualifiedPerson.class, cavs, null);
		ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
		cavs2.addGenericArgumentValue(MARK);
		RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
		context.registerBeanDefinition(PERSON1, person1);
		context.registerBeanDefinition(PERSON2, person2);
		context.registerBeanDefinition("autowired", new RootBeanDefinition(QualifiedMethodParameterTestBean.class));
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
		context.registerBeanDefinition(PERSON1, person);
		context.registerBeanDefinition("autowired", new RootBeanDefinition(QualifiedConstructorArgumentTestBean.class));
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
		context.registerBeanDefinition(PERSON1, person1);
		context.registerBeanDefinition(PERSON2, person2);
		context.registerBeanDefinition("autowired", new RootBeanDefinition(QualifiedFieldTestBean.class));
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
		context.registerBeanDefinition(PERSON1, person1);
		context.registerBeanDefinition(PERSON2, person2);
		context.registerBeanDefinition("autowired", new RootBeanDefinition(QualifiedMethodParameterTestBean.class));
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
		context.registerBeanDefinition(PERSON1, person1);
		context.registerBeanDefinition(PERSON2, person2);
		context.registerBeanDefinition("autowired", new RootBeanDefinition(QualifiedConstructorArgumentTestBean.class));
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
		context.registerBeanDefinition(PERSON1, person1);
		context.registerBeanDefinition(PERSON2, person2);
		context.registerBeanDefinition("autowired", new RootBeanDefinition(QualifiedFieldTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		context.refresh();
		QualifiedFieldTestBean bean = (QualifiedFieldTestBean) context.getBean("autowired");
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
		context.registerBeanDefinition(PERSON1, person1);
		context.registerBeanDefinition(PERSON2, person2);
		context.registerBeanDefinition("autowired", new RootBeanDefinition(QualifiedMethodParameterTestBean.class));
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
		context.registerBeanDefinition(PERSON1, person1);
		context.registerBeanDefinition(PERSON2, person2);
		context.registerBeanDefinition("autowired", new RootBeanDefinition(QualifiedConstructorArgumentTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		context.refresh();
		QualifiedConstructorArgumentTestBean bean =
				(QualifiedConstructorArgumentTestBean) context.getBean("autowired");
		assertThat(bean.getPerson().getName()).isEqualTo(JUERGEN);
	}

	@Test  // gh-33345
	void autowiredConstructorArgumentResolvesJakartaNamedCandidate() {
		Class<JakartaNamedConstructorArgumentTestBean> testBeanClass = JakartaNamedConstructorArgumentTestBean.class;
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(testBeanClass, JakartaCat.class, JakartaDog.class);
		JakartaNamedConstructorArgumentTestBean bean = context.getBean(testBeanClass);
		assertThat(bean.getAnimal1().getName()).isEqualTo("Jakarta Tiger");
		assertThat(bean.getAnimal2().getName()).isEqualTo("Jakarta Fido");
	}

	@Test  // gh-33345
	void autowiredConstructorArgumentResolvesJavaxNamedCandidate() {
		Class<JavaxNamedConstructorArgumentTestBean> testBeanClass = JavaxNamedConstructorArgumentTestBean.class;
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(testBeanClass, JavaxCat.class, JavaxDog.class);
		JavaxNamedConstructorArgumentTestBean bean = context.getBean(testBeanClass);
		assertThat(bean.getAnimal1().getName()).isEqualTo("Javax Tiger");
		assertThat(bean.getAnimal2().getName()).isEqualTo("Javax Fido");
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
		context.registerBeanDefinition(PERSON1, person1);
		context.registerBeanDefinition(PERSON2, person2);
		context.registerBeanDefinition("autowired", new RootBeanDefinition(QualifiedFieldWithDefaultValueTestBean.class));
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
		context.registerBeanDefinition(PERSON1, person1);
		context.registerBeanDefinition(PERSON2, person2);
		context.registerBeanDefinition("autowired", new RootBeanDefinition(QualifiedFieldWithDefaultValueTestBean.class));
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
		context.registerBeanDefinition(PERSON1, person1);
		context.registerBeanDefinition(PERSON2, person2);
		context.registerBeanDefinition("autowired", new RootBeanDefinition(QualifiedFieldWithDefaultValueTestBean.class));
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
		context.registerBeanDefinition(PERSON1, person1);
		context.registerBeanDefinition(PERSON2, person2);
		context.registerBeanDefinition("autowired", new RootBeanDefinition(QualifiedFieldWithMultipleAttributesTestBean.class));
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
		context.registerBeanDefinition(PERSON1, person1);
		context.registerBeanDefinition(PERSON2, person2);
		context.registerBeanDefinition("autowired", new RootBeanDefinition(QualifiedFieldWithMultipleAttributesTestBean.class));
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
		context.registerBeanDefinition(PERSON1, person1);
		context.registerBeanDefinition(PERSON2, person2);
		context.registerBeanDefinition("autowired", new RootBeanDefinition(QualifiedFieldWithMultipleAttributesTestBean.class));
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
		context.registerBeanDefinition(PERSON1, person1);
		context.registerBeanDefinition(PERSON2, person2);
		context.registerBeanDefinition("autowired", new RootBeanDefinition(QualifiedFieldWithMultipleAttributesTestBean.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(context::refresh)
				.satisfies(ex -> {
					assertThat(ex.getRootCause()).isInstanceOf(NoSuchBeanDefinitionException.class);
					assertThat(ex.getBeanName()).isEqualTo("autowired");
				});
	}

	@Test
	void autowiredConstructorArgumentDoesNotResolveWithBaseQualifierAndNonDefaultValueAndMultipleMatchingCandidates() {
		GenericApplicationContext context = new GenericApplicationContext();
		ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
		cavs1.addGenericArgumentValue("the real juergen");
		RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
		person1.addQualifier(new AutowireCandidateQualifier(Qualifier.class, JUERGEN));
		ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
		cavs2.addGenericArgumentValue("juergen imposter");
		RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
		person2.addQualifier(new AutowireCandidateQualifier(Qualifier.class, JUERGEN));
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

		@Inject
		@TestQualifier
		private Person person;

		public Person getPerson() {
			return this.person;
		}
	}


	private static class QualifiedMethodParameterTestBean {

		private Person person;

		@Inject
		public void setPerson(@TestQualifier Person person) {
			this.person = person;
		}

		public Person getPerson() {
			return this.person;
		}
	}


	private static class QualifiedConstructorArgumentTestBean {

		private final Person person;

		@Inject
		public QualifiedConstructorArgumentTestBean(@TestQualifier Person person) {
			this.person = person;
		}

		public Person getPerson() {
			return this.person;
		}

	}


	static class JakartaNamedConstructorArgumentTestBean {

		private final Animal animal1;
		private final Animal animal2;

		@jakarta.inject.Inject
		public JakartaNamedConstructorArgumentTestBean(@jakarta.inject.Named("Cat") Animal animal1,
				@jakarta.inject.Named("Dog") Animal animal2) {

			this.animal1 = animal1;
			this.animal2 = animal2;
		}

		public Animal getAnimal1() {
			return this.animal1;
		}

		public Animal getAnimal2() {
			return this.animal2;
		}
	}


	static class JavaxNamedConstructorArgumentTestBean {

		private final Animal animal1;
		private final Animal animal2;

		@javax.inject.Inject
		public JavaxNamedConstructorArgumentTestBean(@javax.inject.Named("Cat") Animal animal1,
				@javax.inject.Named("Dog") Animal animal2) {

			this.animal1 = animal1;
			this.animal2 = animal2;
		}

		public Animal getAnimal1() {
			return this.animal1;
		}

		public Animal getAnimal2() {
			return this.animal2;
		}
	}


	public static class QualifiedFieldWithDefaultValueTestBean {

		@Inject
		@TestQualifierWithDefaultValue
		private Person person;

		public Person getPerson() {
			return this.person;
		}
	}


	public static class QualifiedFieldWithMultipleAttributesTestBean {

		@Inject
		@TestQualifierWithMultipleAttributes(number=123)
		private Person person;

		public Person getPerson() {
			return this.person;
		}
	}


	@SuppressWarnings("unused")
	private static class QualifiedFieldWithBaseQualifierDefaultValueTestBean {

		@Inject
		private Person person;

		public Person getPerson() {
			return this.person;
		}
	}


	static class QualifiedConstructorArgumentWithBaseQualifierNonDefaultValueTestBean {

		private Person person;

		@Inject
		public QualifiedConstructorArgumentWithBaseQualifierNonDefaultValueTestBean(
				@Named(JUERGEN) Person person) {
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


	interface Animal {

		String getName();
	}


	@jakarta.inject.Named("Cat")
	static class JakartaCat implements Animal {

		@Override
		public String getName() {
			return "Jakarta Tiger";
		}
	}


	@javax.inject.Named("Cat")
	static class JavaxCat implements Animal {

		@Override
		public String getName() {
			return "Javax Tiger";
		}
	}


	@jakarta.inject.Named("Dog")
	static class JakartaDog implements Animal {

		@Override
		public String getName() {
			return "Jakarta Fido";
		}
	}


	@javax.inject.Named("Dog")
	static class JavaxDog implements Animal {

		@Override
		public String getName() {
			return "Javax Fido";
		}
	}


	@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@Qualifier
	public @interface TestQualifier {
	}


	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@Qualifier
	public @interface TestQualifierWithDefaultValue {

		String value() default "default";
	}


	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@Qualifier
	public @interface TestQualifierWithMultipleAttributes {

		String value() default "default";

		int number();
	}

}
