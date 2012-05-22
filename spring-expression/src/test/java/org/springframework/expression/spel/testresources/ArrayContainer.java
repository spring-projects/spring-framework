/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 package org.springframework.expression.spel.testresources;

/**
 * Hold the various kinds of primitive array for access through the test evaluation context.
 *
 * @author Andy Clement
 */
public class ArrayContainer {
	public int[] ints = new int[3];
	public long[] longs = new long[3];
	public double[] doubles = new double[3];
	public byte[] bytes = new byte[3];
	public char[] chars = new char[3];
	public short[] shorts = new short[3];
	public boolean[] booleans = new boolean[3];
	public float[] floats = new float[3];

	public ArrayContainer() {
		// setup some values
		ints[0] = 42;
		longs[0] = 42L;
		doubles[0] = 42.0d;
		bytes[0] = 42;
		chars[0] = 42;
		shorts[0] = 42;
		booleans[0] = true;
		floats[0] = 42.0f;
	}
}
