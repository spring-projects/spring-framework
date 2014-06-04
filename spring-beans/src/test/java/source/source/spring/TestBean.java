package source.source.spring;

public class TestBean {

	private String name = "bruce";
	private String mail = "gmail";

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public TestBean(String name, String mail) {
		super();
		this.name = name;
		this.mail = mail;
	}

	public TestBean() {
		super();
	}

}
