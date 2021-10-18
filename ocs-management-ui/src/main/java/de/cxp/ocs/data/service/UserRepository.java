package de.cxp.ocs.data.service;

import org.springframework.data.jpa.repository.JpaRepository;

import de.cxp.ocs.data.entity.User;

public interface UserRepository extends JpaRepository<User, Integer> {

	User findByUsername(String username);
}
