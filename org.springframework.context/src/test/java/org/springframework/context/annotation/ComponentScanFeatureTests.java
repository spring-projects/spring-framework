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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ComponentScanFeatureTests {
	@Test
	public void viaContextRegistration() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ComponentScanFeatureConfig.class);
		ctx.register(ComponentScanFeatureConfig.Features.class);
		ctx.refresh();
		ctx.getBean(ComponentScanFeatureConfig.class);
		ctx.getBean(TestBean.class);
		assertThat("config class bean not found", ctx.containsBeanDefinition("componentScanFeatureConfig"), is(true));
		assertThat("@ComponentScan annotated @Configuration class registered directly against " +
				"AnnotationConfigApplicationContext did not trigger component scanning as expected",
				ctx.containsBean("fooServiceImpl"), is(true));
	}
}

@Configuration
//@Import(ComponentScanFeatureConfig.Features.class)
class ComponentScanFeatureConfig {
	@FeatureConfiguration
	static class Features {
		@Feature
		public ComponentScanSpec componentScan() {
			return new ComponentScanSpec(example.scannable._package.class);
		}
	}

	@Bean
	public TestBean testBean() {
		return new TestBean();
	}
}