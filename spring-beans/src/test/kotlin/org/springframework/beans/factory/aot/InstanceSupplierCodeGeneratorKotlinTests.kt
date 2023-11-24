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

package org.springframework.beans.factory.aot

import org.assertj.core.api.Assertions
import org.assertj.core.api.ThrowingConsumer
import org.junit.jupiter.api.Test
import org.springframework.aot.hint.*
import org.springframework.aot.test.generate.TestGenerationContext
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.beans.factory.support.InstanceSupplier
import org.springframework.beans.factory.support.RegisteredBean
import org.springframework.beans.factory.support.RootBeanDefinition
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
        val constructorOrFactoryMethod = registeredBean.resolveConstructorOrFactoryMethod()
        Assertions.assertThat(constructorOrFactoryMethod).isNotNull()
        val generatedCode = generator.generateCode(registeredBean, constructorOrFactoryMethod)
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
