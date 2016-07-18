/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.client.reactive;

/**
 * Contract to post-process the {@link ClientWebRequest} after it is created
 * an initialized in order to mofidy or even wrap it. This may be used for
 * example to pre-package specific modifications to the request.
 *
 * @author Rob Winch
 * @author Brian Clozel
 * @since 5.0
 * @see DefaultClientWebRequestBuilder#apply(ClientWebRequestPostProcessor)
 */
public interface ClientWebRequestPostProcessor {

	/**
	 * Implementations can modify and/or wrap the {@link ClientWebRequest}
	 * passed in and return it
	 * @param request the {@link ClientWebRequest} to be modified and/or wrapped.
	 */
	ClientWebRequest postProcess(ClientWebRequest request);

}
