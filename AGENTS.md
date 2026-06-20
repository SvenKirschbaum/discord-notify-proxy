# AGENTS.md

This repository is a small Spring Boot 4 application that receives Seerr webhooks and forwards notifications to Discord DMs through JDA.

## Project Snapshot
- Language: Java 25
- Build tool: Maven Wrapper (`./mvnw`)
- Frameworks: Spring Boot 4, Spring MVC, Jakarta Validation, JDA, OpenTelemetry
- Packaging: GraalVM native image
- Main code: `src/main/java`
- Tests: `src/test/java`
- Runtime config: `src/main/resources/application.yaml`

## Agent Rules Present In Repo
At the time this file was generated, the repository does not contain any of the following:
- `.cursorrules`
- files under `.cursor/rules/`
- `.github/copilot-instructions.md`

If any of those files are added later, treat them as higher-priority repository instructions and merge them with this document.

## Recommended Workflow
1. Read `pom.xml` first to confirm plugins, Java version, and test capabilities.
2. Prefer `./mvnw` over a system `mvn` binary so the wrapper controls Maven behavior.
3. Keep changes focused; this codebase is small and uses straightforward layering.
4. Run the smallest relevant test command before broadening to the full suite.
5. Do not edit anything in `target/`; it is build output.

## Build Commands
- Run the app locally: `./mvnw spring-boot:run`
- Compile main code only: `./mvnw compile`
- Compile tests too: `./mvnw test-compile`
- Build the jar: `./mvnw package`
- Clean and rebuild: `./mvnw clean package`
- Run all verification normally used in this repo: `./mvnw test`
- Run a fuller lifecycle build: `./mvnw clean verify`

## Test Commands
- Run all tests: `./mvnw test`
- Run one test class: `./mvnw -Dtest=SeerrNotificationServiceTest test`
- Run one test method: `./mvnw -Dtest=SeerrWebhookControllerTest#acceptsValidWebhookPayload test`
- Run multiple selected tests: `./mvnw -Dtest=SeerrNotificationServiceTest,SeerrWebhookControllerTest test`

Notes: Surefire runs in the default Maven test lifecycle, JUnit 5 discovery works out of the box, and `-Dtest=ClassName#methodName` was verified in this repository.

## Lint / Formatting Commands
There is currently no dedicated lint, Checkstyle, Spotless, PMD, or formatter plugin configured in `pom.xml`.
Use these as the practical quality gates instead:
- Fast compile sanity check: `./mvnw compile`
- Full test gate: `./mvnw test`
- Clean verification pass: `./mvnw clean verify`

If you introduce a formatter or lint tool later, update this file with exact commands.

## Native Image Commands
- Build container image with buildpacks: `./mvnw spring-boot:build-image -Pnative`
- Compile a native executable: `./mvnw native:compile -Pnative`
- Run tests in native mode: `./mvnw test -PnativeTest`

Only use the native commands when the task explicitly touches native support or startup/runtime compatibility.

## Architecture Expectations
- `web` package contains HTTP controllers.
- `seerr` package contains webhook processing logic.
- `seerr.model` contains DTOs and enums for inbound payloads and outbound results.
- `discord` package wraps JDA behavior.
- `config` contains Spring configuration, properties, and runtime hints.

Keep controllers thin and move business decisions into services or model helpers.

## Code Style
- Follow the existing Java style already present in `src/main/java` and `src/test/java`.
- Use 4-space indentation in Java files.
- Put opening braces on the same line as class, method, `if`, `for`, and `switch` declarations.
- Prefer small methods with a single clear responsibility.
- Use blank lines to separate logical sections, not after every statement.
- Avoid wildcard imports.
- Keep normal imports together and static imports in a separate trailing block.
- Prefer standard library and framework imports over fully qualified names inside method bodies, unless used once in tests for clarity.

