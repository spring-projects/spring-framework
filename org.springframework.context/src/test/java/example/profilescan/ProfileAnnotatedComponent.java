package example.profilescan;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile(ProfileAnnotatedComponent.PROFILE_NAME)
@Component(ProfileAnnotatedComponent.BEAN_NAME)
public class ProfileAnnotatedComponent {

	public static final String BEAN_NAME = "profileAnnotatedComponent";
	public static final String PROFILE_NAME = "test";

}
