# Contributing

Feedback, bug reports, feature requests, and pull requests are welcome.

## Issues

GitHub issues are the source of truth for planned work. Search open and closed issues before creating a new report, then use the relevant issue form and provide a minimal reproduction where possible.

## Development

This project uses pnpm and requires the Node and native toolchains supported by Capacitor 8.

```bash
pnpm install
pnpm lint
pnpm test
pnpm verify:package
pnpm verify:android
pnpm verify:ios
```

## Pull requests

- Open one focused pull request per subject and link its GitHub issue.
- Develop on a topic branch and target `develop`. Releases reach `main` through the maintainers' Git Flow process, not pull requests.
- Explain the problem, the chosen solution, and how it was verified.
- Preserve public API and documented behaviour unless the issue explicitly approves a breaking change for a major release.
- Add regression tests for bug fixes and tests for new behaviour.
- Update `README.md` where consumer guidance changes.
- Add every user-facing change to the Unreleased section of `CHANGELOG.md`.
- Avoid unrelated formatting or configuration changes.
- Keep commit history concise; pull requests are squashed when merged.