## Types And Data Modeling
- Prefer `record` for immutable request/response DTOs, as seen in `SeerrWebhookPayload` and `SeerrNotificationResult`.
- Prefer `enum` for closed sets of states and wire-level values.
- Add an `UNKNOWN` enum member when parsing upstream data that may drift.
- Use `@JsonEnumDefaultValue` for tolerant deserialization when appropriate.
- Use `@JsonCreator` / `@JsonValue` when an enum has custom wire values.
- Normalize null inputs in record compact constructors instead of scattering null checks elsewhere.
- Prefer `List.of()`, `Map.of()`, and `List.copyOf()` for immutable defaults and defensive copies.
- Return empty collections instead of `null`.

## Spring Conventions
- Prefer constructor injection; do not add field injection.
- Keep bean configuration in `config` classes with explicit `@Bean` methods when needed.
- Keep request validation close to the DTO using Jakarta annotations like `@NotNull` and `@NotBlank`.
- Use slice tests such as `@WebMvcTest` for controller behavior.
- Use `@SpringBootTest` only when a full application context is actually needed.
- Mock external integrations like `JDA` in tests with `@MockitoBean`.

## Naming Conventions
- Classes, records, and enums: PascalCase.
- Methods and fields: camelCase.
- Constants: UPPER_SNAKE_CASE.
- Packages: lowercase; this repo uses underscores in the base package (`de.elite12.discord_notify_proxy`) because the original hyphenated name was invalid in Java.
- Test method names should be descriptive behavior phrases like `acceptsValidWebhookPayload`.
- Prefer names that describe domain intent over framework mechanics.

## Error Handling And Resilience
- Treat webhook payloads as partially trusted upstream input.
- Be tolerant of missing or malformed optional data when a safe fallback exists.
- Validate required request fields at the boundary using annotations.
- For non-fatal business outcomes, prefer returning a structured result like `SeerrNotificationResult` instead of throwing.
- Only swallow exceptions when the failure is expected and intentionally ignored; document that choice with a brief comment.
- In asynchronous integration code, log failures with context instead of letting them disappear silently.
- Include useful identifiers in logs, such as Discord user IDs or request IDs.

## Logging
- Use SLF4J with `LoggerFactory`.
- Name the logger field `log`.
- Use parameterized logging (`{}`), not string concatenation.
- Log external delivery failures at `warn` unless they are truly expected noise.
- Log successful one-off delivery events sparingly and only when useful for operations.

## Testing Conventions
- Use JUnit 5 (`@Test`).
- Use AssertJ for assertions.
- Use Mockito for mocks and interaction verification.
- Keep unit tests focused on observable behavior, not implementation details.
- Use Spring MVC test support for controller JSON assertions.
- Use text blocks for multi-line JSON payloads in tests.
- When testing webhook parsing, cover both top-level fields and nested fallback objects.
- When testing recipient logic, include malformed IDs, duplicates, and missing values.

## Configuration And Secrets
- Never commit a real Discord bot token.
- Application property binding currently expects `app.discord-token`.
- Tests should provide fake tokens inline, as `DiscordNotifyProxyApplicationTests` already does.
- Keep environment-specific values out of source files whenever possible.

## File-Specific Notes
- `pom.xml` is the source of truth for build behavior.
- `HELP.md` includes native-image commands generated by Spring Initializr; keep it aligned with `pom.xml` if native support changes.
- `webhook.json.tpl` is the template which seer will use to generate the webhooks content. Documentation on how to use it can be found here: https://docs.seerr.dev/using-seerr/notifications/webhook/#template-variables
- `application.yaml` at the repository root is not the main runtime resource; prefer `src/main/resources/application.yaml` for application config changes unless you confirm a separate use case.

## When Making Changes
- Preserve the current package structure unless there is a strong reason to refactor it.
- Add tests with new behavior changes.
- Prefer the narrowest test command that proves your change first, then run `./mvnw test` before finishing substantial work.
- If you add new build plugins, code generators, lint rules, or agent instruction files, update `AGENTS.md` in the same change.
