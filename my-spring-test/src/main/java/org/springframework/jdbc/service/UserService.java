package org.springframework.jdbc.service;

import org.springframework.jdbc.model.User;

import java.util.List;

/**
 * @author sushuaiqiang
 * @date 2024/12/25 - 15:36
 */
public interface UserService {

	void save(User user);

	List<User> getUsers();

	List<User> getUserByAge();
}
