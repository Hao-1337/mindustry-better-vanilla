# Contributing to mindustry-better-vanilla
Thank you for contributing. Please follow these guidelines to keep the project stable and maintainable.

> [!NOTE]
> **Translation status:** `bundle.properties`, `bundle_es.properties`, and `bundle_vi.properties` are currently treated as verified references. Other bundle files are machine-assisted (Google Gemini Pro & ChatGPT 5.4) translations and still need review by native speakers.

> [!NOTE]
> **Current Javadoc by Github Copilot Pro**

## Before you start
- Fork the repository and create a branch named: `type/short-description` (e.g., `balance/solar-booster`, `feature/new-block`).
- Target pull requests to the default branch `master` unless instructed otherwise.

## General rules
- Follow existing coding style and project conventions.
- Write clear, atomic commits with descriptive messages.
- Run and verify the game/mod locally; ensure there are no runtime errors or obvious regressions.
- Include tests or screenshots when applicable.

## Balance changes
- Keep changes minimal and justified.
- Document reason, expected effect, and any numbers changed in the PR description.
- Include playtesting notes and results.

## Logic changes (Java required)
- Any change to game logic, AI, or core mechanics must be implemented in Java.
- Provide a clear explanation of the change and unit/integration tests when possible.
- Java contributors should follow standard Java conventions and project build settings.

## Language translation
- Translations must be contributed by people who speak that language (a.k.a "native speaker").
- Language bundles may contain machine-assisted translations. Treat them as draft quality until reviewed by a native speaker.
- Keep terminology consistent with existing translations.
- Provide context for ambiguous strings and test in-game for layout/overflow issues.

## Feature implementations
- Propose features via an issue first unless trivial.
- Include design notes, API changes, and compatibility considerations.
- Implement features in a backward-compatible manner when possible.

## Pull request checklist
- I have pulled the latest `master` and rebased my branch.
- Code builds without errors.
- All changed content works correctly in-game.
- PR includes description, motivation, and testing notes.

## Reviews and feedback
- Be responsive to review comments.
- Keep changes focused per PR; use follow-up PRs for additional work.

Thank you for helping improve mindustry-better-vanilla.
