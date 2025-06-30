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

package org.springframework.context.index.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Meta-data for candidate components.
 *
 * @author Stephane Nicoll
 * @since 5.0
 */
class CandidateComponentsMetadata {

	private final List<ItemMetadata> items;


	public CandidateComponentsMetadata() {
		this.items = new ArrayList<>();
	}


	public void add(ItemMetadata item) {
		this.items.add(item);
	}

	public List<ItemMetadata> getItems() {
		return Collections.unmodifiableList(this.items);
	}

	@Override
	public String toString() {
		return "CandidateComponentsMetadata{" + "items=" + this.items + '}';
	}

}
