package org.springframework.mock.static_mock;

import javax.persistence.Entity;
import org.junit.Test;

/**
 * JUnit-specific aspect to use in test build to enable mocking static methods
 * on Entity classes, as used by Roo for finders.
 * <br>
 * Mocking will occur in JUnit tests where the Test class is annotated with the
 * @MockStaticEntityMethods annotation, in the call stack of each
 * JUnit @Test method. 
 * <br>
 * Also provides static methods to simplify the programming model for
 * entering playback mode and setting expected return values.
 * <br>
 * Usage:<ol> 
 * <li>Annotate a JUnit test class with @MockStaticEntityMethods.
 * <li>In each @Test method, JUnitMockControl will begin in recording mode.
 * Invoke static methods on Entity classes, with each recording-mode invocation
 * being followed by an invocation to the static expectReturn() or expectThrow()
 * method on JUnitMockControl.
 * <li>Invoke the static JUnitMockControl.playback() method.
 * <li>Call the code you wish to test that uses the static methods. Verification will
 * occur automatically.
 * </ol>
 * 
 * @see MockStaticEntityMethods
 * 
 * @author Rod Johnson
 * @author Ramnivas Laddad
 *
 */
public aspect JUnitStaticEntityMockingControl extends AbstractMethodMockingControl {
	
	/**
	 * Stop recording mock calls and enter playback state
	 */
	public static void playback() {
		JUnitStaticEntityMockingControl.aspectOf().playbackInternal();
	}
	
	public static void expectReturn(Object retVal) {
		JUnitStaticEntityMockingControl.aspectOf().expectReturnInternal(retVal);
	}

	public static void expectThrow(Throwable throwable) {
		JUnitStaticEntityMockingControl.aspectOf().expectThrowInternal(throwable);
	}

	// Only matches directly annotated @Test methods, to allow methods in
	// @MockStatics classes to invoke each other without resetting the mocking environment
	protected pointcut mockStaticsTestMethod() : execution(@Test public * (@MockStaticEntityMethods *).*(..));

	protected pointcut methodToMock() : execution(public static * (@Entity *).*(..));

}
