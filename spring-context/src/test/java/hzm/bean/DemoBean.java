package hzm.bean;

/**
 * @author Hezeming
 * @version 1.0
 * @date 2020年10月26日
 */
public class DemoBean {

	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void say() {
		System.out.println(name);
	}
}
