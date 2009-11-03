package org.springframework.mock.staticmock;

import javax.persistence.Entity;

/**
 * Annotation-based aspect to use in test build to enable mocking static methods
 * on Entity classes, as used by Roo for finders.
 * <br>
 * Mocking will occur in the call stack of any method in a class (typically a test class) 
 * that is annotated with the @MockStaticEntityMethods annotation. 
 * <br>
 * Also provides static methods to simplify the programming model for
 * entering playback mode and setting expected return values.
 * <br>
 * Usage:<ol> 
 * <li>Annotate a test class with @MockStaticEntityMethods.
 * <li>In each test method, AnnotationDrivenStaticEntityMockingControl will begin in recording mode.
 * Invoke static methods on Entity classes, with each recording-mode invocation
 * being followed by an invocation to the static expectReturn() or expectThrow()
 * method on AnnotationDrivenStaticEntityMockingControl.
 * <li>Invoke the static AnnotationDrivenStaticEntityMockingControl() method.
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
public aspect AnnotationDrivenStaticEntityMockingControl extends AbstractMethodMockingControl {
	
	/**
	 * Stop recording mock calls and enter playback state
	 */
	public static void playback() {
		AnnotationDrivenStaticEntityMockingControl.aspectOf().playbackInternal();
	}
	
	public static void expectReturn(Object retVal) {
		AnnotationDrivenStaticEntityMockingControl.aspectOf().expectReturnInternal(retVal);
	}

	public static void expectThrow(Throwable throwable) {
		AnnotationDrivenStaticEntityMockingControl.aspectOf().expectThrowInternal(throwable);
	}

	// Only matches directly annotated @Test methods, to allow methods in
	// @MockStatics classes to invoke each other without resetting the mocking environment
	protected pointcut mockStaticsTestMethod() : execution(public * (@MockStaticEntityMethods *).*(..));

	protected pointcut methodToMock() : execution(public static * (@Entity *).*(..));

}
