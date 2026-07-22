package com.niyotechnologies.claimlens.notification.controller;

import com.niyotechnologies.claimlens.common.response.ApiResponse;
import com.niyotechnologies.claimlens.notification.dto.NotificationResponse;
import com.niyotechnologies.claimlens.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** A user's own notifications — any authenticated user, scoped to themselves. */
@RestController
@RequestMapping("${claimlens.api.base-path}/notifications")
@RequiredArgsConstructor
public class NotificationController {

    @Autowired
    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<NotificationResponse>> myNotifications() {
        return ApiResponse.success(notificationService.myNotifications());
    }

    @PostMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Long notificationId) {
        notificationService.markRead(notificationId);
    }
}
