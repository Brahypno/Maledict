# Maledict

Maledict is a Minecraft Forge 1.20.1 add-on for Malum, centered on the Incursus Blade and its combat, spirit infusion, and repair mechanics.

## Requirements

- Java 17
- Minecraft 1.20.1
- Minecraft Forge 47.4.23 or later
- Malum 1.20.1-1.6.7 or later
- Lodestone 1.20.1-1.6.4.1 or later
- Curios API

## Building

Clone the repository and run the Gradle wrapper:

```shell
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

Build artifacts are written to `build/libs`.

## Development Notes

- Chinese text for Malum codex body pages must contain a literal space after every 13 visible characters. Punctuation counts toward the 13-character limit; the inserted layout spaces do not.
- Write the Chinese codex copy first, then derive the English localization from its meaning.

## License

Maledict is licensed under the [GNU Lesser General Public License v3.0 only](LICENSE) (`LGPL-3.0-only`).
