package org.springframework.context.aspect;

import java.lang.reflect.Field;
import java.util.logging.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.LogExecutionTime;


/**
 * Aspect to handle the logging of method execution times.
 *
 * This aspect intercepts method calls, checks for the @LogExecutionTime annotation,
 * and logs the execution time of the annotated methods.
 *
 * Usage Example:
 * {@code
 * @Service
 * public class MyService {
 *     @LogExecutionTime
 *     public void someMethod() {
 *         // Method implementation
 *     }
 * }}
 *
 * @author Sachin Sudhir Shinde
 */
@Aspect
@Configuration
public class LogExecutionTimeAspect {

	@Around("@annotation(logExecutionTime)")
	public Object logExecutionTime(ProceedingJoinPoint joinPoint, LogExecutionTime logExecutionTime) throws Throwable {
		if(logExecutionTime == null){
			return joinPoint.proceed();
		}
		long start = System.currentTimeMillis();
		Object proceed = joinPoint.proceed();
		long executionTime = System.currentTimeMillis() - start;

		Field loggerField = getLoggerField(joinPoint, logExecutionTime);
		if(loggerField == null) {
			System.out.println(joinPoint.getSignature() + " executed in " + executionTime + "ms");
		} else {
			Object logger = loggerField.get(joinPoint.getTarget());
			if (logger instanceof Logger) {
				((Logger) logger).info(String.format("%s executed in %s ms", joinPoint.getSignature(), executionTime));
			}
		}
		return proceed;
	}

	private Field getLoggerField(ProceedingJoinPoint joinPoint, LogExecutionTime logExecutionTime) {
		if(!logExecutionTime.logger().isEmpty()) {
			try {
				Field loggerField = joinPoint.getTarget().getClass().getDeclaredField(logExecutionTime.logger());
				loggerField.setAccessible(true);
				return loggerField;
			} catch (NoSuchFieldException | SecurityException e) {
				// Field not found, proceed without custom logger
			}
		}
		return null;
	}
}
