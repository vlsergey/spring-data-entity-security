package io.github.vlsergey.springdata.entitysecurity.noquerydsl;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@Table
@ToString
public class UserTestEntity {

	@Id
	private Long uid;

	private String login;

	@ManyToMany
	@ToString.Exclude
	private Set<GroupTestEntity> groups;

}
