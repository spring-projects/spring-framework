/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.transaction.event;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationEvent;

/**
 * @author Juergen Hoeller
 * @author Oliver Drotbohm
 */
class CapturingSynchronizationCallback implements TransactionalApplicationListener.SynchronizationCallback {

	@Nullable ApplicationEvent preEvent;

	@Nullable ApplicationEvent postEvent;

	@Nullable Throwable ex;

	@Override
	public void preProcessEvent(ApplicationEvent event) {
		this.preEvent = event;
	}

	@Override
	public void postProcessEvent(ApplicationEvent event, @Nullable Throwable ex) {
		this.postEvent = event;
		this.ex = ex;
	}

}
