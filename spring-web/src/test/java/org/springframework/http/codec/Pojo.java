/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.http.codec;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Sebastien Deleuze
 */
@XmlRootElement
public class Pojo {

	private String foo;

	private String bar;

	public Pojo() {
	}

	public Pojo(String foo, String bar) {
		this.foo = foo;
		this.bar = bar;
	}

	public String getFoo() {
		return this.foo;
	}

	public void setFoo(String foo) {
		this.foo = foo;
	}

	public String getBar() {
		return this.bar;
	}

	public void setBar(String bar) {
		this.bar = bar;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof Pojo) {
			Pojo other = (Pojo) o;
			return this.foo.equals(other.foo) && this.bar.equals(other.bar);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 31 * foo.hashCode() + bar.hashCode();
	}

	@Override
	public String toString() {
		return "Pojo[foo='" + this.foo + "\'" + ", bar='" + this.bar + "\']";
	}
}
