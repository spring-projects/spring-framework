package org.springframework.beans.factory.xml;
/**
 * Bean that changes state on a business invocation, so that
 * we can check whether it's been invoked
 * @author Rod Johnson
 */
public class SideEffectBean {
	
	private int count;
	
	public void setCount(int count) {
		this.count = count;
	}
	
	public int getCount() {
		return this.count;
	}
	
	public void doWork() {
		++count;
	}

}