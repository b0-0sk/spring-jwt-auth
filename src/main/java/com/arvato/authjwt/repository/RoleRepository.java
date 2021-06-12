package com.arvato.authjwt.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.arvato.authjwt.models.ERole;
import com.arvato.authjwt.models.Role;

@Repository
public interface RoleRepository {
	
	Optional<Role> findByName(ERole name);

}
