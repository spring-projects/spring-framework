/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.context.annotation;

import java.lang.instrument.ClassFileTransformer;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.EnableLoadTimeWeaving.AspectJWeaving;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.instrument.classloading.LoadTimeWeaver;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for @EnableLoadTimeWeaving
 *
 * @author Chris Beams
 * @since 3.1
 */
class EnableLoadTimeWeavingTests {

	@Test
	void control() {
		GenericXmlApplicationContext ctx =
				new GenericXmlApplicationContext(getClass(), "EnableLoadTimeWeavingTests-context.xml");
		ctx.getBean("loadTimeWeaver", LoadTimeWeaver.class);
		ctx.close();
	}

	@Test
	void enableLTW_withAjWeavingDisabled() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(EnableLTWConfig_withAjWeavingDisabled.class);
		ctx.refresh();
		LoadTimeWeaver loadTimeWeaver = ctx.getBean("loadTimeWeaver", LoadTimeWeaver.class);
		verifyNoInteractions(loadTimeWeaver);
		ctx.close();
	}

	@Test
	void enableLTW_withAjWeavingAutodetect() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(EnableLTWConfig_withAjWeavingAutodetect.class);
		ctx.refresh();
		LoadTimeWeaver loadTimeWeaver = ctx.getBean("loadTimeWeaver", LoadTimeWeaver.class);
		// no expectations -> a class file transformer should NOT be added
		// because no META-INF/aop.xml is present on the classpath
		verifyNoInteractions(loadTimeWeaver);
		ctx.close();
	}

	@Test
	void enableLTW_withAjWeavingEnabled() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(EnableLTWConfig_withAjWeavingEnabled.class);
		ctx.refresh();
		LoadTimeWeaver loadTimeWeaver = ctx.getBean("loadTimeWeaver", LoadTimeWeaver.class);
		verify(loadTimeWeaver).addTransformer(isA(ClassFileTransformer.class));
		ctx.close();
	}


	@Configuration
	@EnableLoadTimeWeaving(aspectjWeaving = AspectJWeaving.DISABLED)
	static class EnableLTWConfig_withAjWeavingDisabled implements LoadTimeWeavingConfigurer {

		@Override
		public LoadTimeWeaver getLoadTimeWeaver() {
			return mock();
		}
	}


	@Configuration
	@EnableLoadTimeWeaving(aspectjWeaving = AspectJWeaving.AUTODETECT)
	static class EnableLTWConfig_withAjWeavingAutodetect implements LoadTimeWeavingConfigurer {

		@Override
		public LoadTimeWeaver getLoadTimeWeaver() {
			return mock();
		}
	}


	@Configuration
	@EnableLoadTimeWeaving(aspectjWeaving = AspectJWeaving.ENABLED)
	static class EnableLTWConfig_withAjWeavingEnabled implements LoadTimeWeavingConfigurer {

		@Override
		public LoadTimeWeaver getLoadTimeWeaver() {
			return mock();
		}
	}

}
