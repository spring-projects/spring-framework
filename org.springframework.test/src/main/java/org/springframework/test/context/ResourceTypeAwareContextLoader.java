/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.test.context;

/**
 * TODO Document ResourceTypeAwareContextLoader.
 *
 * @author Sam Brannen
 * @since 3.1
 */
public interface ResourceTypeAwareContextLoader extends ContextLoader {

	/**
	 * @return <code>true</code> if this <code>ContextLoader</code> supports
	 * String-based resource locations
	 * @see ContextConfiguration#locations()
	 * @see ContextConfiguration#value()
	 */
	boolean supportsStringResources();

	/**
	 * @return <code>true</code> if this <code>ContextLoader</code> supports
	 * Class-based resource locations
	 * @see ContextConfiguration#classes()
	 */
	boolean supportsClassResources();

}
