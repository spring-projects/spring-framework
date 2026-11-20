/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export;

/**
 * @author Rob Harrop
 */
public class ExceptionOnInitBean {

	private boolean exceptOnInit = false;

	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setExceptOnInit(boolean exceptOnInit) {
		this.exceptOnInit = exceptOnInit;
	}

	public ExceptionOnInitBean() {
		if (exceptOnInit) {
			throw new RuntimeException("I am being init'd!");
		}
	}

}
