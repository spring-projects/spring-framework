package org.springframework.context.annotation6;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.testfixture.stereotype.Component;

/**
 * @author Jerry
 * @Date 2023/9/5 11:55
 */
@Component
public class B {
	@Autowired
	private A a;

	public A getA() {
		return a;
	}

	public void setA(A a) {
		this.a = a;
	}
}
