package org.springframework.beans.factory

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

/**
 * Mock object based tests for ListableBeanFactory Kotlin extensions
 *
 * @author Sebastien Deleuze
 */
@RunWith(MockitoJUnitRunner::class)
class ListenableBeanFactoryExtensionsTests {

    @Mock(answer = Answers.RETURNS_MOCKS)
    lateinit var lbf: ListableBeanFactory

    @Test
    fun `getBeanNamesForType with KClass`() {
        lbf.getBeanNamesForType(Foo::class)
        Mockito.verify(lbf, Mockito.times(1)).getBeanNamesForType(Foo::class.java, true , true)
    }

    @Test
    fun `getBeanNamesForType with KClass and Boolean`() {
        lbf.getBeanNamesForType(Foo::class, false)
        Mockito.verify(lbf, Mockito.times(1)).getBeanNamesForType(Foo::class.java, false , true)
    }

    @Test
    fun `getBeanNamesForType with KClass, Boolean and Boolean`() {
        lbf.getBeanNamesForType(Foo::class, false, false)
        Mockito.verify(lbf, Mockito.times(1)).getBeanNamesForType(Foo::class.java, false , false)
    }

    @Test
    fun `getBeanNamesForType with reified type parameters`() {
        lbf.getBeanNamesForType<Foo>()
        Mockito.verify(lbf, Mockito.times(1)).getBeanNamesForType(Foo::class.java, true , true)
    }

    @Test
    fun `getBeanNamesForType with reified type parameters and Boolean`() {
        lbf.getBeanNamesForType<Foo>(false)
        Mockito.verify(lbf, Mockito.times(1)).getBeanNamesForType(Foo::class.java, false , true)
    }

    @Test
    fun `getBeanNamesForType with reified type parameters, Boolean and Boolean`() {
        lbf.getBeanNamesForType<Foo>(false, false)
        Mockito.verify(lbf, Mockito.times(1)).getBeanNamesForType(Foo::class.java, false , false)
    }

    @Test
    fun `getBeansOfType with KClass`() {
        lbf.getBeansOfType(Foo::class)
        Mockito.verify(lbf, Mockito.times(1)).getBeansOfType(Foo::class.java, true , true)
    }

    @Test
    fun `getBeansOfType with KClass and Boolean`() {
        lbf.getBeansOfType(Foo::class, false)
        Mockito.verify(lbf, Mockito.times(1)).getBeansOfType(Foo::class.java, false , true)
    }

    @Test
    fun `getBeansOfType with KClass, Boolean and Boolean`() {
        lbf.getBeansOfType(Foo::class, false, false)
        Mockito.verify(lbf, Mockito.times(1)).getBeansOfType(Foo::class.java, false , false)
    }

    @Test
    fun `getBeansOfType with reified type parameters`() {
        lbf.getBeansOfType<Foo>()
        Mockito.verify(lbf, Mockito.times(1)).getBeansOfType(Foo::class.java, true , true)
    }

    @Test
    fun `getBeansOfType with reified type parameters and Boolean`() {
        lbf.getBeansOfType<Foo>(false)
        Mockito.verify(lbf, Mockito.times(1)).getBeansOfType(Foo::class.java, false , true)
    }

    @Test
    fun `getBeansOfType with reified type parameters, Boolean and Boolean`() {
        lbf.getBeansOfType<Foo>(false, false)
        Mockito.verify(lbf, Mockito.times(1)).getBeansOfType(Foo::class.java, false , false)
    }

    @Test
    fun `getBeanNamesForAnnotation with KClass`() {
        lbf.getBeanNamesForAnnotation(Bar::class)
        Mockito.verify(lbf, Mockito.times(1)).getBeanNamesForAnnotation(Bar::class.java)
    }

    @Test
    fun `getBeanNamesForAnnotation with reified type parameters`() {
        lbf.getBeanNamesForAnnotation<Bar>()
        Mockito.verify(lbf, Mockito.times(1)).getBeanNamesForAnnotation(Bar::class.java)
    }

    @Test
    fun `getBeansWithAnnotation with KClass`() {
        lbf.getBeansWithAnnotation(Bar::class)
        Mockito.verify(lbf, Mockito.times(1)).getBeansWithAnnotation(Bar::class.java)
    }

    @Test
    fun `getBeansWithAnnotation with reified type parameters`() {
        lbf.getBeansWithAnnotation<Bar>()
        Mockito.verify(lbf, Mockito.times(1)).getBeansWithAnnotation(Bar::class.java)
    }

    @Test
    fun `findAnnotationOnBean with String and KClass`() {
        val name = "bar"
        lbf.findAnnotationOnBean(name, Bar::class)
        Mockito.verify(lbf, Mockito.times(1)).findAnnotationOnBean(name, Bar::class.java)
    }

    @Test
    fun `findAnnotationOnBean with String and reified type parameters`() {
        val name = "bar"
        lbf.findAnnotationOnBean<Bar>(name)
        Mockito.verify(lbf, Mockito.times(1)).findAnnotationOnBean(name, Bar::class.java)
    }

    class Foo

    annotation class Bar
}
