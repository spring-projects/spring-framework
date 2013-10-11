package org.springframework.web.method;

import org.junit.Test;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.junit.Assert.*;

/**
 * @author Brian Clozel
 */
public class ControllerAdviceBeanTests {

	@Test
	public void shouldMatchAll() {
		ControllerAdviceBean bean = new ControllerAdviceBean(new SimpleControllerAdvice());
		assertApplicable("should match all", bean, AnnotatedController.class);
		assertApplicable("should match all", bean, ImplementationController.class);
		assertApplicable("should match all", bean, InheritanceController.class);
		assertApplicable("should match all", bean, String.class);
	}

	@Test
	public void basePackageSupport() {
		ControllerAdviceBean bean = new ControllerAdviceBean(new BasePackageSupport());
		assertApplicable("base package support", bean, AnnotatedController.class);
		assertApplicable("base package support", bean, ImplementationController.class);
		assertApplicable("base package support", bean, InheritanceController.class);
		assertNotApplicable("bean not in package", bean, String.class);
	}

	@Test
	public void basePackageValueSupport() {
		ControllerAdviceBean bean = new ControllerAdviceBean(new BasePackageValueSupport());
		assertApplicable("base package support", bean, AnnotatedController.class);
		assertApplicable("base package support", bean, ImplementationController.class);
		assertApplicable("base package support", bean, InheritanceController.class);
		assertNotApplicable("bean not in package", bean, String.class);
	}

	@Test
	public void annotationSupport() {
		ControllerAdviceBean bean = new ControllerAdviceBean(new AnnotationSupport());
		assertApplicable("annotation support", bean, AnnotatedController.class);
		assertNotApplicable("this bean is not annotated", bean, InheritanceController.class);
	}

	@Test
	public void markerClassSupport() {
		ControllerAdviceBean bean = new ControllerAdviceBean(new MarkerClassSupport());
		assertApplicable("base package class support", bean, AnnotatedController.class);
		assertApplicable("base package class support", bean, ImplementationController.class);
		assertApplicable("base package class support", bean, InheritanceController.class);
		assertNotApplicable("bean not in package", bean, String.class);
	}

	@Test
	public void shouldNotMatch() {
		ControllerAdviceBean bean = new ControllerAdviceBean(new ShouldNotMatch());
		assertNotApplicable("should not match", bean, AnnotatedController.class);
		assertNotApplicable("should not match", bean, ImplementationController.class);
		assertNotApplicable("should not match", bean, InheritanceController.class);
		assertNotApplicable("should not match", bean, String.class);
	}

	@Test
	public void assignableTypesSupport() {
		ControllerAdviceBean bean = new ControllerAdviceBean(new AssignableTypesSupport());
		assertApplicable("controller implements assignable", bean, ImplementationController.class);
		assertApplicable("controller inherits assignable", bean, InheritanceController.class);
		assertNotApplicable("not assignable", bean, AnnotatedController.class);
		assertNotApplicable("not assignable", bean, String.class);
	}

	@Test
	public void multipleMatch() {
		ControllerAdviceBean bean = new ControllerAdviceBean(new MultipleSelectorsSupport());
		assertApplicable("controller implements assignable", bean, ImplementationController.class);
		assertApplicable("controller is annotated", bean, AnnotatedController.class);
		assertNotApplicable("should not match", bean, InheritanceController.class);
	}

	private void assertApplicable(String message, ControllerAdviceBean controllerAdvice,
	                              Class<?> controllerBeanType) {
		assertNotNull(controllerAdvice);
		assertTrue(message,controllerAdvice.isApplicableToBeanType(controllerBeanType));
	}

	private void assertNotApplicable(String message, ControllerAdviceBean controllerAdvice,
	                              Class<?> controllerBeanType) {
		assertNotNull(controllerAdvice);
		assertFalse(message,controllerAdvice.isApplicableToBeanType(controllerBeanType));
	}

	// ControllerAdvice classes

	@ControllerAdvice
	static class SimpleControllerAdvice {}

	@ControllerAdvice(annotations = ControllerAnnotation.class)
	static class AnnotationSupport {}

	@ControllerAdvice(basePackageClasses = MarkerClass.class)
	static class MarkerClassSupport {}

	@ControllerAdvice(assignableTypes = {ControllerInterface.class,
			AbstractController.class})
	static class AssignableTypesSupport {}

	@ControllerAdvice(basePackages = "org.springframework.web.method")
	static class BasePackageSupport {}

	@ControllerAdvice("org.springframework.web.method")
	static class BasePackageValueSupport {}

	@ControllerAdvice(annotations = ControllerAnnotation.class,
			assignableTypes = ControllerInterface.class)
	static class MultipleSelectorsSupport {}

	@ControllerAdvice(basePackages = "java.util",
			annotations = RestController.class)
	static class ShouldNotMatch {}

	// Support classes

	static class MarkerClass {}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface ControllerAnnotation {}

	@ControllerAnnotation
	public static class AnnotatedController {}

	static interface ControllerInterface {}

	static class ImplementationController implements ControllerInterface {}

	static abstract class AbstractController {}

	static class InheritanceController extends AbstractController {}
}
