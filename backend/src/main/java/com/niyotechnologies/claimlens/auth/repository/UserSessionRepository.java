package com.niyotechnologies.claimlens.auth.repository;

import com.niyotechnologies.claimlens.auth.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    // Not tenant-filtered (UserSession has no @TenantId) — refresh happens before authentication.
    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);
}
