package com.luban.dao;

import org.springframework.stereotype.Repository;

@Repository
public class UserDaoImpl implements UserDao {
    @Override
    public void query() {
        System.out.println("dao");
    }
}
