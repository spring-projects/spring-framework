/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.support;

/**
 * @author Alef Arendsen
 */
public class Assembler implements TestIF {

	@SuppressWarnings("unused")
	private Service service;

	private Logic l;

	@SuppressWarnings("unused")
	private String name;

	public void setService(Service service) {
		this.service = service;
	}

	public void setLogic(Logic l) {
		this.l = l;
	}

	public void setBeanName(String name) {
		this.name = name;
	}

	public void test() {
	}

	public void output() {
		l.output();
	}

}
