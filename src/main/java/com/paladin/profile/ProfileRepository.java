package com.paladin.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository
        extends JpaRepository<Profile, UUID> {
    Optional<List<Profile>> findByUserId(UUID userId);

    Optional<List<Profile>> findAllByCvId(UUID cvId);

    Optional<Profile> findByCvId(UUID cvId);
}
