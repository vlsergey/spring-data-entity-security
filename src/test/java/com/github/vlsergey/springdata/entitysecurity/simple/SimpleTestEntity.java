package com.github.vlsergey.springdata.entitysecurity.simple;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
public class SimpleTestEntity {

	@Id
	private String id;

	private int value;

}
