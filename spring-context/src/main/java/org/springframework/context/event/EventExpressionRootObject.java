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

package org.springframework.context.event;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationEvent;

/**
 * Root object used during event listener SpEL expression evaluation.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 4.2
 * @param event the application event
 * @param args the arguments supplied to the listener method
 * @see EventListener#condition()
 */
record EventExpressionRootObject(ApplicationEvent event, @Nullable Object[] args) {
}
