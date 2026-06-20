package de.elite12.discord_notify_proxy.web;

import de.elite12.discord_notify_proxy.seerr.SeerrNotificationService;
import de.elite12.discord_notify_proxy.seerr.model.DeliveryStatus;
import de.elite12.discord_notify_proxy.seerr.model.SeerrNotificationResult;
import de.elite12.discord_notify_proxy.seerr.model.SeerrWebhookPayload;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/seerr")
public class SeerrWebhookController {

    private final SeerrNotificationService seerrNotificationService;

    public SeerrWebhookController(SeerrNotificationService seerrNotificationService) {
        this.seerrNotificationService = seerrNotificationService;
    }

    @PostMapping
    public ResponseEntity<SeerrNotificationResult> receive(@Valid @RequestBody SeerrWebhookPayload payload) {
        SeerrNotificationResult result = seerrNotificationService.process(payload);
        HttpStatus status = result.status() == DeliveryStatus.SENT ? HttpStatus.OK : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(result);
    }
}
