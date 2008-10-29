/**
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.aop.aspectj;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;

import org.springframework.core.JdkVersion;
import org.springframework.core.Ordered;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * Test for SPR-3522. Arguments changed on a call to proceed should be
 * visible to advice further down the invocation chain.
 *
 * @author Adrian Colyer
 */
public class ProceedTests extends AbstractDependencyInjectionSpringContextTests {

	private SimpleBean testBean;

	private ProceedTestingAspect testAspect;

	private ProceedTestingAspect secondTestAspect;


	public ProceedTests() {
		setAutowireMode(AUTOWIRE_BY_NAME);
	}

	protected String[] getConfigLocations() {
		return new String[] {"org/springframework/aop/aspectj/proceedTests.xml"};
	}

	public void setFirstTestAspect(ProceedTestingAspect anAspect) {
		this.testAspect = anAspect;
	}

	public void setSecondTestAspect(ProceedTestingAspect anAspect) {
		this.secondTestAspect = anAspect;
	}

	public void setTestBean(SimpleBean aBean) {
		this.testBean = aBean;
	}


	public void testSimpleProceedWithChangedArgs() {
		this.testBean.setName("abc");
		assertEquals("Name changed in around advice", "ABC", this.testBean.getName());
	}

	public void testGetArgsIsDefensive() {
		this.testBean.setAge(5);
		assertEquals("getArgs is defensive", 5, this.testBean.getAge());
	}

	public void testProceedWithArgsInSameAspect() {
		if (!JdkVersion.isAtLeastJava15()) {
			// Doesn't work on JDK 1.4 for some reason...
			return;
		}

		this.testBean.setMyFloat(1.0F);
		assertTrue("value changed in around advice", this.testBean.getMyFloat() > 1.9F);
		assertTrue("changed value visible to next advice in chain", this.testAspect.getLastBeforeFloatValue() > 1.9F);
	}

	public void testProceedWithArgsAcrossAspects() {
		this.testBean.setSex("male");
		assertEquals("value changed in around advice","MALE", this.testBean.getSex());
		assertEquals("changed value visible to next before advice in chain","MALE", this.secondTestAspect.getLastBeforeStringValue());
		assertEquals("changed value visible to next around advice in chain","MALE", this.secondTestAspect.getLastAroundStringValue());
	}


	public interface SimpleBean {
		
		public void setName(String name);
		public String getName();
		public void setAge(int age);
		public int getAge();
		public void setMyFloat(float f);
		public float getMyFloat();
		public void setSex(String sex);
		public String getSex();
	}


	public static class SimpleBeanImpl implements SimpleBean {

		private int age;
		private float aFloat;
		private String name;
		private String sex;
		
		public int getAge() {
			return age;
		}

		public float getMyFloat() {
			return aFloat;
		}

		public String getName() {
			return name;
		}

		public String getSex() {
			return sex;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public void setMyFloat(float f) {
			this.aFloat = f;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setSex(String sex) {
			this.sex = sex;
		}
	}


	public static class ProceedTestingAspect implements Ordered {
		
		private String lastBeforeStringValue;
		private String lastAroundStringValue;
		private float lastBeforeFloatValue;
		private int order;
		
		public void setOrder(int order) { this.order = order; }
		public int getOrder() { return this.order; }
		
		public Object capitalize(ProceedingJoinPoint pjp, String value) throws Throwable {
			return pjp.proceed(new Object[] {value.toUpperCase()});
		}
		
		public Object doubleOrQuits(ProceedingJoinPoint pjp) throws Throwable {
			int value = ((Integer) pjp.getArgs()[0]).intValue();
			pjp.getArgs()[0] = new Integer(value * 2);
			return pjp.proceed();
		}
		
		public Object addOne(ProceedingJoinPoint pjp, Float value) throws Throwable {
			float fv = value.floatValue();
			return pjp.proceed(new Object[] {new Float(fv + 1.0F)});
		}
		
		public void captureStringArgument(JoinPoint tjp, String arg) {
			if (!tjp.getArgs()[0].equals(arg)) {
				throw new IllegalStateException(
						"argument is '" + arg + "', " +
						"but args array has '" + tjp.getArgs()[0] + "'"
						);
			}
			this.lastBeforeStringValue = arg;
		}
		
		public Object captureStringArgumentInAround(ProceedingJoinPoint pjp, String arg) throws Throwable {
			if (!pjp.getArgs()[0].equals(arg)) {
				throw new IllegalStateException(
						"argument is '" + arg + "', " +
						"but args array has '" + pjp.getArgs()[0] + "'");
			}
			this.lastAroundStringValue = arg;
			return pjp.proceed();
		}
		
		public void captureFloatArgument(JoinPoint tjp, float arg) {
			float tjpArg = ((Float) tjp.getArgs()[0]).floatValue();
			if (Math.abs(tjpArg - arg) > 0.000001) {
				throw new IllegalStateException(
						"argument is '" + arg + "', " +
						"but args array has '" + tjpArg + "'"
						);
			}
			this.lastBeforeFloatValue = arg;
		}
		
		public String getLastBeforeStringValue() {
			return this.lastBeforeStringValue;
		}
		
		public String getLastAroundStringValue() {
			return this.lastAroundStringValue;
		}
		
		public float getLastBeforeFloatValue() {
			return this.lastBeforeFloatValue;
		}
	}

}
