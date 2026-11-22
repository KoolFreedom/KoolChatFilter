# Contributing to KoolChatFilter

Thanks for your interest in contributing to KoolChatFilter!

This file covers the practical workflow for this repository. Our project-wide policy, including what we accept, what we deny, and how the Contributor role works, lives in the [KoolFreedom Developer Information](https://koolfreedom.eu.org/d/4-koolfreedom-developer-information) forum post. **Please read it before opening a Pull Request.** If anything here conflicts with that post, the forum post wins.

## Before you start

* **Open an issue first** for anything beyond a small fix, so a maintainer can confirm the direction before you spend time on it.
* Check existing issues and PRs to avoid duplicating work.
* Make sure your idea fits the policy in the forum post. Joke features, NSFW content, and anything that grants elevated permissions will not be accepted.
## Setting up

<!-- project-specific -->
1. Fork the repository and clone your fork.
2. Requirements: Gradle + Java 25.
3. Build with: `./gradlew clean build` on Linux | `gradlew.bat clean build` on Windows.
4. Test on: PaperMC server running 26.3 with no other plugins that could conflict with KoolChatFilter.
## Making changes

* Follow the existing code style and project structure.
* Keep changes focused. One PR should do one thing.
* Don't commit secrets, tokens, server IPs, or personal data.
* Test your changes before pushing, and include what you tested in the PR.
* If there should be a new config value directly related to the filter, it should go in the `filter.<x>` config section. Where `<x>` is your new value.
* Permission nodes should always start with `koolchatfilter.<x>`. Where `<x>` is your specific permission.
## Branches and commits

* Name branches `feat/<short-description>` or `fix/<short-description>`.
* Branching from `main` is preferred. Keep your fork in sync with upstream before opening the PR.
* Write clear, specific commit messages that say what changed and why.
## Pull Requests

Your PR description should include:

* **What** you changed and **why**.
* **How you tested it** (server version, steps taken).
* Screenshots or logs where relevant.
* A link to the related issue, if there is one.
  After you submit:

* A repository admin will review your PR. Reviews may take a few days.
* Be ready to respond to feedback and make changes.
* Please stay around after your PR is merged. We expect contributors to help maintain what they add.
## The Contributor role

Having a PR approved earns you the cosmetic Contributor title on our servers, forum, and Discord. It carries no permissions. Full details are in the forum post linked above.

## Reporting security issues

If you find a security vulnerability, **do not open a public issue.** Contact a maintainer privately on Discord instead.

## Contact

* Discord: https://discord.gg/EBPkCjSDMW
* Forums: https://koolfreedom.eu.org