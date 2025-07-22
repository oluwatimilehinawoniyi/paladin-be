package com.paladin.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProfileRepository
        extends JpaRepository<Profile, UUID> {
    List<Profile> findByUserId(UUID userId);
    List<Profile> findAllByCvId(UUID cvId);
}
