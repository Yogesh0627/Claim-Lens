package com.niyotechnologies.claimlens.auth.service;

import com.niyotechnologies.claimlens.auth.entity.UserInvitation;
import com.niyotechnologies.claimlens.auth.enums.InvitationPurpose;
import com.niyotechnologies.claimlens.auth.repository.UserInvitationRepository;
import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.notification.email.AccountEmailRenderer;
import com.niyotechnologies.claimlens.notification.email.EmailMessage;
import com.niyotechnologies.claimlens.notification.email.EmailSender;
import com.niyotechnologies.claimlens.user.entity.AppUser;
import com.niyotechnologies.claimlens.user.enums.UserStatus;
import com.niyotechnologies.claimlens.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Issues and redeems the single-use links that let a person set their own password — the invitation
 * sent when an admin creates their account, and the self-service password reset.
 *
 * <p>Why this exists: creating a user without a password produced an {@code INVITED} account that
 * could never sign in — no password to use, and Google sign-in requires {@code ACTIVE}. The status
 * was a dead end. Admins worked around it by typing a password and telling the person out of band,
 * which means the admin knows their credential.
 *
 * <p>Security choices, all mirroring {@code user_session}'s refresh-token handling:
 * <ul>
 *   <li>32 random bytes from {@link SecureRandom}, Base64URL — the raw token only ever exists in the
 *       emailed link.</li>
 *   <li>Only its SHA-256 hash is stored, so a database leak yields no usable links.</li>
 *   <li>Single use, and redeeming one burns every other outstanding token for that user.</li>
 *   <li>Reset links live an hour; invitations a week (someone may be onboarded before they read it).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration INVITE_TTL = Duration.ofDays(7);
    private static final Duration RESET_TTL = Duration.ofHours(1);

    @Autowired
    private final UserInvitationRepository invitationRepository;
    @Autowired
    private final AppUserRepository appUserRepository;
    @Autowired
    private final PasswordEncoder passwordEncoder;
    @Autowired
    private final EmailSender emailSender;
    @Autowired
    private final AccountEmailRenderer emailRenderer;

    /** Where the "set your password" link points — the frontend, not the API. */
    @Value("${claimlens.app.base-url:http://localhost:3000}")
    private String appBaseUrl;

    /**
     * Issue a token and email the link. Best-effort on the email: the account is already created, so
     * a mail failure must not roll the creation back — an admin can always re-send.
     */
    @Transactional
    public void invite(AppUser user, InvitationPurpose purpose) {
        String rawToken = issueToken(user, purpose);
        // URL-encode: the token is Base64URL so it is already safe, but encoding keeps this correct
        // if the format ever changes.
        String link = appBaseUrl + "/set-password?token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        try {
            emailSender.send(EmailMessage.of(
                    user.getEmail(),
                    emailRenderer.subject(purpose),
                    emailRenderer.text(user.getFirstName(), purpose, link),
                    emailRenderer.html(user.getFirstName(), purpose, link)));
        } catch (Exception e) {
            log.warn("Could not email the {} link to {}: {}", purpose, user.getEmail(), e.getMessage());
        }
    }

    /** Creates the token row and returns the RAW token (never stored). */
    @Transactional
    public String issueToken(AppUser user, InvitationPurpose purpose) {
        Instant now = Instant.now();
        // Anything previously outstanding becomes void the moment a new link is issued.
        invitationRepository.invalidateOutstanding(user.getId(), now);

        String rawToken = randomToken();
        UserInvitation invitation = new UserInvitation();
        invitation.setTenantId(user.getTenantId());
        invitation.setUserId(user.getId());
        invitation.setTokenHash(sha256Hex(rawToken));
        invitation.setPurpose(purpose);
        invitation.setExpiresAt(now.plus(purpose == InvitationPurpose.RESET ? RESET_TTL : INVITE_TTL));
        invitationRepository.save(invitation);
        return rawToken;
    }

    /**
     * Redeem a token and set the password. Anonymous call — the token itself is the credential, which
     * is why the lookup is not tenant-filtered.
     */
    @Transactional
    public void redeem(String rawToken, String newPassword) {
        Instant now = Instant.now();
        UserInvitation invitation = invitationRepository.findByTokenHash(sha256Hex(rawToken))
                .filter(i -> i.isRedeemable(now))
                // One error for missing / expired / already-used: never reveal which.
                .orElseThrow(() -> new BusinessException(
                        "INVALID_INVITATION", "This link is invalid or has expired"));

        // Native lookup + update: this request is anonymous, so there is no tenant in context and a
        // normal @TenantId-scoped query would match nothing. The bypass has to be in the SQL.
        AppUser user = appUserRepository.findByIdForAuthentication(invitation.getUserId())
                .orElseThrow(() -> new BusinessException(
                        "INVALID_INVITATION", "This link is invalid or has expired"));

        // Redeeming an invitation is what turns INVITED into a usable account. A suspended or
        // terminated account stays that way — a password link must never reinstate access.
        UserStatus status = user.getStatus() == UserStatus.INVITED ? UserStatus.ACTIVE : user.getStatus();
        if (status != UserStatus.ACTIVE) {
            throw new BusinessException("ACCOUNT_NOT_ACTIVE", "This account cannot be used");
        }
        appUserRepository.updateCredentialsForAuthentication(
                user.getId(), passwordEncoder.encode(newPassword), status.name());

        invitation.setUsedAt(now);
        invitationRepository.save(invitation);
        invitationRepository.invalidateOutstanding(user.getId(), now);
    }

    /**
     * Issue a reset link. Deliberately silent about whether the address exists — responding
     * differently would turn this into an account-enumeration oracle. Suspended and terminated
     * accounts get nothing.
     */
    @Transactional
    public void requestReset(String email) {
        appUserRepository.findByEmailForAuthentication(email)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE || u.getStatus() == UserStatus.INVITED)
                .ifPresent(u -> invite(u, InvitationPurpose.RESET));
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
