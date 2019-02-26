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

import org.springframework.context.ApplicationEvent;
import org.springframework.test.context.TestContext;

/**
 * Base class for events published by {@link EventPublishingTestExecutionListener}.
 *
 * @author Frank Scheffler
 * @since 5.2
 */
@SuppressWarnings("serial")
public abstract class TestContextEvent extends ApplicationEvent {

    public TestContextEvent(TestContext source) {
        super(source);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.EventObject#getSource()
     */
    @Override
    public TestContext getSource() {
        return (TestContext) super.getSource();
    }
}
