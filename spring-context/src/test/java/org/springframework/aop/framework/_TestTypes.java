package org.springframework.aop.framework;

/**
 * Definitions of testing types for use in within this package.
 * Wherever possible, test types should be defined local to the java
 * file that makes use of them.  In some cases however, a test type may
 * need to be shared across tests.  Such types reside here, with the
 * intention of reducing the surface area of java files within this
 * package.  This allows developers to think about tests first, and deal
 * with these second class testing artifacts on an as-needed basis.
 *
 * Types here should be defined as package-private top level classes in
 * order to avoid needing to fully qualify, e.g.: _TestTypes$Foo.
 *
 * @author Chris Beams
 */
final class _TestTypes { }


interface IEcho {
	int echoException(int i, Throwable t) throws Throwable;
	int getA();
	void setA(int a);
}


class Echo implements IEcho {
	private int a;

	@Override
	public int echoException(int i, Throwable t) throws Throwable {
		if (t != null)
			throw t;
		return i;
	}
	@Override
	public void setA(int a) {
		this.a = a;
	}
	@Override
	public int getA() {
		return a;
	}
}