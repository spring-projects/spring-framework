/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
 */
class DeclareParentsDelegateRefTests {

	private ClassPathXmlApplicationContext ctx;

	private NoMethodsBean noMethodsBean;

	private Counter counter;


	@BeforeEach
	void setup() {
		this.ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
		noMethodsBean = (NoMethodsBean) ctx.getBean("noMethodsBean");
		counter = (Counter) ctx.getBean("counter");
	}

	@AfterEach
	void tearDown() {
		this.ctx.close();
	}


	@Test
	void introductionWasMade() {
		assertThat(noMethodsBean).as("Introduction must have been made").isInstanceOf(ICounter.class);
	}

	@Test
	void introductionDelegation() {
		((ICounter)noMethodsBean).increment();
		assertThat(counter.getCount()).as("Delegate's counter should be updated").isEqualTo(1);
	}

}


interface NoMethodsBean {
}


class NoMethodsBeanImpl implements NoMethodsBean {
}

