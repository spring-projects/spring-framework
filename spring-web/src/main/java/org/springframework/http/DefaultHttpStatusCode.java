/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.http;

import java.io.Serializable;

import org.jspecify.annotations.Nullable;

/**
 * Default implementation of {@link HttpStatusCode}.
 *
 * @author Arjen Poutsma
 * @author Mengqi Xu
 * @since 6.0
 */
final class DefaultHttpStatusCode implements HttpStatusCode, Comparable<HttpStatusCode>, Serializable {

	private static final long serialVersionUID = 7017664779360718111L;

	private final int value;

	private final HttpStatus.Series series;


	public DefaultHttpStatusCode(int value) {
		this.value = value;
		this.series = HttpStatus.Series.resolve(value);
	}

	@Override
	public int value() {
		return this.value;
	}

	@Override
	public boolean is1xxInformational() {
		return series() == HttpStatus.Series.INFORMATIONAL;
	}

	@Override
	public boolean is2xxSuccessful() {
		return series() == HttpStatus.Series.SUCCESSFUL;
	}

	@Override
	public boolean is3xxRedirection() {
		return series() == HttpStatus.Series.REDIRECTION;
	}

	@Override
	public boolean is4xxClientError() {
		return series() == HttpStatus.Series.CLIENT_ERROR;
	}

	@Override
	public boolean is5xxServerError() {
		return series() == HttpStatus.Series.SERVER_ERROR;
	}

	@Override
	public boolean isError() {
		return (is4xxClientError() || is5xxServerError());
	}

	/**
	 * Return the HTTP status series of this status code.
	 * @see HttpStatus.Series
	 */
	private HttpStatus.Series series() {
		return this.series;
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
