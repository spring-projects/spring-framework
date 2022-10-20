/*
 * Copyright 2003 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cglib.beans;

import java.util.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public /* need it for class loading  */ class FixedKeySet extends AbstractSet {
    private Set set;
    private int size;

    public FixedKeySet(String[] keys) {
        size = keys.length;
        set = Collections.unmodifiableSet(new HashSet(Arrays.asList(keys)));
    }

    public Iterator iterator() {
        return set.iterator();
    }

    public int size() {
        return size;
    }
}
