# Contributing

## Development Standards

- All changes must go through Pull Requests to `main`.
- Direct pushes to `main` are not allowed.
- Keep PRs focused and reviewable.

## Required Before Requesting Review

- CI checks pass (`lint`, `test`, `sast`, `dependency_scan`, `coverage_gate`).
- Security-impacting changes include risk notes.
- Public APIs include contract/update notes.

## Commit Quality

- Use conventional style where possible (`feat:`, `fix:`, `chore:`).
- Include context in commit body for non-trivial changes.

## Pull Request Checklist

- Problem statement and scope are clear.
- Test coverage for new behavior is included.
- Backward compatibility impact is described.
