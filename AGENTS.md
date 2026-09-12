# Repository Instructions

## Mandatory startup

- Before producing any user-facing response or performing any repository action, read `README.md` completely during the current turn.
- If `README.md` cannot be read, stop and report that blocker instead of continuing.
- Never claim that `README.md` was read unless its contents were actually loaded.

## Generated resources

- Never edit any file under `src/generated` directly, including cleanup, formatting, restoration, or one-line corrections.
- Change the responsible data-generator source, then run `runData` to update `src/generated`.
- Treat every change produced by `runData` as generator output. If that output is wrong or unexpectedly broad, fix the generator and rerun it; do not repair the generated files manually.
