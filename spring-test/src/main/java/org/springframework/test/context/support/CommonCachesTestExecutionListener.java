/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.context.support;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.test.context.TestContext;

/**
 * {@code TestExecutionListener} which makes sure that common caches are cleared
 * once they are no longer required.
 *
 * <p>Clears the resource caches of the {@link ApplicationContext} since they are
 * only required during the bean initialization phase. Runs after
 * {@link DirtiesContextTestExecutionListener} since dirtying the context will
 * close it and remove it from the context cache, making it unnecessary to clear
 * the associated resource caches.
 *
 * @author Stephane Nicoll
 * @since 6.2
 */
public class CommonCachesTestExecutionListener extends AbstractTestExecutionListener {

	/**
	 * Returns {@code 3005}.
	 */
	@Override
	public final int getOrder() {
		return 3005;
	}


	@Override
	public void afterTestClass(TestContext testContext) throws Exception {
		if (testContext.hasApplicationContext()) {
			ApplicationContext applicationContext = testContext.getApplicationContext();
			if (applicationContext instanceof AbstractApplicationContext ctx) {
				ctx.clearResourceCaches();
			}
		}
	}

}
