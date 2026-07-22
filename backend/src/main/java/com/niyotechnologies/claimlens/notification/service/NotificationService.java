package com.niyotechnologies.claimlens.notification.service;

import com.niyotechnologies.claimlens.common.exception.NotFoundException;
import com.niyotechnologies.claimlens.common.exception.UnauthorizedException;
import com.niyotechnologies.claimlens.notification.dto.NotificationResponse;
import com.niyotechnologies.claimlens.notification.email.EmailSender;
import com.niyotechnologies.claimlens.notification.entity.Notification;
import com.niyotechnologies.claimlens.notification.report.ClaimEmailEvent;
import com.niyotechnologies.claimlens.notification.report.ClaimReportContext;
import com.niyotechnologies.claimlens.notification.report.ClaimReportService;
import com.niyotechnologies.claimlens.notification.repository.NotificationRepository;
import com.niyotechnologies.claimlens.security.model.ClaimLensPrincipal;
import com.niyotechnologies.claimlens.user.entity.AppUser;
import com.niyotechnologies.claimlens.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * In-app notifications, plus a best-effort email copy through the swappable {@link EmailSender}
 * (logs by default, real SMTP when {@code claimlens.email.provider=smtp}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    @Autowired
    private final NotificationRepository notificationRepository;
    @Autowired
    private final AppUserRepository appUserRepository;
    @Autowired
    private final EmailSender emailSender;
    @Autowired
    private final ClaimReportService reportService;

    /** Off in tests so the email path stays plain-text/log; on in dev + prod for branded HTML + PDF. */
    @Value("${claimlens.email.rich.enabled:true}")
    private boolean richEmailEnabled;

    /** Called internally (e.g. on assignment). Tenant is stamped by @TenantId. */
    @Transactional
    public void notify(Long recipientUserId, String type, String title, String message) {
        saveInApp(recipientUserId, type, title, message);
        emailRecipient(recipientUserId, title, message);
    }

    /**
     * Like {@link #notify} but the email copy is the rich branded report (HTML + PDF) built from
     * {@code ctx}. Falls back to the plain-text email when rich rendering is disabled (tests) or the
     * context is missing. The in-app notification is unchanged (short title + message).
     */
    @Transactional
    public void notifyClaimEvent(Long recipientUserId, String type, String title, String message,
                                 ClaimEmailEvent event, ClaimReportContext ctx) {
        saveInApp(recipientUserId, type, title, message);
        if (!richEmailEnabled || ctx == null) {
            emailRecipient(recipientUserId, title, message);
            return;
        }
        try {
            String email = appUserRepository.findByIdAndIsDeletedFalse(recipientUserId)
                    .map(AppUser::getEmail).orElse(null);
            if (email != null && !email.isBlank()) {
                emailSender.send(reportService.buildEmail(email, event, ctx));
            }
        } catch (Exception e) {
            log.warn("Failed to dispatch rich claim email to user {}: {}", recipientUserId, e.getMessage());
        }
    }

    private void saveInApp(Long recipientUserId, String type, String title, String message) {
        Notification notification = new Notification();
        notification.setRecipientUserId(recipientUserId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notificationRepository.save(notification);
    }

    /**
     * Fire the email copy. The recipient lookup is @TenantId-scoped (same tenant as the caller), and
     * the whole thing is swallowed on failure — email must never break the notification write.
     */
    private void emailRecipient(Long recipientUserId, String title, String message) {
        try {
            String email = appUserRepository.findByIdAndIsDeletedFalse(recipientUserId)
                    .map(AppUser::getEmail).orElse(null);
            if (email != null && !email.isBlank()) {
                emailSender.send(email, title, message);
            }
        } catch (Exception e) {
            log.warn("Failed to dispatch email for notification to user {}: {}", recipientUserId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> myNotifications() {
        return notificationRepository.findAllByRecipientUserIdOrderByCreatedAtDesc(currentUserId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void markRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("NOTIFICATION_NOT_FOUND", "Notification not found"));
        // A user can only mark their own notification (do not leak others' via a 403).
        if (!notification.getRecipientUserId().equals(currentUserId())) {
            throw new NotFoundException("NOTIFICATION_NOT_FOUND", "Notification not found");
        }
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof ClaimLensPrincipal principal) {
            return principal.userId();
        }
        throw new UnauthorizedException("UNAUTHENTICATED", "No authenticated user");
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                n.getIsRead(), n.getCreatedAt());
    }
}
