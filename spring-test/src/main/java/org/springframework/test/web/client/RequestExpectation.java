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
package org.springframework.test.web.client;

/**
 * An extension of {@code ResponseActions} that also implements
 * {@code RequestMatcher} and {@code ResponseCreator}
 *
 * <p>While {@code ResponseActions} is the API for defining expectations this
 * sub-interface is the internal SPI for matching these expectations to actual
 * requests and for creating responses.
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 */
public interface RequestExpectation extends ResponseActions, RequestMatcher, ResponseCreator {

	/**
	 * Whether there is a remaining count of invocations for this expectation.
	 */
	boolean hasRemainingCount();

	/**
	 * Whether the requirements for this request expectation have been met.
	 */
	boolean isSatisfied();

}
