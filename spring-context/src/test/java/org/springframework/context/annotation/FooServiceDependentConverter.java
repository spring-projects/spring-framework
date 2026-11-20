/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import example.scannable.FooService;

import org.springframework.core.convert.converter.Converter;

/**
 * @author Juergen Hoeller
 */
public class FooServiceDependentConverter implements Converter<String, org.springframework.beans.testfixture.beans.TestBean> {

	@SuppressWarnings("unused")
	private FooService fooService;

	public void setFooService(FooService fooService) {
		this.fooService = fooService;
	}

	@Override
	public org.springframework.beans.testfixture.beans.TestBean convert(String source) {
		return new org.springframework.beans.testfixture.beans.TestBean(source);
	}

}
