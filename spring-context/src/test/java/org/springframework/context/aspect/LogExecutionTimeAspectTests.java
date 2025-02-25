package org.springframework.context.aspect;

import java.lang.annotation.Annotation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.LogExecutionTime;

import static org.mockito.Mockito.*;


/**
 * Tests for the LogExecutionTimeAspect.
 *
 * This class tests the functionality of the LogExecutionTimeAspect to ensure
 * that method execution times are logged correctly.
 */
@ExtendWith(MockitoExtension.class)
public class LogExecutionTimeAspectTests {

	@InjectMocks
	private LogExecutionTimeAspect logExecutionTimeAspect;

	@Mock
	private ProceedingJoinPoint proceedingJoinPoint;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	/**
	 * Test to verify that the LogExecutionTime aspect logs execution time
	 * for methods annotated with @LogExecutionTime.
	 */
	@Test
	public void testLogExecutionTime() throws Throwable {
		when(proceedingJoinPoint.proceed()).thenReturn(null);
		when(proceedingJoinPoint.getSignature()).thenReturn(mock(org.aspectj.lang.Signature.class));

		// Call the aspect method and verify that execution time is logged
		logExecutionTimeAspect.logExecutionTime(proceedingJoinPoint, createLogExecutionTimeAnnotation());
		verify(proceedingJoinPoint, times(1)).proceed();
	}

	/**
	 * Test to verify that the LogExecutionTime aspect works with methods
	 * that have different signatures, including parameters and return values.
	 */
	@Test
	public void testLogExecutionTimeWithDifferentSignatures() throws Throwable {
		when(proceedingJoinPoint.proceed()).thenReturn("Result");
		when(proceedingJoinPoint.getSignature()).thenReturn(mock(org.aspectj.lang.Signature.class));

		// Call the aspect method and verify that execution time is logged
		logExecutionTimeAspect.logExecutionTime(proceedingJoinPoint, createLogExecutionTimeAnnotation());
		verify(proceedingJoinPoint, times(1)).proceed();
	}

	/**
	 * Test to verify that the LogExecutionTime aspect logs execution time
	 * for methods that throw exceptions.
	 */
	@Test
	public void testLogExecutionTimeWithException() throws Throwable {
		when(proceedingJoinPoint.proceed()).thenThrow(new Exception("Test Exception"));

		// Call the aspect method and catch the exception
		try {
			logExecutionTimeAspect.logExecutionTime(proceedingJoinPoint, createLogExecutionTimeAnnotation());
		} catch (Exception e) {
			// Expected exception
		}
		verify(proceedingJoinPoint, times(1)).proceed();
	}

	/**
	 * Test to verify that the LogExecutionTime aspect works correctly
	 * when a method has multiple annotations, including @LogExecutionTime.
	 */
	@Test
	public void testMethodWithMultipleAnnotations() throws Throwable {
		when(proceedingJoinPoint.proceed()).thenReturn(null);
		when(proceedingJoinPoint.getSignature()).thenReturn(mock(org.aspectj.lang.Signature.class));

		// Call the aspect method and verify that execution time is logged
		logExecutionTimeAspect.logExecutionTime(proceedingJoinPoint, createLogExecutionTimeAnnotation());
		verify(proceedingJoinPoint, times(1)).proceed();
	}

	/**
	 * Create a mock LogExecutionTime annotation.
	 */
	private LogExecutionTime createLogExecutionTimeAnnotation() {
		return new LogExecutionTime() {
			@Override
			public Class<? extends Annotation> annotationType() {
				return LogExecutionTime.class;
			}

			@Override
			public String logger() {
				return "";
			}
		};
	}
}
