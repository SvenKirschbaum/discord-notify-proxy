package de.elite12.discord_notify_proxy.web;

import de.elite12.discord_notify_proxy.seerr.SeerrNotificationService;
import de.elite12.discord_notify_proxy.seerr.model.DeliveryStatus;
import de.elite12.discord_notify_proxy.seerr.model.SeerrNotificationResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeerrWebhookController.class)
class SeerrWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SeerrNotificationService seerrNotificationService;

    @Test
    void acceptsValidWebhookPayload() throws Exception {
        when(seerrNotificationService.process(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new SeerrNotificationResult(DeliveryStatus.SENT, 1, "Queued Discord direct messages"));

        mockMvc.perform(post("/webhooks/seerr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "notificationType": "MEDIA_AVAILABLE",
                                  "event": "Your request is now available",
                                  "subject": "Dune: Part Two",
                                  "message": "Now streaming.",
                                  "requestId": 42,
                                  "requestUrl": "https://seerr.example/request/42",
                                  "notifyUserDiscordIds": ["108253150351200256"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.recipientCount").value(1));

        verify(seerrNotificationService).process(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsInvalidWebhookPayload() throws Exception {
        mockMvc.perform(post("/webhooks/seerr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
