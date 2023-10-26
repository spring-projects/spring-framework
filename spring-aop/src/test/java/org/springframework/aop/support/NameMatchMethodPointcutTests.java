/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.aop.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.testfixture.interceptor.NopInterceptor;
import org.springframework.aop.testfixture.interceptor.SerializableNopInterceptor;
import org.springframework.beans.testfixture.beans.Person;
import org.springframework.beans.testfixture.beans.SerializablePerson;
import org.springframework.core.testfixture.io.SerializationTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NameMatchMethodPointcut}.
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @author Sam Brannen
 */
class NameMatchMethodPointcutTests {

	private final NameMatchMethodPointcut pc = new NameMatchMethodPointcut();

	private final SerializableNopInterceptor nop = new SerializableNopInterceptor();

	private Person personProxy;


	/**
	 * Create an empty pointcut, populating instance variables.
	 */
	@BeforeEach
	void setup() {
		ProxyFactory pf = new ProxyFactory(new SerializablePerson());
		pf.addAdvisor(new DefaultPointcutAdvisor(pc, nop));
		personProxy = (Person) pf.getProxy();
	}


	@Test
	void isMatch() {
		assertThat(pc.isMatch("echo", "echo")).isTrue();
		assertThat(pc.isMatch("echo", "ech*")).isTrue();
		assertThat(pc.isMatch("setName", "setN*")).isTrue();
		assertThat(pc.isMatch("setName", "set*")).isTrue();
		assertThat(pc.isMatch("getName", "set*")).isFalse();
		assertThat(pc.isMatch("setName", "set")).isFalse();
		assertThat(pc.isMatch("testing", "*ing")).isTrue();
	}

	@Test
	void noMappedMethodNamePatterns() throws Throwable {
		assertThat(nop.getCount()).isEqualTo(0);
		personProxy.getName();
		personProxy.setName("");
		personProxy.echo(null);
		assertThat(nop.getCount()).isEqualTo(0);
	}

	@Test
	void methodNamePatternsMappedIndividually() throws Throwable {
		pc.addMethodName("echo");
		pc.addMethodName("set*");

		assertThat(nop.getCount()).isEqualTo(0);

		personProxy.getName();
		assertThat(nop.getCount()).isEqualTo(0);

		personProxy.getName();
		assertThat(nop.getCount()).isEqualTo(0);

		personProxy.echo(null);
		assertThat(nop.getCount()).isEqualTo(1);

		personProxy.setName("");
		assertThat(nop.getCount()).isEqualTo(2);

		personProxy.setAge(25);
		assertThat(nop.getCount()).isEqualTo(3);
		assertThat(personProxy.getAge()).isEqualTo(25);
	}

	@Test
	void methodNamePatternsMappedAsVarargs() throws Throwable {
		pc.setMappedNames("set*", "echo");

		assertThat(nop.getCount()).isEqualTo(0);

		personProxy.getName();
		assertThat(nop.getCount()).isEqualTo(0);

		personProxy.setName("");
		assertThat(nop.getCount()).isEqualTo(1);

		personProxy.echo(null);
		assertThat(nop.getCount()).isEqualTo(2);
	}

	@Test
	void serializable() throws Throwable {
		methodNamePatternsMappedAsVarargs();

		Person p2 = SerializationTestUtils.serializeAndDeserialize(personProxy);
		NopInterceptor nop2 = (NopInterceptor) ((Advised) p2).getAdvisors()[0].getAdvice();

		// nop.getCount() should still be 2.
		assertThat(nop2.getCount()).isEqualTo(2);

		p2.getName();
		assertThat(nop2.getCount()).isEqualTo(2);

		p2.echo(null);
		assertThat(nop2.getCount()).isEqualTo(3);
	}

	@Test
	void equalsAndHashCode() {
		NameMatchMethodPointcut pc1 = new NameMatchMethodPointcut();
		NameMatchMethodPointcut pc2 = new NameMatchMethodPointcut();
		String mappedNamePattern = "foo";

		assertThat(pc2).isEqualTo(pc1);
		assertThat(pc2).hasSameHashCodeAs(pc1);

		pc1.setMappedName(mappedNamePattern);
		assertThat(pc1).isNotEqualTo(pc2);
		assertThat(pc1).doesNotHaveSameHashCodeAs(pc2);

		pc2.setMappedName(mappedNamePattern);
		assertThat(pc2).isEqualTo(pc1);
		assertThat(pc2).hasSameHashCodeAs(pc1);
	}

}
