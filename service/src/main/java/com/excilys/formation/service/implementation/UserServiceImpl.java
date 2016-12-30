package com.excilys.formation.service.implementation;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.excilys.formation.exception.PersistenceException;
import com.excilys.formation.model.User;
import com.excilys.formation.model.util.PageFilter;
import com.excilys.formation.pagination.Page;
import com.excilys.formation.persistence.UserDao;
import com.excilys.formation.service.UserService;

import ch.qos.logback.classic.Logger;

@Service
public class UserServiceImpl implements UserService, UserDetailsService {
    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(UserServiceImpl.class);
    @Autowired
    private UserDao userDao;

    @Override
    public UserDetails loadUserByUsername(String pName) throws UsernameNotFoundException {
        User user = null;
        try {
            user =  userDao.getByName(pName);
        } catch (PersistenceException e) {
            LOGGER.error( "UserServiceImpl : loadUserByUsername() catched PersistenceException",e);
        }
        
        if (user == null) {
            throw new UsernameNotFoundException(String.format("User %s does not exist!", pName));
        }
        return user;
    }

    @Override
    public User getById(long pId) {
        try {
            return userDao.getById(pId);
        } catch (PersistenceException e) {
            LOGGER.error( "UserServiceImpl : getById() catched PersistenceException",e);
        }
        return null;
    }

    @Override
    public Page<User> getPage(PageFilter pPageFilter) {
        try {
            return userDao.getPage(pPageFilter);
        } catch (PersistenceException e) {
            LOGGER.error( "UserServiceImpl : getPage() catched PersistenceException",e);
        }
        return null;
    }
}