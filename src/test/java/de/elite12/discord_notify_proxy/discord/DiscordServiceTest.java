package de.elite12.discord_notify_proxy.discord;

import de.elite12.discord_notify_proxy.observability.NotificationMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.aot.DisabledInAotMode;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisabledInAotMode("Uses Mockito mocks that are only needed for JVM tests")
class DiscordServiceTest {

    @Test
    void recordsFailureMetricAndTraceAttributesWhenUserResolutionFails() {
        TestTelemetry telemetry = TestTelemetry.create();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        NotificationMetrics notificationMetrics = new NotificationMetrics(meterRegistry);

        JDA jda = mock(JDA.class);
        @SuppressWarnings("unchecked")
        CacheRestAction<User> userLookup = mock(CacheRestAction.class);
        when(jda.retrieveUserById(123L)).thenReturn(userLookup);
        doAnswer(invocation -> {
            Consumer<? super Throwable> failure = invocation.getArgument(1);
            failure.accept(new IllegalStateException("missing user"));
            return null;
        }).when(userLookup).queue(any(), any());

        DiscordService service = new DiscordService(jda, telemetry.openTelemetrySdk(), notificationMetrics);
        service.sendDirectMessage(123L, mock(MessageEmbed.class));

        List<SpanData> spans = telemetry.spanExporter().getFinishedSpanItems();
        assertThat(spans).singleElement().satisfies(span -> {
            assertThat(span.getName()).isEqualTo("discord.dm.deliver");
            assertThat(span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("discord.user_id")))
                    .isEqualTo("123");
            assertThat(span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("discord.failure_stage")))
                    .isEqualTo("resolve_user");
            assertThat(span.getStatus().getStatusCode()).isEqualTo(io.opentelemetry.api.trace.StatusCode.ERROR);
        });
        assertThat(meterRegistry.get("discord.dm.enqueued").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("discord.dm.failed").tag("stage", "resolve_user").counter().count()).isEqualTo(1.0);

        telemetry.close();
    }

    @Test
    void recordsSuccessMetricAndTraceEventsWhenMessageIsSent() {
        TestTelemetry telemetry = TestTelemetry.create();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        NotificationMetrics notificationMetrics = new NotificationMetrics(meterRegistry);

        JDA jda = mock(JDA.class);
        @SuppressWarnings("unchecked")
        CacheRestAction<User> userLookup = mock(CacheRestAction.class);
        User user = mock(User.class);
        @SuppressWarnings("unchecked")
        CacheRestAction<PrivateChannel> privateChannelAction = mock(CacheRestAction.class);
        PrivateChannel privateChannel = mock(PrivateChannel.class);
        MessageCreateAction messageCreateAction = mock(MessageCreateAction.class);

        when(jda.retrieveUserById(123L)).thenReturn(userLookup);
        when(user.openPrivateChannel()).thenReturn(privateChannelAction);
        when(privateChannel.sendMessageEmbeds(any(MessageEmbed.class))).thenReturn(messageCreateAction);

        doAnswer(invocation -> {
            Consumer<? super User> success = invocation.getArgument(0);
            success.accept(user);
            return null;
        }).when(userLookup).queue(any(), any());
        doAnswer(invocation -> {
            Consumer<? super PrivateChannel> success = invocation.getArgument(0);
            success.accept(privateChannel);
            return null;
        }).when(privateChannelAction).queue(any(), any());
        doAnswer(invocation -> {
            Consumer<? super Message> success = invocation.getArgument(0);
            success.accept(mock(Message.class));
            return null;
        }).when(messageCreateAction).queue(any(), any());

        DiscordService service = new DiscordService(jda, telemetry.openTelemetrySdk(), notificationMetrics);
        service.sendDirectMessage(123L, mock(MessageEmbed.class));

        List<SpanData> spans = telemetry.spanExporter().getFinishedSpanItems();
        assertThat(spans).singleElement().satisfies(span -> {
            assertThat(span.getName()).isEqualTo("discord.dm.deliver");
            assertThat(span.getEvents()).extracting(event -> event.getName())
                    .contains("discord.delivery.started", "discord.user.resolved", "discord.dm.channel.opened", "discord.dm.sent");
        });
        assertThat(meterRegistry.get("discord.dm.enqueued").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("discord.dm.sent").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("discord.dm.failed").counter()).isNull();

        telemetry.close();
    }

    private record TestTelemetry(OpenTelemetrySdk openTelemetrySdk, InMemorySpanExporter spanExporter) {

        private static TestTelemetry create() {
            InMemorySpanExporter spanExporter = InMemorySpanExporter.create();
            SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                    .build();
            OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
                    .setTracerProvider(tracerProvider)
                    .build();
            return new TestTelemetry(openTelemetrySdk, spanExporter);
        }

        private void close() {
            openTelemetrySdk.close();
        }
    }
}
