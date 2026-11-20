/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.testfixture.xml;

import jakarta.xml.bind.annotation.XmlRootElement;
import org.jspecify.annotations.Nullable;

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
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof Pojo other) {
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
