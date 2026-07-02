# Release notes

One Markdown file per release tag: `release-notes/vX.Y.Z.md`.

The `tagged-release.yml` workflow uses the matching file verbatim as the GitHub
Release body and fails if it is missing, so write it before pushing the tag.

Keep the house voice: sentence case for body and bullets, Title Case for section
headers, uppercase acronyms (FPS, GPU, SD, UI, APK), proper nouns capitalized
(Steam, Vulkan, Android), identifiers left as-is (`app.gamenative.seilent`), and
no em dashes.
