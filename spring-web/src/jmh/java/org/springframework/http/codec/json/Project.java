/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.http.codec.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sample Pojo for JSON encoder benchmarks.
 * @author Brian Clozel
 */
public class Project {

	private String name;

	private String url;

	private List<Project> subProjects = Collections.emptyList();

	public Project() {
	}

	public Project(String name) {
		this.name = name;
		this.url = "https://spring.io/projects/" + name;
	}

	public Project(String name, int subProjectsCount) {
		this(name);
		this.subProjects = new ArrayList<>(subProjectsCount);
		for (int i = 0; i < subProjectsCount; i++) {
			this.subProjects.add(new Project(name + i));
		}
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public List<Project> getSubProjects() {
		return this.subProjects;
	}

	public void setSubProjects(List<Project> subProjects) {
		this.subProjects = subProjects;
	}

	public String getName() {
		return this.name;
	}

	public String getUrl() {
		return this.url;
	}

}
