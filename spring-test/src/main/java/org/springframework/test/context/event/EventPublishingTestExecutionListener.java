/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.context.event;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * {@link org.springframework.test.context.TestExecutionListener} that may be used to publish test life-cycle event to
 * the Spring test {@link org.springframework.context.ApplicationContext}.
 *
 * <p>These events may be consumed for various reasons, such as resetting {@em mock} beans or tracing test
 * execution. Since these events may consumed as part of regular Spring beans, they can be shared among
 * different test classes.
 *
 * <p>This {@link org.springframework.test.context.TestExecutionListener} is not active by default. Test classes
 * should be annotated using {@link org.springframework.test.context.TestExecutionListeners}, if they want to use it.
 * Alternatively, it may be added to {@code spring.factories}, if needed.
 *
 * @author Frank Scheffler
 * @since 5.2
 * @see org.springframework.test.context.event.annotation.BeforeTestClass
 * @see org.springframework.test.context.event.annotation.PrepareTestInstance
 * @see org.springframework.test.context.event.annotation.BeforeTestMethod
 * @see org.springframework.test.context.event.annotation.BeforeTestExecution
 * @see org.springframework.test.context.event.annotation.AfterTestExecution
 * @see org.springframework.test.context.event.annotation.AfterTestMethod
 * @see org.springframework.test.context.event.annotation.AfterTestClass
 */
public class EventPublishingTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public void beforeTestClass(TestContext testContext) {
        testContext.getApplicationContext().publishEvent(
                new BeforeTestClassEvent(testContext));
    }

    @Override
    public void prepareTestInstance(TestContext testContext) {
        testContext.getApplicationContext().publishEvent(
                new PrepareTestInstanceEvent(testContext));
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        testContext.getApplicationContext().publishEvent(
                new BeforeTestMethodEvent(testContext));
    }

    @Override
    public void beforeTestExecution(TestContext testContext) {
        testContext.getApplicationContext().publishEvent(
                new BeforeTestExecutionEvent(testContext));
    }

    @Override
    public void afterTestExecution(TestContext testContext) {
        testContext.getApplicationContext().publishEvent(
                new AfterTestExecutionEvent(testContext));
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
        testContext.getApplicationContext().publishEvent(
                new AfterTestMethodEvent(testContext));
    }

    @Override
    public void afterTestClass(TestContext testContext) {
        testContext.getApplicationContext().publishEvent(
                new AfterTestClassEvent(testContext));
    }
}
