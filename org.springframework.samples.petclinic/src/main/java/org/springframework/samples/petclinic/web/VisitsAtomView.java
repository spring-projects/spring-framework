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
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.syndication.feed.atom.Content;
import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;

import org.springframework.samples.petclinic.Visit;
import org.springframework.web.servlet.view.feed.AbstractAtomFeedView;

/**
 * A view creating a Atom representation from a list of Visit objects. 
 * 
 * @author Alef Arendsen
 * @author Arjen Poutsma
 */
public class VisitsAtomView extends AbstractAtomFeedView {

	@Override
	protected void buildFeedMetadata(Map<String, Object> model, Feed feed, HttpServletRequest request) {
		feed.setId("tag:springsource.com");
		feed.setTitle("Pet Clinic Visits");
		@SuppressWarnings("unchecked")
		List<Visit> visits = (List<Visit>) model.get("visits");
		for (Visit visit : visits) {
			Date date = visit.getDate();
			if (feed.getUpdated() == null || date.compareTo(feed.getUpdated()) > 0) {
				feed.setUpdated(date);
			}
		}
	}

	@Override
	protected List<Entry> buildFeedEntries(Map<String, Object> model,
			HttpServletRequest request, HttpServletResponse response) throws Exception {

		@SuppressWarnings("unchecked")
		List<Visit> visits = (List<Visit>) model.get("visits");
		List<Entry> entries = new ArrayList<Entry>(visits.size());

		for (Visit visit : visits) {
			Entry entry = new Entry();
			String date = String.format("%1$tY-%1$tm-%1$td", visit.getDate());
			// see http://diveintomark.org/archives/2004/05/28/howto-atom-id#other
			entry.setId(String.format("tag:springsource.com,%s:%d", date, visit.getId()));
			entry.setTitle(String.format("%s visit on %s", visit.getPet().getName(), date));
			entry.setUpdated(visit.getDate());

			Content summary = new Content();
			summary.setValue(visit.getDescription());
			entry.setSummary(summary);

			entries.add(entry);
		}

		return entries;

	}
	
}
