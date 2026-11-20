/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import example.scannable.FooService;
import example.scannable.ServiceInvocationCounter;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class SimpleScanTests {

	protected String[] getConfigLocations() {
		return new String[] {"simpleScanTests.xml"};
	}

	@Test
	void fooService() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(getConfigLocations(), getClass());

		FooService fooService = (FooService) ctx.getBean("fooServiceImpl");
		ServiceInvocationCounter serviceInvocationCounter = (ServiceInvocationCounter) ctx.getBean("serviceInvocationCounter");

		assertThat(serviceInvocationCounter.getCount()).isEqualTo(0);

		assertThat(fooService.isInitCalled()).isTrue();
		assertThat(serviceInvocationCounter.getCount()).isEqualTo(1);

		String value = fooService.foo(1);
		assertThat(value).isEqualTo("bar");
		assertThat(serviceInvocationCounter.getCount()).isEqualTo(2);

		fooService.foo(1);
		assertThat(serviceInvocationCounter.getCount()).isEqualTo(3);
	}

}
