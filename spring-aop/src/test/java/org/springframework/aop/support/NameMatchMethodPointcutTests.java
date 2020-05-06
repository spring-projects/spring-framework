/*
 * Copyright 2002-2019 the original author or authors.
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
 * @author Rod Johnson
 * @author Chris Beams
 */
public class NameMatchMethodPointcutTests {

	protected NameMatchMethodPointcut pc;

	protected Person proxied;

	protected SerializableNopInterceptor nop;


	/**
	 * Create an empty pointcut, populating instance variables.
	 */
	@BeforeEach
	public void setup() {
		ProxyFactory pf = new ProxyFactory(new SerializablePerson());
		nop = new SerializableNopInterceptor();
		pc = new NameMatchMethodPointcut();
		pf.addAdvisor(new DefaultPointcutAdvisor(pc, nop));
		proxied = (Person) pf.getProxy();
	}


	@Test
	public void testMatchingOnly() {
		// Can't do exact matching through isMatch
		assertThat(pc.isMatch("echo", "ech*")).isTrue();
		assertThat(pc.isMatch("setName", "setN*")).isTrue();
		assertThat(pc.isMatch("setName", "set*")).isTrue();
		assertThat(pc.isMatch("getName", "set*")).isFalse();
		assertThat(pc.isMatch("setName", "set")).isFalse();
		assertThat(pc.isMatch("testing", "*ing")).isTrue();
	}

	@Test
	public void testEmpty() throws Throwable {
		assertThat(nop.getCount()).isEqualTo(0);
		proxied.getName();
		proxied.setName("");
		proxied.echo(null);
		assertThat(nop.getCount()).isEqualTo(0);
	}


	@Test
	public void testMatchOneMethod() throws Throwable {
		pc.addMethodName("echo");
		pc.addMethodName("set*");
		assertThat(nop.getCount()).isEqualTo(0);
		proxied.getName();
		proxied.getName();
		assertThat(nop.getCount()).isEqualTo(0);
		proxied.echo(null);
		assertThat(nop.getCount()).isEqualTo(1);

		proxied.setName("");
		assertThat(nop.getCount()).isEqualTo(2);
		proxied.setAge(25);
		assertThat(proxied.getAge()).isEqualTo(25);
		assertThat(nop.getCount()).isEqualTo(3);
	}

	@Test
	public void testSets() throws Throwable {
		pc.setMappedNames("set*", "echo");
		assertThat(nop.getCount()).isEqualTo(0);
		proxied.getName();
		proxied.setName("");
		assertThat(nop.getCount()).isEqualTo(1);
		proxied.echo(null);
		assertThat(nop.getCount()).isEqualTo(2);
	}

	@Test
	public void testSerializable() throws Throwable {
		testSets();
		// Count is now 2
		Person p2 = (Person) SerializationTestUtils.serializeAndDeserialize(proxied);
		NopInterceptor nop2 = (NopInterceptor) ((Advised) p2).getAdvisors()[0].getAdvice();
		p2.getName();
		assertThat(nop2.getCount()).isEqualTo(2);
		p2.echo(null);
		assertThat(nop2.getCount()).isEqualTo(3);
	}

	@Test
	public void testEqualsAndHashCode() {
		NameMatchMethodPointcut pc1 = new NameMatchMethodPointcut();
		NameMatchMethodPointcut pc2 = new NameMatchMethodPointcut();

		String foo = "foo";

		assertThat(pc2).isEqualTo(pc1);
		assertThat(pc2.hashCode()).isEqualTo(pc1.hashCode());

		pc1.setMappedName(foo);
		assertThat(pc1.equals(pc2)).isFalse();
		assertThat(pc1.hashCode() != pc2.hashCode()).isTrue();

		pc2.setMappedName(foo);
		assertThat(pc2).isEqualTo(pc1);
		assertThat(pc2.hashCode()).isEqualTo(pc1.hashCode());
	}

}
