package com.github.vlsergey.springdata.entitysecurity.noquerydsl;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
public class FileTestEntity {

	@Id
	private String path;

	private String permissions;

	@ManyToOne
	private UserTestEntity ownerUser;

	@ManyToOne
	private GroupTestEntity ownerGroup;

}
