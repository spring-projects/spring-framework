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

package org.springframework.beans.factory.aot

import org.assertj.core.api.Assertions
import org.assertj.core.api.ThrowingConsumer
import org.junit.jupiter.api.Test
import org.springframework.aot.hint.*
import org.springframework.aot.test.generate.TestGenerationContext
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.support.*
import org.springframework.beans.testfixture.beans.KotlinConfiguration
import org.springframework.beans.testfixture.beans.KotlinTestBean
import org.springframework.beans.testfixture.beans.KotlinTestBeanWithOptionalParameter
import org.springframework.beans.testfixture.beans.factory.aot.DeferredTypeBuilder
import org.springframework.core.test.tools.Compiled
import org.springframework.core.test.tools.TestCompiler
import org.springframework.javapoet.MethodSpec
import org.springframework.javapoet.ParameterizedTypeName
import org.springframework.javapoet.TypeSpec
import java.util.function.BiConsumer
import java.util.function.Supplier
import javax.lang.model.element.Modifier

/**
 * Kotlin tests for [InstanceSupplierCodeGenerator].
 *
 * @author Sebastien Deleuze
 */
class InstanceSupplierCodeGeneratorKotlinTests {

    private val generationContext = TestGenerationContext()

	private val beanFactory = DefaultListableBeanFactory()

    @Test
    fun generateWhenHasDefaultConstructor() {
        val beanDefinition: BeanDefinition = RootBeanDefinition(KotlinTestBean::class.java)
        val beanFactory = DefaultListableBeanFactory()
        compile(beanFactory, beanDefinition) { instanceSupplier, compiled ->
            val bean = getBean<KotlinTestBean>(beanFactory, beanDefinition, instanceSupplier)
            Assertions.assertThat(bean).isInstanceOf(KotlinTestBean::class.java)
            Assertions.assertThat(compiled.sourceFile).contains("InstanceSupplier.using(KotlinTestBean::new)")
        }
        Assertions.assertThat(getReflectionHints().getTypeHint(KotlinTestBean::class.java))
            .satisfies(hasConstructorWithMode(ExecutableMode.INTROSPECT))
    }

