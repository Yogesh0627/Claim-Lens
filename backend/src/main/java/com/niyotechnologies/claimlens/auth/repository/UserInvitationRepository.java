package com.niyotechnologies.claimlens.auth.repository;

import com.niyotechnologies.claimlens.auth.entity.UserInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UserInvitationRepository extends JpaRepository<UserInvitation, Long> {

    /** Redemption is anonymous, so this is not (and must not be) tenant-filtered. */
    Optional<UserInvitation> findByTokenHash(String tokenHash);

    /**
     * Burn every other outstanding token for a user. Called on redemption so an older invitation or
     * a second "forgot password" link can't still be used afterwards.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserInvitation i SET i.usedAt = :now "
            + "WHERE i.userId = :userId AND i.usedAt IS NULL")
    int invalidateOutstanding(@Param("userId") Long userId, @Param("now") Instant now);
}
