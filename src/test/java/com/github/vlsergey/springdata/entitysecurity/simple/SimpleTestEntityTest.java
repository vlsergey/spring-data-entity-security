package com.github.vlsergey.springdata.entitysecurity.simple;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@ContextConfiguration(classes = TestConfiguration.class)
class SimpleTestEntityTest {

	@Autowired
	private SimpleTestEntityRepository testRepository;

	@Test
	void testThatNewEntityIsDeletedNotOldOneOnIdChange() {
		SimpleTestEntity old = new SimpleTestEntity();
		old.setId("oldId");
		old.setValue(42);
		old = testRepository.save(old);
		testRepository.flush();

		SimpleTestEntity newEntity = new SimpleTestEntity();
		newEntity.setId("newId");
		newEntity.setValue(84);
		newEntity = testRepository.save(newEntity);
		testRepository.flush();

		newEntity.setId(old.getId());

		testRepository.delete(newEntity);
		testRepository.flush();

		assertTrue(testRepository.existsById(old.getId()));
	}

}
