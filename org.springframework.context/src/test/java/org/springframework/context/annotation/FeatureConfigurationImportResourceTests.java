/*
 * Copyright 2002-2011 the original author or authors.
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.configuration.StubSpecification;
import org.springframework.context.config.FeatureSpecification;

import test.beans.TestBean;

/**
 * Tests proving that @FeatureConfiguration classes may be use @ImportResource
 * and then parameter autowire beans declared in the imported resource(s).
 *
 * @author Chris Beams
 * @since 3.1
 */
public class FeatureConfigurationImportResourceTests {

	@Test
	public void importResourceFromFeatureConfiguration() {
		ApplicationContext ctx =
			new AnnotationConfigApplicationContext(ImportingFeatureConfig.class);
		TestBean testBean = ctx.getBean(TestBean.class);
		assertThat(testBean.getName(), equalTo("beanFromXml"));

		// and just quickly prove that the target of the bean proxied for the Feature method
		// is indeed the same singleton instance as the one we just pulled from the container
		ImportingFeatureConfig ifc = ctx.getBean(ImportingFeatureConfig.class);
		TestBean proxyBean = ifc.testBean;
		assertThat(proxyBean, instanceOf(EarlyBeanReferenceProxy.class));
		assertNotSame(proxyBean, testBean);
		assertSame(((EarlyBeanReferenceProxy)proxyBean).dereferenceTargetBean(), testBean);
	}

	@FeatureConfiguration
	@ImportResource("org/springframework/context/annotation/FeatureConfigurationImportResourceTests-context.xml")
	static class ImportingFeatureConfig {
		TestBean testBean;

		@Feature
		public FeatureSpecification f(TestBean testBean) {
			this.testBean = testBean;
			return new StubSpecification();
		}
	}

}
