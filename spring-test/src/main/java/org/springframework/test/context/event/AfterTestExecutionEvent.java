/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.event;

import org.springframework.test.context.TestContext;

/**
 * {@link TestContextEvent} published by the {@link EventPublishingTestExecutionListener} when
 * {@link org.springframework.test.context.TestExecutionListener#afterTestExecution(TestContext)}
 * is invoked.
 *
 * @author Frank Scheffler
 * @since 5.2
 * @see org.springframework.test.context.event.annotation.AfterTestExecution @AfterTestExecution
 */
@SuppressWarnings("serial")
public class AfterTestExecutionEvent extends TestContextEvent {

	public AfterTestExecutionEvent(TestContext source) {
		super(source);
	}

}
