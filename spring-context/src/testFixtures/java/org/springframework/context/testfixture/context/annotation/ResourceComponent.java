/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.context.annotation;

import jakarta.annotation.Resource;

public class ResourceComponent {

	private String text;

	private Integer counter;

	public String getText() {
		return this.text;
	}

	@Resource
	public void setText(String text) {
		this.text = text;
	}

	public Integer getCounter() {
		return this.counter;
	}

	@Resource
	public void setCounter(Integer counter) {
		this.counter = counter;
	}

}
