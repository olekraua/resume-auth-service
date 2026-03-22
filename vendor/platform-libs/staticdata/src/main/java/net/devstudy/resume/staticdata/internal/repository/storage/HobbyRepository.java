package net.devstudy.resume.staticdata.internal.repository.storage;

import org.springframework.data.jpa.repository.JpaRepository;

import net.devstudy.resume.staticdata.api.model.Hobby;

public interface HobbyRepository extends JpaRepository<Hobby, Long> {
}
