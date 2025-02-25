package org.springframework.context.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotation to log the execution time of methods.
 *
 * When applied to a method, the execution time of that method will be logged.
 * Optionally, a custom logger method can be provided.
 *
 * Usage Example:
 * {@code
 * @Service
 * public class MyService {
 *     @LogExecutionTime
 *     public void someMethod() {
 *         // Method implementation
 *     }
 *
 *     public void customLogger(String message) {
 *         // Custom logging implementation
 *     }
 *
 *     @LogExecutionTime(logger = "customLogger")
 *     public void someMethodWithCustomLogger() {
 *         // Method implementation
 *     }
 * }}
 *
 * @author Sachin Sudhir Shinde
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogExecutionTime {
	String logger() default "";
}