    @Test
    fun generateWhenConstructorHasOptionalParameter() {
        val beanDefinition: BeanDefinition = RootBeanDefinition(KotlinTestBeanWithOptionalParameter::class.java)
        val beanFactory = DefaultListableBeanFactory()
        compile(beanFactory, beanDefinition) { instanceSupplier, compiled ->
                val bean: KotlinTestBeanWithOptionalParameter = getBean(beanFactory, beanDefinition, instanceSupplier)
                Assertions.assertThat(bean).isInstanceOf(KotlinTestBeanWithOptionalParameter::class.java)
                Assertions.assertThat(compiled.sourceFile)
                    .contains("return BeanInstanceSupplier.<KotlinTestBeanWithOptionalParameter>forConstructor();")
            }
        Assertions.assertThat<TypeHint>(getReflectionHints().getTypeHint(KotlinTestBeanWithOptionalParameter::class.java))
            .satisfies(hasMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS))
    }

	@Test
	fun generateWhenHasFactoryMethodWithNoArg() {
		val beanDefinition = BeanDefinitionBuilder
			.rootBeanDefinition(String::class.java)
			.setFactoryMethodOnBean("stringBean", "config").beanDefinition
		this.beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
				.genericBeanDefinition(KotlinConfiguration::class.java).beanDefinition
		)
		compile(beanFactory, beanDefinition) { instanceSupplier, compiled ->
			val bean = getBean<String>(beanFactory, beanDefinition, instanceSupplier)
			Assertions.assertThat(bean).isInstanceOf(String::class.java)
			Assertions.assertThat(bean).isEqualTo("Hello")
			Assertions.assertThat(compiled.sourceFile).contains(
				"getBeanFactory().getBean(KotlinConfiguration.class).stringBean()"
			)
		}
		Assertions.assertThat<TypeHint?>(getReflectionHints().getTypeHint(KotlinConfiguration::class.java))
			.satisfies(hasMethodWithMode(ExecutableMode.INTROSPECT))
	}

	@Test
	fun generateWhenHasSuspendingFactoryMethod() {
		val beanDefinition = BeanDefinitionBuilder
			.rootBeanDefinition(String::class.java)
			.setFactoryMethodOnBean("suspendingStringBean", "config").beanDefinition
		this.beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
				.genericBeanDefinition(KotlinConfiguration::class.java).beanDefinition
		)
		Assertions.assertThatExceptionOfType(AotBeanProcessingException::class.java).isThrownBy {
			compile(beanFactory, beanDefinition) { _, _  -> }
		}
	}

    private fun getReflectionHints(): ReflectionHints {
        return generationContext.runtimeHints.reflection()
    }

    private fun hasConstructorWithMode(mode: ExecutableMode): ThrowingConsumer<TypeHint> {
        return ThrowingConsumer {
            Assertions.assertThat(it.constructors()).anySatisfy(hasMode(mode))
        }
    }

    private fun hasMemberCategory(category: MemberCategory): ThrowingConsumer<TypeHint> {
        return ThrowingConsumer {
            Assertions.assertThat(it.memberCategories).contains(category)
        }
    }

    private fun hasMode(mode: ExecutableMode): ThrowingConsumer<ExecutableHint> {
        return ThrowingConsumer {
            Assertions.assertThat(it.mode).isEqualTo(mode)
        }
    }

	private fun hasMethodWithMode(mode: ExecutableMode): ThrowingConsumer<TypeHint> {
		return ThrowingConsumer { hint: TypeHint ->
			Assertions.assertThat(hint.methods()).anySatisfy(hasMode(mode))
		}
	}

    @Suppress("UNCHECKED_CAST")
    private fun <T> getBean(beanFactory: DefaultListableBeanFactory, beanDefinition: BeanDefinition,
                            instanceSupplier: InstanceSupplier<*>): T {
        (beanDefinition as RootBeanDefinition).instanceSupplier = instanceSupplier
        beanFactory.registerBeanDefinition("testBean", beanDefinition)
        return beanFactory.getBean("testBean") as T
    }

    private fun compile(beanFactory: DefaultListableBeanFactory, beanDefinition: BeanDefinition,
                        result: BiConsumer<InstanceSupplier<*>, Compiled>) {

        val freshBeanFactory = DefaultListableBeanFactory(beanFactory)
        freshBeanFactory.registerBeanDefinition("testBean", beanDefinition)
        val registeredBean = RegisteredBean.of(freshBeanFactory, "testBean")
        val typeBuilder = DeferredTypeBuilder()
        val generateClass = generationContext.generatedClasses.addForFeature("TestCode", typeBuilder)
        val generator = InstanceSupplierCodeGenerator(
            generationContext, generateClass.name,
            generateClass.methods, false
        )
        val instantiationDescriptor = registeredBean.resolveInstantiationDescriptor()
        Assertions.assertThat(instantiationDescriptor).isNotNull()
        val generatedCode = generator.generateCode(registeredBean, instantiationDescriptor)
        typeBuilder.set { type: TypeSpec.Builder ->
            type.addModifiers(Modifier.PUBLIC)
            type.addSuperinterface(
                ParameterizedTypeName.get(
                    Supplier::class.java,
                    InstanceSupplier::class.java
                )
            )
            type.addMethod(
                MethodSpec.methodBuilder("get")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(InstanceSupplier::class.java)
                    .addStatement("return \$L", generatedCode).build()
            )
        }
        generationContext.writeGeneratedContent()
        TestCompiler.forSystem().with(generationContext).compile {
            result.accept(it.getInstance(Supplier::class.java).get() as InstanceSupplier<*>, it)
        }
    }

}
