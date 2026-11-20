/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http;

import java.io.Serializable;

import org.jspecify.annotations.Nullable;

/**
 * Default implementation of {@link HttpStatusCode}.
 *
 * @author Arjen Poutsma
 * @since 6.0
 */
final class DefaultHttpStatusCode implements HttpStatusCode, Comparable<HttpStatusCode>, Serializable {

	private static final long serialVersionUID = 7017664779360718111L;

	private final int value;


	public DefaultHttpStatusCode(int value) {
		this.value = value;
	}

	@Override
	public int value() {
		return this.value;
	}

	@Override
	public boolean is1xxInformational() {
		return hundreds() == 1;
	}

	@Override
	public boolean is2xxSuccessful() {
		return hundreds() == 2;
	}

	@Override
	public boolean is3xxRedirection() {
		return hundreds() == 3;
	}

	@Override
	public boolean is4xxClientError() {
		return hundreds() == 4;
	}

	@Override
	public boolean is5xxServerError() {
		return hundreds() == 5;
	}

	@Override
	public boolean isError() {
		int hundreds = hundreds();
		return hundreds == 4 || hundreds == 5;
	}

	private int hundreds() {
		return this.value / 100;
	}


	@Override
	public int compareTo(HttpStatusCode other) {
		return Integer.compare(this.value, other.value());
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof HttpStatusCode that && this.value == that.value()));
	}

	@Override
	public int hashCode() {
		return this.value;
	}

	@Override
	public String toString() {
		return Integer.toString(this.value);
	}

}
