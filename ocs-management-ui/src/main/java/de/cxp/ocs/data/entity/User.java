package de.cxp.ocs.data.entity;

import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.cxp.ocs.data.AbstractEntity;
import de.cxp.ocs.data.Role;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class User extends AbstractEntity {

	private String		username;
	private String		name;
	@JsonIgnore
	private String		hashedPassword;
	@ElementCollection(fetch = FetchType.EAGER)
	private Set<Role>	roles;
	@Lob
	private String		profilePictureUrl;
}
