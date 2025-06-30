/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.aop.aspectj.autoproxy;

/**
 * @author Adrian Colyer
 * @since 2.0
 */
class AnnotatedTestBeanImpl implements AnnotatedTestBean {

	@Override
	@TestAnnotation("this value")
	public String doThis() {
		return "doThis";
	}

	@Override
	@TestAnnotation("that value")
	public String doThat() {
		return "doThat";
	}

	@Override
	@TestAnnotation("array value")
	public String[] doArray() {
		return new String[] {"doThis", "doThat"};
	}

	// not annotated
	@Override
	public String doTheOther() {
		return "doTheOther";
	}

}
