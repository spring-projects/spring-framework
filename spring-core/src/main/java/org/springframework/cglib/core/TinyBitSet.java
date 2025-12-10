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
package org.springframework.cglib.core;

@Deprecated
public class TinyBitSet {
    private static final int[] T = new int[256];
    private int value = 0;

    private static int gcount(int x) {
        int c = 0;
        while (x != 0) {
            c++;
            x &= (x - 1);
        }
        return c;
    }

    static {
        for (int j = 0; j < 256; j++) {
            T[j] = gcount(j);
        }
    }

    private static int topbit(int i) {
        int j;
        for (j = 0; i != 0; i ^= j) {
            j = i & -i;
        }
        return j;
    }

    private static int log2(int i) {
        int j = 0;
        for (j = 0; i != 0; i >>= 1) {
            j++;
        }
        return j;
    }

    public int length() {
        return log2(topbit(value));
    }

    /**
     * If bit 31 is set then this method results in an infinite loop.
     *
     * @return the number of bits set to <code>true</code> in this TinyBitSet.
     */
    public int cardinality() {
        int w = value;
        int c = 0;
        while (w != 0) {
            c += T[w & 255];
            w >>= 8;
        }
        return c;
    }

    public boolean get(int index) {
        return (value & (1 << index)) != 0;
    }

    public void set(int index) {
        value |= (1 << index);
    }

    public void clear(int index) {
        value &= ~(1 << index);
    }
}
