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

package org.springframework.beans.factory.parsing;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.DescriptiveResource;

/**
 * TODO SPR-7420: document
 *
 * @author Chris Beams
 * @since 3.1
 */
public class SimpleProblemCollector {

	private Location location = null;
	private List<Problem> errors = new ArrayList<Problem>();

	public SimpleProblemCollector(Object location) {
		if (location != null) {
			this.location = new Location(new DescriptiveResource(location.toString()));
		}
	}

	public void error(String message) {
		this.errors.add(new Problem(message, this.location));
	}

	public void error(String message, Throwable cause) {
		this.errors.add(new Problem(message, this.location, null, cause));
	}

	public void reportProblems(ProblemReporter reporter) {
		for (Problem error : errors) {
			reporter.error(error);
		}
	}

	public boolean hasErrors() {
		return this.errors.size() > 0;
	}

}
