/**
 * 
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