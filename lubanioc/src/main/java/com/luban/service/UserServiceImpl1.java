package com.luban.service;

import com.luban.anno.Luban;
import com.luban.dao.UserDao;

@Luban("AA")
public class UserServiceImpl1 implements UserService {

    
    UserDao dao;

    @Override
    public void find() {
        System.out.println("service");
        dao.query();
    }

    //public void setDao(UserDao dao) {
       // this.dao = dao;
   // }
}
