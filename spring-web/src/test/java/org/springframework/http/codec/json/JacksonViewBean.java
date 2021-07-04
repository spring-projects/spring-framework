/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
