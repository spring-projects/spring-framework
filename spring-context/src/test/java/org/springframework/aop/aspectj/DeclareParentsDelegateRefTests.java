/*
 * Copyright 2002-2008 the original author or authors.
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

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Ramnivas Laddad
 * @author Chris Beams
 */
public class DeclareParentsDelegateRefTests {

	protected NoMethodsBean noMethodsBean;

	protected Counter counter;


	@Before
	public void setUp() {
		ClassPathXmlApplicationContext ctx =
			new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
		noMethodsBean = (NoMethodsBean) ctx.getBean("noMethodsBean");
		counter = (Counter) ctx.getBean("counter");
		counter.reset();
	}

	@Test
	public void testIntroductionWasMade() {
		assertTrue("Introduction must have been made", noMethodsBean instanceof ICounter);
	}

	@Test
	public void testIntroductionDelegation() {
		((ICounter)noMethodsBean).increment();
		assertEquals("Delegate's counter should be updated", 1, counter.getCount());
	}

}


interface NoMethodsBean {
}


class NoMethodsBeanImpl implements NoMethodsBean {
}

