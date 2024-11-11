/*
 * Copyright 2002-2024 the original author or authors.
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ramnivas Laddad
 * @author Chris Beams
 * @author Sam Brannen
 */
class ThisAndTargetSelectionOnlyPointcutsTests {

	private ClassPathXmlApplicationContext ctx;

	private TestInterface testBean;

	private Counter thisAsClassCounter;
	private Counter thisAsInterfaceCounter;
	private Counter targetAsClassCounter;
	private Counter targetAsInterfaceCounter;
	private Counter thisAsClassAndTargetAsClassCounter;
	private Counter thisAsInterfaceAndTargetAsInterfaceCounter;


	@BeforeEach
	void setup() {
		this.ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
		testBean = (TestInterface) ctx.getBean("testBean");
		thisAsClassCounter = ctx.getBean("thisAsClassCounter", Counter.class);
		thisAsInterfaceCounter = ctx.getBean("thisAsInterfaceCounter", Counter.class);
		targetAsClassCounter = ctx.getBean("targetAsClassCounter", Counter.class);
		targetAsInterfaceCounter = ctx.getBean("targetAsInterfaceCounter", Counter.class);
		thisAsClassAndTargetAsClassCounter = ctx.getBean("thisAsClassAndTargetAsClassCounter", Counter.class);
		thisAsInterfaceAndTargetAsInterfaceCounter = ctx.getBean("thisAsInterfaceAndTargetAsInterfaceCounter", Counter.class);
	}

	@AfterEach
	void tearDown() {
		this.ctx.close();
	}


	@Test
	void thisAsClassDoesNotMatch() {
		testBean.doIt();
		assertThat(thisAsClassCounter.getCount()).isEqualTo(0);
	}

	@Test
	void thisAsInterfaceMatch() {
		testBean.doIt();
		assertThat(thisAsInterfaceCounter.getCount()).isEqualTo(1);
	}

	@Test
	void targetAsClassDoesMatch() {
		testBean.doIt();
		assertThat(targetAsClassCounter.getCount()).isEqualTo(1);
	}

	@Test
	void targetAsInterfaceMatch() {
		testBean.doIt();
		assertThat(targetAsInterfaceCounter.getCount()).isEqualTo(1);
	}

	@Test
	void thisAsClassAndTargetAsClassCounterNotMatch() {
		testBean.doIt();
		assertThat(thisAsClassAndTargetAsClassCounter.getCount()).isEqualTo(0);
	}

	@Test
	void thisAsInterfaceAndTargetAsInterfaceCounterMatch() {
		testBean.doIt();
		assertThat(thisAsInterfaceAndTargetAsInterfaceCounter.getCount()).isEqualTo(1);
	}

	@Test
	void thisAsInterfaceAndTargetAsClassCounterMatch() {
		testBean.doIt();
		assertThat(thisAsInterfaceAndTargetAsInterfaceCounter.getCount()).isEqualTo(1);
	}

}


interface TestInterface {
	void doIt();
}


class TestImpl implements TestInterface {
	@Override
	public void doIt() {
	}
}
