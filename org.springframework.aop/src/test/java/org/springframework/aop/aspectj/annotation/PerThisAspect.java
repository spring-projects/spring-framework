/**
 * 
 */
package org.springframework.aop.aspectj.annotation;

import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;

@Aspect("perthis(execution(* *.getSpouse()))")
public class PerThisAspect {

	public int count;

	/**
	 * Just to check that this doesn't cause problems with introduction processing
	 */
	private ITestBean fieldThatShouldBeIgnoredBySpringAtAspectJProcessing = new TestBean();

	@Around("execution(int *.getAge())")
	public int returnCountAsAge() {
		return count++;
	}

	@Before("execution(void *.set*(int))")
	public void countSetter() {
		++count;
	}
}