package org.springframework.aop;

/**
 * @author sushuaiqiang
 * @date 2024/7/8 - 10:49
 */
public class AopBean {

	private String testStr = "testStr";

	public String getTestStr() {
		return testStr;
	}

	public void setTestStr(String testStr) {
		this.testStr = testStr;
	}

	public void test(){
		System.out.println(this.testStr);
	}

}
