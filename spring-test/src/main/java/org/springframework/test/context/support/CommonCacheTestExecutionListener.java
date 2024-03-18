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
 * {@code TestExecutionListener} which makes sure that caches are cleared once
 * they are no longer required. Clears the resource cache of the
 * {@link ApplicationContext} as it is only required during the beans
 * initialization phase. Runs after {@link DirtiesContextTestExecutionListener}
 * as dirtying the context will remove it from the cache and make this
 * unnecessary.
 *
 * @author Stephane Nicoll
 * @since 6.2
 */
public class CommonCacheTestExecutionListener extends AbstractTestExecutionListener {

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
