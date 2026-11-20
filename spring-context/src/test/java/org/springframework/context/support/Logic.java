/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.support;

import org.springframework.beans.factory.BeanNameAware;

public class Logic implements BeanNameAware {

	@SuppressWarnings("unused")
	private String name;

	@SuppressWarnings("unused")
	private Assembler a;

	public void setAssembler(Assembler a) {
		this.a = a;
	}

	@Override
	public void setBeanName(String name) {
		this.name = name;
	}

	public void output() {
		// System.out.println("Bean " + name);
	}

}
