/*
 * Copyright 2002-2012 the original author or authors.
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

package example.scannable;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

import org.springframework.stereotype.Component;

/**
 * @author Mark Fisher
 */
@Component
@Aspect
public class ServiceInvocationCounter {

	private int useCount;

	private static final ThreadLocal<Integer> threadLocalCount = new ThreadLocal<Integer>();


	@Pointcut("execution(* example.scannable.FooService+.*(..))")
	public void serviceExecution() {}

	@Before("serviceExecution()")
	public void countUse() {
		this.useCount++;
		threadLocalCount.set(this.useCount);
		System.out.println("");
	}

	public int getCount() {
		return this.useCount;
	}

	public static Integer getThreadLocalCount() {
		return threadLocalCount.get();
	}

}
