/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.annotation;

/**
 * @author Stephane Nicoll
 */
public class AnotherAnnotationTestBeanImpl implements AnotherAnnotationTestBean {

	private String bar;

	@Override
	public void foo() {
	}

	public void doNotExpose() {

	}

	@Override
	public String getBar() {
		return this.bar;
	}

	@Override
	public void setBar(String bar) {
		this.bar = bar;
	}

	@Override
	public int getCacheEntries() {
		return 42;
	}

}
