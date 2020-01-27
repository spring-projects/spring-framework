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

package org.springframework.beans.factory.parsing;

import java.util.LinkedList;

import org.springframework.lang.Nullable;

/**
 * Simple {@link LinkedList}-based structure for tracking the logical position during
 * a parsing process. {@link Entry entries} are added to the LinkedList at
 * each point during the parse phase in a reader-specific manner.
 *
 * <p>Calling {@link #toString()} will render a tree-style view of the current logical
 * position in the parse phase. This representation is intended for use in
 * error messages.
 *
 * @author Rob Harrop
 * @since 2.0
 */
public final class ParseState {

	/**
	 * Tab character used when rendering the tree-style representation.
	 */
	private static final char TAB = '\t';

	/**
	 * Internal {@link LinkedList} storage.
	 */
	private final LinkedList<Entry> state;


	/**
	 * Create a new {@code ParseState} with an empty {@link LinkedList}.
	 */
	public ParseState() {
		this.state = new LinkedList<>();
	}

	/**
	 * Create a new {@code ParseState} whose {@link LinkedList} is a {@link Object#clone clone}
	 * of that of the passed in {@code ParseState}.
	 */
	@SuppressWarnings("unchecked")
	private ParseState(ParseState other) {
		this.state = (LinkedList<Entry>) other.state.clone();
	}


	/**
	 * Add a new {@link Entry} to the {@link LinkedList}.
	 */
	public void push(Entry entry) {
		this.state.push(entry);
	}

	/**
	 * Remove an {@link Entry} from the {@link LinkedList}.
	 */
	public void pop() {
		this.state.pop();
	}

	/**
	 * Return the {@link Entry} currently at the top of the {@link LinkedList} or
	 * {@code null} if the {@link LinkedList} is empty.
	 */
	@Nullable
	public Entry peek() {
		return this.state.peek();
	}

	/**
	 * Create a new instance of {@link ParseState} which is an independent snapshot
	 * of this instance.
	 */
	public ParseState snapshot() {
		return new ParseState(this);
	}


	/**
	 * Returns a tree-style representation of the current {@code ParseState}.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int x = 0; x < this.state.size(); x++) {
			if (x > 0) {
				sb.append('\n');
				for (int y = 0; y < x; y++) {
					sb.append(TAB);
				}
				sb.append("-> ");
			}
			sb.append(this.state.get(x));
		}
		return sb.toString();
	}


	/**
	 * Marker interface for entries into the {@link ParseState}.
	 */
	public interface Entry {

	}

}
