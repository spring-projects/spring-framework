/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.support;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Scott Andrews
 * @author Juergen Hoeller
 */
class Spr7283Tests {

	@Test
	void listWithInconsistentElementTypes() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("spr7283.xml", getClass());
		List<?> list = ctx.getBean("list", List.class);
		assertThat(list).satisfiesExactly(zero -> assertThat(zero).isInstanceOf(A.class),
				one -> assertThat(one).isInstanceOf(B.class));
		ctx.close();
	}


	static class A {
		A() {}
	}

	static class B {
		B() {}
	}

}
