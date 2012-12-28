/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.context.annotation.configuration;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * Tests ensuring that @Configuration-related CGLIB callbacks are de-registered
 * at container shutdown time, allowing for proper garbage collection. See SPR-7901.
 *
 * @author Chris Beams
 */
public class ConfigurationClassCglibCallbackDeregistrationTests {

	/**
	 * asserting that the actual callback is deregistered is difficult,
	 * but we can at least assert that the @Configuration class is enhanced
	 * to implement DisposableBean. The enhanced implementation of destroy()
	 * will do the de-registration work.
	 */
	@Test
	public void destroyContext() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
		Config config = ctx.getBean(Config.class);
		assertThat(config, instanceOf(DisposableBean.class));
		ctx.destroy();
	}

	/**
	 * The DisposableBeanMethodInterceptor in ConfigurationClassEnhancer
	 * should be careful to invoke any explicit super-implementation of
	 * DisposableBean#destroy().
	 */
	@Test
	public void destroyExplicitDisposableBeanConfig() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(DisposableConfig.class);
		DisposableConfig config = ctx.getBean(DisposableConfig.class);
		assertThat(config.destroyed, is(false));
		ctx.destroy();
		assertThat("DisposableConfig.destroy() was not invoked", config.destroyed, is(true));
	}


	@Configuration
	static class Config {
	}


	@Configuration
	static class DisposableConfig implements DisposableBean {
		boolean destroyed = false;
		@Override
		public void destroy() throws Exception {
			this.destroyed = true;
		}
	}
}
