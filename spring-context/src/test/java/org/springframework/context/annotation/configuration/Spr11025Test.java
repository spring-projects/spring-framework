/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation.configuration;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Test Configuration class overloaded methods by inheritance
 * 
 * @author Jose Luis Martin
 */
public class Spr11025Test {

	@Test
	public void testBeanMethodOverloading() {
		try {
			AnnotationConfigApplicationContext ctx = 
					new AnnotationConfigApplicationContext(OverloadingConfig.class);
			ctx.close();
			Assert.fail("Accepted invalid overloaded configuration");
			
		}
		catch(BeanDefinitionParsingException bdpe) {
			// pass
		}
	}
	
	@Test
	public void testBeanMethodOverriding() {
		
		AnnotationConfigApplicationContext ctx = 
				new AnnotationConfigApplicationContext(OverridingConfig.class);
		ctx.close();
	}

	@Configuration
	public static class ParentConfig {

		@Bean
		public String bean() {
			return "parentBean";
		}

	}

	@Configuration
	public static class OverridingConfig extends ParentConfig {


		@Bean
		public String bean() {
			return "overriden";
		}

	}
	
	@Configuration
	public static class OverloadingConfig extends ParentConfig {


		@Bean
		public String bean(String arg) {
			return "overriden";
		}

	}

}
