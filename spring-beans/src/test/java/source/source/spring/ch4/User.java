package source.source.spring.ch4;

public class User {
	private String userName;
	private String email;

	public String getUserName() {
		return userName;
	}

	public void setUserName(String username) {
		this.userName = username;
	}

	public String getEmail() {
		return email;
	}

	@Override
	public String toString() {
		return "User [username=" + userName + ", email=" + email + "]";
	}

	public void setEmail(String email) {
		this.email = email;
	}
}
