/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.servlet.handler;

import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Abstract adapter class for the {@link AsyncHandlerInterceptor} interface,
 * for simplified implementation of pre-only/post-only interceptors.
 *
 * @author Juergen Hoeller
 * @since 05.12.2003
 * @deprecated as of 5.3 in favor of implementing {@link HandlerInterceptor}
 * and/or {@link AsyncHandlerInterceptor} directly.
 */
@Deprecated
public abstract class HandlerInterceptorAdapter implements AsyncHandlerInterceptor {

}
