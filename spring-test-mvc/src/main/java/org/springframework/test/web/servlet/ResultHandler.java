/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.web.servlet;

/**
 * Executes a generic action (e.g. printing debug information) on the result of
 * an executed request.
 *
 * <p>See static factory methods in
 * {@code org.springframework.test.web.server.result.MockMvcResultHandlers}.
 *
 * <p>Example:
 *
 * <pre>
 * static imports: MockMvcRequestBuilders.*, MockMvcResultHandlers.*
 *
 * mockMvc.perform(get("/form")).andDo(print());
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public interface ResultHandler {

	/**
	 * Apply the action on the given result.
	 *
	 * @param result the result of the executed request
	 * @throws Exception if a failure occurs
	 */
	void handle(MvcResult result) throws Exception;

}
