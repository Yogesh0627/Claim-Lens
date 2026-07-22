package com.niyotechnologies.claimlens.auth.service;

import com.niyotechnologies.claimlens.common.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Set;

/**
 * Verifies a Google ID token (the "credential" the frontend gets from Google Sign-In) and returns the
 * identity it asserts. Uses Google's tokeninfo endpoint, which validates the signature and expiry;
 * we then check the audience (must be OUR client id), the issuer, and that the email is verified.
 *
 * V1 simplicity: tokeninfo is one HTTP call per login (fine at this scale). Hardening option: verify
 * the RS256 signature locally against Google's JWKS to avoid the round-trip.
 */
@Component
public class GoogleTokenVerifier {

    private static final Set<String> VALID_ISSUERS =
            Set.of("accounts.google.com", "https://accounts.google.com");

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://oauth2.googleapis.com")
            .build();

    private final String clientId;

    public GoogleTokenVerifier(@Value("${claimlens.auth.google.client-id:}") String clientId) {
        this.clientId = clientId;
    }

    @SuppressWarnings("unchecked")
    public GoogleIdentity verify(String idToken) {
        if (!StringUtils.hasText(clientId)) {
            throw new UnauthorizedException("GOOGLE_LOGIN_DISABLED", "Google sign-in is not configured");
        }

        Map<String, Object> claims;
        try {
            claims = restClient.get()
                    .uri(uri -> uri.path("/tokeninfo").queryParam("id_token", idToken).build())
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            throw invalid();
        }
        if (claims == null) {
            throw invalid();
        }

        if (!clientId.equals(String.valueOf(claims.get("aud")))) {
            throw invalid();
        }
        if (!VALID_ISSUERS.contains(String.valueOf(claims.get("iss")))) {
            throw invalid();
        }
        String email = (String) claims.get("email");
        if (!StringUtils.hasText(email) || !isTrue(claims.get("email_verified"))) {
            throw new UnauthorizedException("GOOGLE_EMAIL_UNVERIFIED", "Google email is not verified");
        }

        return new GoogleIdentity(email, String.valueOf(claims.get("sub")), (String) claims.get("name"));
    }

    private static boolean isTrue(Object value) {
        return value instanceof Boolean b ? b : "true".equalsIgnoreCase(String.valueOf(value));
    }

    private static UnauthorizedException invalid() {
        return new UnauthorizedException("INVALID_GOOGLE_TOKEN", "Invalid Google credential");
    }

    public record GoogleIdentity(String email, String subject, String name) {
    }
}
