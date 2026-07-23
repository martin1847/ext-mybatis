# ext-mybatis — Agent Guide

Quarkus extension that integrates MyBatis with KRPC: auto-scan discovery and injection
of `Mapper`s over a Quarkus/Agroal `QuarkusDataSource`, weak-transaction / throughput-
first data access. Artifact `tech.krpc.ext:ext-mybatis` (version and `rpc-*` pin:
`gradle.properties` is authoritative — do not trust versions quoted in prose).
Capability-cluster ownership lives in the umbrella `docs/modules/extensions.md`.

## Scope

- FOR: Quarkus packaging of MyBatis — CDI auto-injection of Mappers; the
  `QuarkusDataSourceFactory` bridge to Agroal; safe-by-construction connection return
  (non-transactional call = one auto-commit, connection returned immediately);
  `MysqlPagingInterceptor`; native-image support via `native-it`.
- NOT FOR: core runtime behavior (belongs in `krpc`); a standalone ORM/persistence
  product; ambient cross-statement atomicity (must stay explicit opt-in).
- Tier-1: actively maintained/released, but on concrete krpc/consumer need — not
  speculative feature parity.

## Ecosystem rules

- Wire compatibility originates in `krpc`; this repo follows via its `rpc-*` deps, never
  forks the protocol, never leads a wire change. (NS-2.)
- Released on its own cadence, in lockstep with krpc needs — the `rpcVersion` in
  `gradle.properties` is the alignment point.
- **Conforms to umbrella ADR-0002 (weak transactions, throughput first):** the shipped
  minimal config must be safe-by-construction — per-operation auto-commit, no leaked or
  pinned connections, no lifecycle tuning flag (`closeConnection`) required; the framework
  overrides unsafe settings to safe return semantics. Cross-statement atomicity is an
  explicit `@Transactional`/JTA opt-in, never ambient. (NS-5.)
- Native-image is first-class: every feature must work under AOT/native. (NS-7.)
- Long-term direction: umbrella `docs/NORTH_STAR.md` — most relevant here NS-2, NS-5,
  NS-7, NS-8.

## Build & test

Java 21 baseline (build.gradle `sourceCompatibility 21`); Gradle wrapper 9.6.0.

- `./gradlew tasks` — list tasks / verify the wrapper (verified: runs).
- `./gradlew build` — compile + assemble the extension (not verified).
- `./gradlew test` — unit tests, JUnit Platform (not verified).
- `./gradlew publishToMavenLocal` — install to `mavenLocal()` (README's local-test
  recipe) (not verified).
- `./gradlew testNative` (in `native-it`) — native smoke; needs GraalVM (not verified).

Note: README install coordinates may lag releases; `gradle.properties` is
authoritative.

## Discipline

- Behavior changes hide behind a flag, default OFF. No drive-by refactors or format churn.
- Secrets, internal hostnames/IPs, topology never enter the committed tree, logs, or docs.
- Commits stay local until the owner approves a push. No AI signature lines.
