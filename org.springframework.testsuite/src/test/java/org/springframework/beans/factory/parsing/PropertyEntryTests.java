package org.springframework.beans.factory.parsing;

import junit.framework.TestCase;
import org.springframework.test.AssertThrows;

/**
 * Unit tests for the {@link PropertyEntry} class.
 *
 * @author Rick Evans
 */
public final class PropertyEntryTests extends TestCase {

	public void testCtorBailsOnNullPropertyNameArgument() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				new PropertyEntry(null);
			}
		}.runTest();
	}

	public void testCtorBailsOnEmptyPropertyNameArgument() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				new PropertyEntry("");
			}
		}.runTest();
	}

	public void testCtorBailsOnWhitespacedPropertyNameArgument() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				new PropertyEntry("\t   ");
			}
		}.runTest();
	}

}
