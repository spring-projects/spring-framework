/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.scheduling.aspectj;

import java.util.concurrent.Future;
import org.springframework.scheduling.annotation.Async;

/**
 * Aspect to route methods based on the {@link Async} annotation.
 *
 * <p>This aspect routes methods marked with the {@link Async} annotation
 * as well as methods in classes marked with the same. Any method expected
 * to be routed asynchronously must return either void, {@link Future}, 
 * or a subtype of {@link Future}. This aspect, therefore, will produce 
 * a compile-time error for methods that violate this constraint on the return type. 
 * If, however, a class marked with <code>&#64;Async</code> contains a method that 
 * violates this constraint, it produces only a warning.
 * 
 * @author Ramnivas Laddad
 * @since 3.0.5
 */
public aspect AnnotationAsyncExecutionAspect extends AbstractAsyncExecutionAspect {

	private pointcut asyncMarkedMethod() 
		: execution(@Async (void || Future+) *(..));

	private pointcut asyncTypeMarkedMethod() 
		: execution((void || Future+) (@Async *).*(..));
	
	public pointcut asyncMethod() : asyncMarkedMethod() || asyncTypeMarkedMethod();
	
	declare error: 
		execution(@Async !(void||Future) *(..)): 
		"Only methods that return void or Future may have an @Async annotation";

	declare warning: 
		execution(!(void||Future) (@Async *).*(..)): 
		"Methods in a class marked with @Async that do not return void or Future will be routed synchronously";

}
