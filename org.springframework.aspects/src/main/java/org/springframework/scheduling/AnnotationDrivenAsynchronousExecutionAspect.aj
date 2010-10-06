package org.springframework.scheduling;

import java.util.concurrent.Future;
import org.springframework.scheduling.annotation.Async;

/**
 * Aspect to route methods based on the {@link Async} annotation.
 * <p>
 * This aspect routes methods marked with the {@link Async} annotation
 * as well as methods in classes marked with the same. Any method expected
 * to be routed asynchronously must return either void, {@link Future}, 
 * or a subtype of {@link Future}. This aspect, therefore, will produce 
 * a compile-time error for methods that violate this constraint on the return type. 
 * If, however, a class marked with <code>&#64;Async</code> contains a method that 
 * violates this constraint, it produces only a warning.
 * 
 * @author Ramnivas Laddad
 *
 */
public aspect AnnotationDrivenAsynchronousExecutionAspect extends AbstractAsynchronousExecutionAspect {
	private pointcut asyncMarkedMethod() 
		: execution(@Async (void || Future+) *(..));
	private pointcut asyncTypeMarkedMethod() 
		: execution((void || Future+) (@Async *).*(..));
	
	public pointcut asyncMethod() : asyncMarkedMethod() || asyncTypeMarkedMethod();
	
	declare error: 
		execution(@Async !(void||Future) *(..)): 
		"Only method that return void or Future may have @Async annotation";

	declare warning: 
		execution(!(void||Future) (@Async *).*(..)): 
		"Method in class marked with @Async that do not return void or Future will be routed synchronously";
}
