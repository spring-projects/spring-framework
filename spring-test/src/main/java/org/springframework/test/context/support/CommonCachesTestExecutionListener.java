/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	 * The {@link #getOrder() order} value for this listener: {@value}.
	 * @since 6.2.3
	 */
	public static final int ORDER = 3005;


	/**
	 * Returns {@value #ORDER}, which ensures that the {@code CommonCachesTestExecutionListener}
	 * is ordered after the
	 * {@link DirtiesContextTestExecutionListener DirtiesContextTestExecutionListener} and before the
	 * {@link org.springframework.test.context.transaction.TransactionalTestExecutionListener
	 * TransactionalTestExecutionListener}.
	 */
	@Override
	public final int getOrder() {
		return ORDER;
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
