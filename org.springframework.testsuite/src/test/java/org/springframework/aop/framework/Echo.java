/**
 * 
 */
package org.springframework.aop.framework;

class Echo implements IEcho {
	private int a;
	
	public int echoException(int i, Throwable t) throws Throwable {
		if (t != null)
			throw t;
		return i;
	}
	public void setA(int a) {
		this.a = a;
	}
	public int getA() {
		return a;
	}
}