# AGENTS.md

Instructions for coding agents working on this project.

## Rules

1. This is a released, production plugin. Preserve documented behaviour and public API unless a breaking change is explicitly approved for a major release. Record breaking changes and migration steps in `CHANGELOG.md` and `README.md`.
2. Before planning or implementing architectural changes, read `docs/decisions.md`.
3. GitHub issues are the source of truth for planned work. Do not create a local task backlog.
4. Use pnpm for dependency installation and package scripts. Commit `pnpm-lock.yaml`; do not add an npm lockfile.
5. Dependencies use exact versions in `package.json`. Only `peerDependencies` may use a compatibility range.
6. TypeScript stays in strict mode. Avoid `any`; use `unknown` at untrusted boundaries.
7. Keep `src/index.ts` and `src/definitions.ts` as the public entry point and API. Internal code should be organised by domain, and `test/` should mirror `src/`.
8. Every user-facing change gets an entry under `CHANGELOG.md`'s Unreleased section.
9. Run the relevant web, package, Android, and iOS checks before proposing a change for review.
10. Packaging changes require `pnpm verify:package`; a successful build alone does not prove that the npm artifact resolves.
11. Ask the user for review and propose a sensible commit before committing.
12. Agentic coding requires meaningful human oversight. Never add AI attribution, generated-by footers, or AI `Co-Authored-By` trailers to the project record.
13. Follow the formatting rules in `.editorconfig` for every edited or created file.

## Stack

- TypeScript in strict mode
- pnpm
- Capacitor 8
- Web, Android (Kotlin), and iOS (Swift)
- iOS packaging through Swift Package Manager only
- Vitest for TypeScript tests and JUnit for Android tests

## Repository layout

| Path | Purpose |
| --- | --- |
| `src/` | TypeScript implementation and public API |
| `test/` | TypeScript tests mirroring `src/` |
| `android/` | Android implementation and tests |
| `ios/Sources/` | Swift Package Manager implementation |
| `ios/Tests/` | Swift tests |
| `docs/decisions.md` | Architectural decisions and rationale |
| `.github/ISSUE_TEMPLATE/` | Work intake; remaining work is tracked in GitHub issues |

## Standard procedure

Before asking for clarification, inspect the repository with the available tools.

- Read `docs/decisions.md` before product or architecture work.
- Use `git diff` and `git status` to preserve in-progress user changes.
- Check dependency versions in `package.json` and `pnpm-lock.yaml` rather than guessing.
- Keep tests outside `src/` and add regression tests for behaviour changes.
- Use concise commit messages: a subject and at most one short paragraph.

## Commands

```bash
pnpm install
pnpm lint
pnpm test
pnpm build
pnpm verify:package
pnpm verify:android
pnpm verify:ios
pnpm verify
```
