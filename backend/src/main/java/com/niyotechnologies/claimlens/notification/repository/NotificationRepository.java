package com.niyotechnologies.claimlens.notification.repository;

import com.niyotechnologies.claimlens.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Tenant-scoped by @TenantId. */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);
}
