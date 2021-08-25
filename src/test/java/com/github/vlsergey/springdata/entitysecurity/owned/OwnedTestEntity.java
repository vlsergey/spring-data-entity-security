package com.github.vlsergey.springdata.entitysecurity.owned;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
public class OwnedTestEntity {

	@Id
	private UUID id;

	private int value;

	private String owner;

}
