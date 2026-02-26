# sbt-lift-build-plugin

An sbt `AutoPlugin` providing common build settings for [Lift](https://liftweb.net/) framework projects.

## What It Provides

- **`liftDefaultSettings`** — standard `javacOptions`/`scalacOptions`, javadoc/scaladoc configuration, manifest attributes (`Built-By`, `Built-Time`), default resolvers, and a custom shell prompt
- **`printLogo`** — prints a colorized Lift logo in the sbt console on startup; suppress with `-Dsbt.lift.nologo=true`
- **`formalize`** — utility to capitalize hyphenated project names (e.g. `my-project` → `My Project`)

## Requirements

- sbt 1.11+

## License

Apache License, Version 2.0. See [LICENSE](https://www.apache.org/licenses/LICENSE-2.0.txt).
