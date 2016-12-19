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

package org.springframework.context.index.metadata;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents one entry in the index. The type defines the identify of the target
 * candidate (usually fully qualified name) and the stereotypes are "markers" that can
 * be used to retrieve the candidates. A typical use case is the presence of a given
 * annotation on the candidate.
 *
 * @author Stephane Nicoll
 * @since 5.0
 */
public class ItemMetadata {

	private final String type;

	private final Set<String> stereotypes;

	public ItemMetadata(String type, Set<String> stereotypes) {
		this.type = type;
		this.stereotypes = new HashSet<>(stereotypes);
	}

	public String getType() {
		return this.type;
	}

	public Set<String> getStereotypes() {
		return this.stereotypes;
	}

}
