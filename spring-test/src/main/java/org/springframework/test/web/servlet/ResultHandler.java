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

package org.springframework.test.web.servlet;

/**
 * A {@code ResultHandler} performs a generic action on the result of an
 * executed request &mdash; for example, printing debug information.
 *
 * <p>See static factory methods in
 * {@link org.springframework.test.web.servlet.result.MockMvcResultHandlers
 * MockMvcResultHandlers}.
 *
 * <h3>Example</h3>
 *
 * <pre class="code">
 * import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
 * import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
 * import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;
 *
 * // ...
 *
 * WebApplicationContext wac = ...;
 *
 * MockMvc mockMvc = webAppContextSetup(wac).build();
 *
 * mockMvc.perform(get("/form")).andDo(print());
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
public interface ResultHandler {

	/**
	 * Perform an action on the given result.
	 *
	 * @param result the result of the executed request
	 * @throws Exception if a failure occurs
	 */
	void handle(MvcResult result) throws Exception;

}
