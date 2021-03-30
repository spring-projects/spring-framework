package cglib;

/**
 * @Author mayf
 * @Date 2021/3/29 23:23
 */
public class SampleBean {
	private String value;

	public SampleBean() {
	}

	public SampleBean(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
