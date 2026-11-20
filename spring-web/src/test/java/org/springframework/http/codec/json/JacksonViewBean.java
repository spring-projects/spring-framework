/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.json;

import com.fasterxml.jackson.annotation.JsonView;

/**
 * @author Sebastien Deleuze
 */
@JsonView(JacksonViewBean.MyJacksonView3.class)
class JacksonViewBean {

	interface MyJacksonView1 {}
	interface MyJacksonView2 {}
	interface MyJacksonView3 {}

	@JsonView(MyJacksonView1.class)
	private String withView1;

	@JsonView(MyJacksonView2.class)
	private String withView2;

	private String withoutView;

	public String getWithView1() {
		return withView1;
	}

	public void setWithView1(String withView1) {
		this.withView1 = withView1;
	}

	public String getWithView2() {
		return withView2;
	}

	public void setWithView2(String withView2) {
		this.withView2 = withView2;
	}

	public String getWithoutView() {
		return withoutView;
	}

	public void setWithoutView(String withoutView) {
		this.withoutView = withoutView;
	}
}
