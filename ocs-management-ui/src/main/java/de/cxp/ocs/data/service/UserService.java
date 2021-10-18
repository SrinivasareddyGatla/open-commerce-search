package de.cxp.ocs.data.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.vaadin.artur.helpers.CrudService;

import de.cxp.ocs.data.entity.User;
import lombok.Getter;

@Service
public class UserService extends CrudService<User, Integer> {

	@Getter
    private UserRepository repository;

    public UserService(@Autowired UserRepository repository) {
        this.repository = repository;
    }
}
