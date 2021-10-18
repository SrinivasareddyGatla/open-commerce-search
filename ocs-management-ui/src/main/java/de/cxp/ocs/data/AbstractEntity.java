package de.cxp.ocs.data;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import com.vaadin.fusion.Nonnull;

import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class AbstractEntity {

	@Id
	@GeneratedValue
	@Nonnull
	@Getter
	@Setter
	@Include
	private Integer id;

}
