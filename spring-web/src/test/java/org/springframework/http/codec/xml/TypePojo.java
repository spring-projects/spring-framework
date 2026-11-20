/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.xml;

import org.jspecify.annotations.Nullable;

/**
 * @author Arjen Poutsma
 */
@jakarta.xml.bind.annotation.XmlType(name = "pojo")
public class TypePojo {

	private String foo;

	private String bar;

	public TypePojo() {
	}

	public TypePojo(String foo, String bar) {
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
		if (o instanceof TypePojo other) {
			return this.foo.equals(other.foo) && this.bar.equals(other.bar);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = this.foo.hashCode();
		result = 31 * result + this.bar.hashCode();
		return result;
	}
}
