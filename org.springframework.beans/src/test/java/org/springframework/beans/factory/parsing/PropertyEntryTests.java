package org.springframework.beans.factory.parsing;

import org.junit.Test;

/**
 * Unit tests for the {@link PropertyEntry} class.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public final class PropertyEntryTests {

	@Test(expected=IllegalArgumentException.class)
	public void testCtorBailsOnNullPropertyNameArgument() throws Exception {
		new PropertyEntry(null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCtorBailsOnEmptyPropertyNameArgument() throws Exception {
		new PropertyEntry("");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCtorBailsOnWhitespacedPropertyNameArgument() throws Exception {
		new PropertyEntry("\t   ");
	}

}
