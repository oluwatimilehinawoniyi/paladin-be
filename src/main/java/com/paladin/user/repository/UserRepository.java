package com.paladin.user.repository;

import com.paladin.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository
        extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.accessTokenExpiry IS NOT NULL AND u.accessTokenExpiry <= :expiryThreshold AND u.refreshToken IS NOT NULL")
    List<User> findUsersWithExpiringTokens(LocalDateTime expiryThreshold);
}
