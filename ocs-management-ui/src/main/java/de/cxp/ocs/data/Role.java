package de.cxp.ocs.data;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum Role {

	USER("user"), ADMIN("admin");

	@Getter
	private String roleName;
}
