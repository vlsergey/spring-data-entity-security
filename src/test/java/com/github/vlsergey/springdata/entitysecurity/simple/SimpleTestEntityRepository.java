package com.github.vlsergey.springdata.entitysecurity.simple;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SimpleTestEntityRepository extends JpaRepository<SimpleTestEntity, String> {

}
