/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.support;

import org.junit.jupiter.api.Test;

import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GenericXmlApplicationContext}.
 *
 * See SPR-7530.
 *
 * @author Chris Beams
 */
class GenericXmlApplicationContextTests {

	private static final Class<?> RELATIVE_CLASS = GenericXmlApplicationContextTests.class;
	private static final String RESOURCE_BASE_PATH = ClassUtils.classPackageAsResourcePath(RELATIVE_CLASS);
	private static final String RESOURCE_NAME = GenericXmlApplicationContextTests.class.getSimpleName() + "-context.xml";
	private static final String FQ_RESOURCE_PATH = RESOURCE_BASE_PATH + '/' + RESOURCE_NAME;
	private static final String TEST_BEAN_NAME = "testBean";


	@Test
	void classRelativeResourceLoading_ctor() {
		GenericXmlApplicationContext ctx = new GenericXmlApplicationContext(RELATIVE_CLASS, RESOURCE_NAME);
		assertThat(ctx.containsBean(TEST_BEAN_NAME)).isTrue();
		ctx.close();
	}

	@Test
	void classRelativeResourceLoading_load() {
		GenericXmlApplicationContext ctx = new GenericXmlApplicationContext();
		ctx.load(RELATIVE_CLASS, RESOURCE_NAME);
		ctx.refresh();
		assertThat(ctx.containsBean(TEST_BEAN_NAME)).isTrue();
		ctx.close();
	}

	@Test
	void fullyQualifiedResourceLoading_ctor() {
		GenericXmlApplicationContext ctx = new GenericXmlApplicationContext(FQ_RESOURCE_PATH);
		assertThat(ctx.containsBean(TEST_BEAN_NAME)).isTrue();
		ctx.close();
	}

	@Test
	void fullyQualifiedResourceLoading_load() {
		GenericXmlApplicationContext ctx = new GenericXmlApplicationContext();
		ctx.load(FQ_RESOURCE_PATH);
		ctx.refresh();
		assertThat(ctx.containsBean(TEST_BEAN_NAME)).isTrue();
		ctx.close();
	}

}
