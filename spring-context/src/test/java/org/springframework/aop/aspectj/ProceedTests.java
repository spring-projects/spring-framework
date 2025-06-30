/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for SPR-3522. Arguments changed on a call to proceed should be
 * visible to advice further down the invocation chain.
 *
 * @author Adrian Colyer
 * @author Chris Beams
 */
class ProceedTests {

	private ClassPathXmlApplicationContext ctx;

	private SimpleBean testBean;

	private ProceedTestingAspect firstTestAspect;

	private ProceedTestingAspect secondTestAspect;


	@BeforeEach
	void setup() {
		this.ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
		testBean = (SimpleBean) ctx.getBean("testBean");
		firstTestAspect = (ProceedTestingAspect) ctx.getBean("firstTestAspect");
		secondTestAspect = (ProceedTestingAspect) ctx.getBean("secondTestAspect");
	}

	@AfterEach
	void tearDown() {
		this.ctx.close();
	}


	@Test
	void testSimpleProceedWithChangedArgs() {
		this.testBean.setName("abc");
		assertThat(this.testBean.getName()).as("Name changed in around advice").isEqualTo("ABC");
	}

	@Test
	void testGetArgsIsDefensive() {
		this.testBean.setAge(5);
		assertThat(this.testBean.getAge()).as("getArgs is defensive").isEqualTo(5);
	}

	@Test
	void testProceedWithArgsInSameAspect() {
		this.testBean.setMyFloat(1.0F);
		assertThat(this.testBean.getMyFloat()).as("value changed in around advice").isGreaterThan(1.9F);
		assertThat(this.firstTestAspect.getLastBeforeFloatValue()).as("changed value visible to next advice in chain")
				.isGreaterThan(1.9F);
	}

	@Test
	void testProceedWithArgsAcrossAspects() {
		this.testBean.setSex("male");
		assertThat(this.testBean.getSex()).as("value changed in around advice").isEqualTo("MALE");
		assertThat(this.secondTestAspect.getLastBeforeStringValue()).as("changed value visible to next before advice in chain").isEqualTo("MALE");
		assertThat(this.secondTestAspect.getLastAroundStringValue()).as("changed value visible to next around advice in chain").isEqualTo("MALE");
	}

}


interface SimpleBean {

	void setName(String name);
	String getName();
	void setAge(int age);
	int getAge();
	void setMyFloat(float f);
	float getMyFloat();
	void setSex(String sex);
	String getSex();
}


class SimpleBeanImpl implements SimpleBean {

	private int age;
	private float aFloat;
	private String name;
	private String sex;

	@Override
	public int getAge() {
		return age;
	}

	@Override
	public float getMyFloat() {
		return aFloat;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getSex() {
		return sex;
	}

	@Override
	public void setAge(int age) {
		this.age = age;
	}

	@Override
	public void setMyFloat(float f) {
		this.aFloat = f;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void setSex(String sex) {
		this.sex = sex;
	}
}


class ProceedTestingAspect implements Ordered {

	private String lastBeforeStringValue;
	private String lastAroundStringValue;
	private float lastBeforeFloatValue;
	private int order;

	public void setOrder(int order) { this.order = order; }
	@Override
	public int getOrder() { return this.order; }

	public Object capitalize(ProceedingJoinPoint pjp, String value) throws Throwable {
		return pjp.proceed(new Object[] {value.toUpperCase()});
	}

	public Object doubleOrQuits(ProceedingJoinPoint pjp) throws Throwable {
		int value = (Integer) pjp.getArgs()[0];
		pjp.getArgs()[0] = value * 2;
		return pjp.proceed();
	}

	public Object addOne(ProceedingJoinPoint pjp, Float value) throws Throwable {
		float fv = value;
		return pjp.proceed(new Object[] {fv + 1.0F});
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
		float tjpArg = (Float) tjp.getArgs()[0];
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
