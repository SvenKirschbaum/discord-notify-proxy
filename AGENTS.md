# AGENTS.md

- Stack: Java 25, Spring Boot 4.1, Maven Wrapper, JDA 6, OpenTelemetry, GraalVM native support.
- Use `./mvnw`, not system `mvn`.
- There is no repo `README`, lint config, formatter config, task runner, or repo-local OpenCode config. `pom.xml` is the build source of truth.

## Verify Fast
- Compile only: `./mvnw compile`
- Run all JVM tests: `./mvnw test`
- Run one test class: `./mvnw -Dtest=SeerrNotificationServiceTest test`
- Run one test method: `./mvnw -Dtest=SeerrWebhookControllerTest#acceptsValidWebhookPayload test`
- Native build/test commands exist, but do not use them for routine changes: `./mvnw native:compile -Pnative`, `./mvnw test -PnativeTest`

## CI Workflows
- `.github/workflows/ci.yml` splits by trust level: fork PRs run only `./mvnw -B test`, while non-fork PRs and other refs run `./mvnw -B -PnativeTest test` with `APP_DISCORD_TOKEN` from secrets.
- `.github/workflows/release-native.yml` runs on `main`, version tags, or manual dispatch; it native-tests first, then publishes a buildpack image to `registry.gitlab.com/fallobst22/images/discord-notify-proxy`, and scans `main` images with Trivy.

## Code Map
- Entry point: `de.elite12.discord_notify_proxy.DiscordNotifyProxyApplication`
- HTTP ingress: `web/SeerrWebhookController` exposes `POST /webhooks/seerr`
- Webhook business logic and embed construction live in `seerr/SeerrNotificationService`
- JDA integration and async delivery tracing live in `discord/DiscordService`
- Health wiring is custom: `discord/DiscordHealthIndicator` is included in readiness but excluded from liveness via `src/main/resources/application.yaml`

## Testing Quirks
- Most JVM tests that use Mockito or `@MockitoBean` are annotated `@DisabledInAotMode`; keep routine verification on normal `./mvnw test`.
- `NativeImageSmokeTest` only runs when `APP_DISCORD_TOKEN` is set and waits for a real JDA connection before posting to `/webhooks/seerr`.
- `@WebMvcTest` is used for controller behavior, while full-context tests mock `JDA` to avoid booting the real Discord client.
- If you need to mirror CI locally, choose the lane deliberately: JVM-only `./mvnw test` vs native `./mvnw -PnativeTest test`.

## Repo-Specific Gotchas
- Checked-in runtime config is `src/main/resources/application.yaml`. The repo-root `application.yaml` is gitignored local config and currently holds a real-looking token; do not treat it as a normal source file.
- `webhook.json.tpl` is the concrete Seerr payload template for this app: it sends top-level fields plus nested `media` / `request` / `issue` / `comment` objects whose keys are themselves templated (for example `"{{request}}"`). Keep parser changes compatible with that shape.
- App properties bind from prefix `app`; the Discord token field is `discordToken`, so Spring accepts `app.discord-token` in tests and env-backed config.
- Seerr payload parsing is intentionally tolerant: `SeerrWebhookPayload` reads both top-level fields and nested `media` / `request` / `issue` / `comment` objects, including string-encoded Discord ID lists. Preserve that behavior and test both payload shapes when changing webhook parsing.
- Recipient selection order matters in `SeerrNotificationService`: `notifyUserDiscordIds` first, then request/issue-specific fallbacks.
- Native support depends on `config/MyRuntimeHints.java` registering JDA reflection hints. If you introduce more JDA types that native mode touches reflectively, update that file.

## Change Guidance
- Keep controllers thin; business rules belong in `seerr` or `discord` services.
- Add or update focused tests with behavior changes, especially around fallback payload parsing, recipient resolution, and actuator health behavior.
