/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package example.scannable;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

/**
 * @author Mark Fisher
 */
@CustomAspectStereotype({"myPointcutInfo", "otherPointcutInfo"})
@Aspect
public class ServiceInvocationCounter {

	private int useCount;

	private static final ThreadLocal<Integer> threadLocalCount = new ThreadLocal<>();


	@Pointcut("execution(* example.scannable.FooService+.*(..))")
	public void serviceExecution() {}

	@Before("serviceExecution()")
	public void countUse() {
		this.useCount++;
		threadLocalCount.set(this.useCount);
	}

	public int getCount() {
		return this.useCount;
	}

	public static Integer getThreadLocalCount() {
		return threadLocalCount.get();
	}

}
