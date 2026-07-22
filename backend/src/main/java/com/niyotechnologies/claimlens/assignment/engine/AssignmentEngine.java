package com.niyotechnologies.claimlens.assignment.engine;

import com.niyotechnologies.claimlens.assignment.enums.AssignmentStatus;
import com.niyotechnologies.claimlens.assignment.repository.ClaimAssignmentRepository;
import com.niyotechnologies.claimlens.common.exception.BusinessException;
import com.niyotechnologies.claimlens.role.repository.RoleRepository;
import com.niyotechnologies.claimlens.user.entity.AppUser;
import com.niyotechnologies.claimlens.user.enums.UserStatus;
import com.niyotechnologies.claimlens.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Chooses an investigator for a claim. Runs inside a request, so @TenantId scopes the candidate users
 * to the current tenant automatically. V1 strategy: least-loaded (fewest active assignments). Region /
 * branch / on-leave filters are refinements deferred to the assignment ruleset.
 */
@Service
@RequiredArgsConstructor
public class AssignmentEngine {

    private static final String INVESTIGATOR_ROLE = "INVESTIGATOR";

    @Autowired
    private final RoleRepository roleRepository;
    @Autowired
    private final AppUserRepository appUserRepository;
    @Autowired
    private final ClaimAssignmentRepository assignmentRepository;

    /** @return the id of the least-loaded active investigator in this tenant. */
    public Long pickInvestigator() {
        Long investigatorRoleId = roleRepository.findByCodeAndIsDeletedFalse(INVESTIGATOR_ROLE)
                .orElseThrow(() -> new BusinessException("INVESTIGATOR_ROLE_MISSING",
                        "Investigator role is not configured"))
                .getId();

        List<AppUser> eligible = appUserRepository
                .findAllByRoleIdAndStatusAndIsDeletedFalse(investigatorRoleId, UserStatus.ACTIVE);
        if (eligible.isEmpty()) {
            throw new BusinessException("NO_ELIGIBLE_INVESTIGATOR",
                    "No active investigator available for assignment");
        }

        return eligible.stream()
                .min(Comparator.comparingLong(this::activeWorkload))
                .map(AppUser::getId)
                .orElseThrow();
    }

    private long activeWorkload(AppUser investigator) {
        return assignmentRepository
                .countByInvestigatorUserIdAndStatus(investigator.getId(), AssignmentStatus.ASSIGNED);
    }
}
