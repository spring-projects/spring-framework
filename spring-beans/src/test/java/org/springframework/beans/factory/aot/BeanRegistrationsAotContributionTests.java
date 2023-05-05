/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.beans.factory.aot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.generate.MethodReference.ArgumentCodeGenerator;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.aot.BeanRegistrationsAotContribution.Registration;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.GenericBeanWithBounds;
import org.springframework.beans.testfixture.beans.Person;
import org.springframework.beans.testfixture.beans.RecordBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.beans.testfixture.beans.factory.aot.MockBeanFactoryInitializationCode;
import org.springframework.core.test.io.support.MockSpringFactoriesLoader;
import org.springframework.core.test.tools.Compiled;
import org.springframework.core.test.tools.SourceFile;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.reflection;

/**
 * Tests for {@link BeanRegistrationsAotContribution}.
 *
 * @author Phillip Webb
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Brian Clozel
 */
class BeanRegistrationsAotContributionTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private final BeanDefinitionMethodGeneratorFactory methodGeneratorFactory = new BeanDefinitionMethodGeneratorFactory(
			AotServices.factoriesAndBeans(new MockSpringFactoriesLoader(), this.beanFactory));

	private TestGenerationContext generationContext = new TestGenerationContext();

	private MockBeanFactoryInitializationCode beanFactoryInitializationCode = new MockBeanFactoryInitializationCode(this.generationContext);


	@Test
	void applyToAppliesContribution() {
		RegisteredBean registeredBean = registerBean(new RootBeanDefinition(TestBean.class));
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(this.methodGeneratorFactory,
				registeredBean, null, List.of());
		BeanRegistrationsAotContribution contribution = createContribution(TestBean.class, generator);
		contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
		compile((consumer, compiled) -> {
			DefaultListableBeanFactory freshBeanFactory = new DefaultListableBeanFactory();
			consumer.accept(freshBeanFactory);
			assertThat(freshBeanFactory.getBean(TestBean.class)).isNotNull();
		});
	}

	@Test
	void applyToAppliesContributionWithAliases() {
		RegisteredBean registeredBean = registerBean(new RootBeanDefinition(TestBean.class));
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(this.methodGeneratorFactory,
				registeredBean, null, List.of());
		BeanRegistrationsAotContribution contribution = createContribution(TestBean.class, generator, "testAlias");
		contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
		compile((consumer, compiled) -> {
			DefaultListableBeanFactory freshBeanFactory = new DefaultListableBeanFactory();
			consumer.accept(freshBeanFactory);
			assertThat(freshBeanFactory.getAliases("testBean")).containsExactly("testAlias");
		});
	}

	@Test
	void applyToWhenHasNameGeneratesPrefixedFeatureName() {
		this.generationContext = new TestGenerationContext(
				new ClassNameGenerator(TestGenerationContext.TEST_TARGET, "Management"));
		this.beanFactoryInitializationCode = new MockBeanFactoryInitializationCode(this.generationContext);
		RegisteredBean registeredBean = registerBean(new RootBeanDefinition(TestBean.class));
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(this.methodGeneratorFactory,
				registeredBean, null, List.of());
		BeanRegistrationsAotContribution contribution = createContribution(TestBean.class, generator);
		contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
		compile((consumer, compiled) -> {
			SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
			assertThat(sourceFile.getClassName()).endsWith("__ManagementBeanDefinitions");
		});
	}

	@Test
	void applyToCallsRegistrationsWithBeanRegistrationsCode() {
		List<BeanRegistrationsCode> beanRegistrationsCodes = new ArrayList<>();
		RegisteredBean registeredBean = registerBean(new RootBeanDefinition(TestBean.class));
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(this.methodGeneratorFactory,
				registeredBean, null, List.of()) {

			@Override
			MethodReference generateBeanDefinitionMethod(GenerationContext generationContext,
					BeanRegistrationsCode beanRegistrationsCode) {
				beanRegistrationsCodes.add(beanRegistrationsCode);
				return super.generateBeanDefinitionMethod(generationContext, beanRegistrationsCode);
			}

		};
		BeanRegistrationsAotContribution contribution = createContribution(TestBean.class, generator);
		contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
		assertThat(beanRegistrationsCodes).hasSize(1);
		BeanRegistrationsCode actual = beanRegistrationsCodes.get(0);
		assertThat(actual.getMethods()).isNotNull();
	}

	@Test
	void applyToRegisterReflectionHints() {
		RegisteredBean registeredBean = registerBean(new RootBeanDefinition(TestBean.class));
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(this.methodGeneratorFactory,
				registeredBean, null, List.of());
		BeanRegistrationsAotContribution contribution = createContribution(TestBean.class, generator);
		contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
		assertThat(reflection().onType(TestBean.class)
				.withMemberCategory(MemberCategory.INTROSPECT_DECLARED_METHODS))
				.accepts(this.generationContext.getRuntimeHints());
	}

	@Test
	void applyToRegisterReflectionHintsOnRecordBean() {
		RegisteredBean registeredBean = registerBean(new RootBeanDefinition(RecordBean.class));
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(this.methodGeneratorFactory,
				registeredBean, null, List.of());
		BeanRegistrationsAotContribution contribution = createContribution(RecordBean.class, generator);
		contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
		assertThat(reflection().onType(RecordBean.class)
				.withMemberCategories(MemberCategory.INTROSPECT_DECLARED_METHODS, MemberCategory.INVOKE_DECLARED_METHODS))
				.accepts(this.generationContext.getRuntimeHints());
	}

	@Test
	void applyToRegisterReflectionHintsOnGenericBeanWithBounds() {
		RegisteredBean registeredBean = registerBean(new RootBeanDefinition(GenericBeanWithBounds.class));
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(this.methodGeneratorFactory,
				registeredBean, null, List.of());
		BeanRegistrationsAotContribution contribution = createContribution(GenericBeanWithBounds.class, generator);
		contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
		assertThat(reflection().onType(Person[].class)).accepts(this.generationContext.getRuntimeHints());
	}

	private RegisteredBean registerBean(RootBeanDefinition rootBeanDefinition) {
		String beanName = "testBean";
		this.beanFactory.registerBeanDefinition(beanName, rootBeanDefinition);
		return RegisteredBean.of(this.beanFactory, beanName);
	}

	@SuppressWarnings({ "unchecked", "cast" })
	private void compile(BiConsumer<Consumer<DefaultListableBeanFactory>, Compiled> result) {
		MethodReference beanRegistrationsMethodReference = this.beanFactoryInitializationCode.getInitializers().get(0);
		MethodReference aliasesMethodReference = this.beanFactoryInitializationCode.getInitializers().get(1);
		this.beanFactoryInitializationCode.getTypeBuilder().set(type -> {
			ArgumentCodeGenerator beanFactory = ArgumentCodeGenerator.of(DefaultListableBeanFactory.class, "beanFactory");
			ClassName className = this.beanFactoryInitializationCode.getClassName();
			CodeBlock beanRegistrationsMethodInvocation = beanRegistrationsMethodReference.toInvokeCodeBlock(beanFactory, className);
			CodeBlock aliasesMethodInvocation = aliasesMethodReference.toInvokeCodeBlock(beanFactory, className);
			type.addModifiers(Modifier.PUBLIC);
			type.addSuperinterface(ParameterizedTypeName.get(Consumer.class, DefaultListableBeanFactory.class));
			type.addMethod(MethodSpec.methodBuilder("accept")
					.addModifiers(Modifier.PUBLIC)
					.addParameter(DefaultListableBeanFactory.class, "beanFactory")
					.addStatement(beanRegistrationsMethodInvocation)
					.addStatement(aliasesMethodInvocation)
					.build());
		});
		this.generationContext.writeGeneratedContent();
		TestCompiler.forSystem().with(this.generationContext).compile(compiled ->
				result.accept(compiled.getInstance(Consumer.class), compiled));
	}

	private BeanRegistrationsAotContribution createContribution(Class<?> beanClass,
			BeanDefinitionMethodGenerator methodGenerator,String... aliases) {
		return new BeanRegistrationsAotContribution(
			Map.of(new BeanRegistrationKey("testBean", beanClass), new Registration(methodGenerator, aliases)));
	}

}
