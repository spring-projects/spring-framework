/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.samples.petclinic.web;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;

import org.springframework.samples.petclinic.Pet;
import org.springframework.samples.petclinic.PetType;
import org.springframework.samples.petclinic.Visit;

/**
 * @author Arjen Poutsma
 */
public class VisitsAtomViewTest {

	private VisitsAtomView visitView;

	private Map<String, Object> model;

	private Feed feed;

	@Before
	public void setUp() {
		visitView = new VisitsAtomView();
		PetType dog = new PetType();
		dog.setName("dog");
		Pet bello = new Pet();
		bello.setName("Bello");
		bello.setType(dog);
		Visit belloVisit = new Visit();
		belloVisit.setPet(bello);
		belloVisit.setDate(new Date(2009, 0, 1));
		belloVisit.setDescription("Bello visit");
		Pet wodan = new Pet();
		wodan.setName("Wodan");
		wodan.setType(dog);
		Visit wodanVisit = new Visit();
		wodanVisit.setPet(wodan);
		wodanVisit.setDate(new Date(2009, 0, 2));
		wodanVisit.setDescription("Wodan visit");
		List<Visit> visits = new ArrayList<Visit>();
		visits.add(belloVisit);
		visits.add(wodanVisit);

		model = new HashMap<String, Object>();
		model.put("visits", visits);
		feed = new Feed();

	}

	@Test
	public void buildFeedMetadata() {
		visitView.buildFeedMetadata(model, feed, null);

		assertNotNull("No id set", feed.getId());
		assertNotNull("No title set", feed.getTitle());
		assertEquals("Invalid update set", new Date(2009, 0, 2), feed.getUpdated());
	}

	@Test
	public void buildFeedEntries() throws Exception {
		List<Entry> entries = visitView.buildFeedEntries(model, null, null);
		assertEquals("Invalid amount of entries", 2, entries.size());
	}
}
