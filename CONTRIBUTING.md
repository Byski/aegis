# Contributing

## Code standards

This repository follows a strict professional-presentation standard. An automated
gate (`scripts/check-hygiene.sh`) runs on every commit and every pull request and
will reject changes that violate the rules below.

1. **No third-party tooling references.** Committed artifacts (code, comments,
   tests, docs, configs, commit messages, pull request text) must not mention
   external assistants, model vendors, or model product names. Authorship stays
   with the repository owner; do not add attribution trailers.
2. **No em-dash characters.** Use commas, parentheses, colons, or separate
   sentences. Hyphens are allowed only where syntax requires them: command flags,
   file names, identifiers, URLs, and kebab-case strings.
3. **No emoji** in source, documentation, or configuration.
4. **No secrets.** Never commit credentials, private keys, access keys, or
   dotenv files. Runtime secrets are supplied through environment variables; see
   `.env.example` files for the expected shape.

## Workflow

- Branch per unit of work off `main` (for example `feat/...`, `chore/...`,
  `fix/...`). Use conventional commit messages.
- Open a pull request; the CI hygiene job and the service test jobs must pass
  before merge.
- The pre-commit hook is wired by `scripts/bootstrap.sh` via
  `git config core.hooksPath .githooks`. Run bootstrap once after cloning.

## Running the gate locally

```bash
scripts/check-hygiene.sh --staged   # what the hook runs
scripts/check-hygiene.sh --all      # what CI runs
```
