package io.github.vlsergey.springdata.entitysecurity.noquerydsl;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTestEntityRepository extends JpaRepository<UserTestEntity, Long> {

}
