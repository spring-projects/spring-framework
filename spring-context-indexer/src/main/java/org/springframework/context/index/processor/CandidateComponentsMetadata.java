/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
