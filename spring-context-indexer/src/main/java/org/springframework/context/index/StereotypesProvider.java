/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.context.index;

import java.util.Set;
import javax.lang.model.element.Element;

/**
 * Provide the list of stereotypes that match an {@link Element}.
 * If an element has one more stereotypes, it is referenced in the index
 * of candidate components and each stereotype can be queried individually.
 *
 * @author Stephane Nicoll
 * @since 5.0
 */
interface StereotypesProvider {

	/**
	 * Return the stereotypes that are present on the given {@link Element}.
	 * @param element the element to handle
	 * @return the stereotypes or an empty set if none were found
	 */
	Set<String> getStereotypes(Element element);

}
