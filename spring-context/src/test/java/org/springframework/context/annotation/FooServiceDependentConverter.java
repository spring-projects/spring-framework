/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import example.scannable.FooService;

import org.springframework.core.convert.converter.Converter;

/**
 * @author Juergen Hoeller
 */
public class FooServiceDependentConverter implements Converter<String, org.springframework.tests.sample.beans.TestBean> {

	@SuppressWarnings("unused")
	private FooService fooService;

	public void setFooService(FooService fooService) {
		this.fooService = fooService;
	}

	@Override
	public org.springframework.tests.sample.beans.TestBean convert(String source) {
		return new org.springframework.tests.sample.beans.TestBean(source);
	}

}
