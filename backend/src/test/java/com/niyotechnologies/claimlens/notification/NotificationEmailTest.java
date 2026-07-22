package com.niyotechnologies.claimlens.notification;

import com.niyotechnologies.claimlens.notification.email.EmailSender;
import com.niyotechnologies.claimlens.notification.entity.Notification;
import com.niyotechnologies.claimlens.notification.repository.NotificationRepository;
import com.niyotechnologies.claimlens.notification.service.NotificationService;
import com.niyotechnologies.claimlens.user.entity.AppUser;
import com.niyotechnologies.claimlens.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

/**
 * The notification write and the email copy are two channels behind one call. This proves the email
 * fires to the recipient's address — and that a broken email sender never stops the notification.
 */
@ExtendWith(MockitoExtension.class)
class NotificationEmailTest {

    @Mock NotificationRepository notificationRepository;
    @Mock AppUserRepository appUserRepository;
    @Mock EmailSender emailSender;
    @InjectMocks NotificationService notificationService;

    @Test
    void notifyAlsoEmailsTheRecipient() {
        AppUser user = new AppUser();
        user.setEmail("investigator@demo.co");
        when(appUserRepository.findByIdAndIsDeletedFalse(7L)).thenReturn(Optional.of(user));

        notificationService.notify(7L, "CLAIM_ASSIGNED", "New claim", "Claim CLM-1 assigned to you");

        verify(notificationRepository).save(any(Notification.class));
        verify(emailSender).send("investigator@demo.co", "New claim", "Claim CLM-1 assigned to you");
    }

    @Test
    void emailFailureDoesNotBreakTheNotification() {
        AppUser user = new AppUser();
        user.setEmail("investigator@demo.co");
        when(appUserRepository.findByIdAndIsDeletedFalse(7L)).thenReturn(Optional.of(user));
        doThrow(new RuntimeException("smtp down")).when(emailSender).send(any(), any(), any());

        // Must not throw — the in-app notification is still persisted.
        notificationService.notify(7L, "CLAIM_ASSIGNED", "New claim", "body");

        verify(notificationRepository).save(any(Notification.class));
    }
}
