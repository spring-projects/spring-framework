/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.servlet.tags;

import javax.servlet.jsp.JspTagException;

/**
 * Allows implementing tag to utilize nested {@code spring:argument} tags.
 *
 * @author Nicholas Williams
 * @since 4.0
 * @see ArgumentTag
 */
public interface ArgumentAware {

	/**
	 * Callback hook for nested spring:argument tags to pass their value
	 * to the parent tag.
	 * @param argument the result of the nested {@code spring:argument} tag
	 */
	void addArgument(Object argument) throws JspTagException;

}
