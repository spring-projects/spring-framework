/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.test.web.reactive.server;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.Assert.assertSame;

/**
 * Unit tests for {@link DefaultControllerSpec}.
 * @author Rossen Stoyanchev
 */
public class DefaultControllerSpecTests {

	@Test
	public void controllers() throws Exception {
		OneController controller1 = new OneController();
		SecondController controller2 = new SecondController();

		TestControllerSpec spec = new TestControllerSpec(controller1, controller2);
		ApplicationContext context = spec.createApplicationContext();

		assertSame(controller1, context.getBean(OneController.class));
		assertSame(controller2, context.getBean(SecondController.class));
	}

	@Test
	public void controllerAdvice() throws Exception {
		OneControllerAdvice advice = new OneControllerAdvice();

		TestControllerSpec spec = new TestControllerSpec(new OneController());
		spec.controllerAdvice(advice);
		ApplicationContext context = spec.createApplicationContext();

		assertSame(advice, context.getBean(OneControllerAdvice.class));
	}

	private static class OneController {}

	private static class SecondController {}

	private static class OneControllerAdvice {}


	private static class TestControllerSpec extends DefaultControllerSpec {

		TestControllerSpec(Object... controllers) {
			super(controllers);
		}

		@Override
		public AnnotationConfigApplicationContext createApplicationContext() {
			return super.createApplicationContext();
		}
	}

}
