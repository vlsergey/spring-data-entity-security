package com.github.vlsergey.springdata.entitysecurity.bughhh14815;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
public class FirstEntity {

	@Id
	private String firstId;

	private int firstValue;

}
