/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.context.event;

import org.springframework.context.ApplicationEvent;

/**
 * Root object used during event listener expression evaluation.
 *
 * @author Stephane Nicoll
 * @since 4.2
 */
class EventExpressionRootObject {

	private final ApplicationEvent event;

	private final Object[] args;

	public EventExpressionRootObject(ApplicationEvent event, Object[] args) {
		this.event = event;
		this.args = args;
	}

	public ApplicationEvent getEvent() {
		return this.event;
	}

	public Object[] getArgs() {
		return this.args;
	}

}
