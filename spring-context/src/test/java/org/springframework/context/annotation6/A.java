package org.springframework.context.annotation6;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.testfixture.stereotype.Component;

/**
 * @author Jerry
 * @Date 2023/9/5 11:54
 */
@Component
public class A {
	@Autowired
	private B b;

	public B getB() {
		return b;
	}

	public void setB(B b) {
		this.b = b;
	}
}
