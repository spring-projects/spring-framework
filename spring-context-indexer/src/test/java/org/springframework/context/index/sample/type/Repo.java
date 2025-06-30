/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.context.index.sample.type;

import org.springframework.stereotype.Indexed;

/**
 * A sample interface flagged with {@link Indexed} to indicate that a stereotype
 * for all implementations should be added to the index.
 *
 * @author Stephane Nicoll
 */
@Indexed
public interface Repo<T, I> {
}
