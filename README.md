# FidoCadJ

<div align="center">

![Version](https://img.shields.io/badge/version-0.24.8-blue.svg)
![License](https://img.shields.io/badge/license-GPL%20v3-green.svg)
![Java](https://img.shields.io/badge/java-9%2B-orange.svg)
![Platform](https://img.shields.io/badge/platform-Windows%20%7C%20macOS%20%7C%20Linux%20%7C%20Android-lightgrey.svg)

**An easy-to-use electronic schematic capture and PCB design tool**

[🌐 Website](http://fidocadj.github.io/FidoCadJ/index.html) • [📖 Documentation](https://github.com/FidoCadJ/FidoCadJ/blob/master/manual/manual_en.pdf) • [🐛 Report Bug](https://github.com/FidoCadJ/FidoCadJ/issues) • [💡 Request Feature](https://github.com/FidoCadJ/FidoCadJ/issues)

</div>

---

## 📑 Table of Contents

- [About FidoCadJ](#-about-fidocadj)
  - [What is FidoCadJ?](#what-is-fidocadj)
  - [Key Features](#-key-features)
  - [Community Libraries](#-community-libraries)
  - [Supported Platforms](#-supported-platforms)
- [Getting Started](#-getting-started)
  - [Installation](#installation)
  - [Quick Start](#quick-start)
- [For Developers](#-for-developers)
  - [Repository Structure](#repository-structure)
  - [Building from Source](#building-from-source)
  - [Coding Conventions](#coding-conventions)
  - [Testing](#testing)
- [Contributing](#-contributing)
  - [Translation](#translation)
  - [Development](#development)
  - [Committer Checklist](#committer-checklist)
- [Support](#-support)
- [Acknowledgments](#-acknowledgments)
- [License](#-license)

---

## 🎯 About FidoCadJ

### What is FidoCadJ?

FidoCadJ is a modern, multiplatform electronic design automation (EDA) software with a vast library of electrical symbols and footprints (both through-hole and SMD). It serves as an easy-to-use editor for creating electrical schematics and printed circuit board layouts.

**Origins:** FidoCadJ is the successor to the original FidoCAD, a popular vector graphic editor that gained widespread adoption in the Italian Usenet community during the late 1990s. The original software used a compact text-based file format, perfect for sharing in newsgroups and forums.

**Modern Evolution:** FidoCadJ brings the simplicity of FidoCAD into the modern era with:
- Full UTF-8 text support
- Advanced features and professional output
- Internationalization
- Sleek, anti-aliased user interface
- Multiple export formats (PDF, EPS, PGF for LaTeX, SVG, PNG, JPG)

> **Note:** FidoCadJ is a graphical drawing tool without netlist functionality. This provides maximum flexibility as a vector drawing application but does not include circuit simulation capabilities.

### ✨ Key Features

- 📐 Extensive library of electrical components and footprints
- 🎨 Multiple export formats (PDF, EPS, SVG, PNG, JPG, PGF)
- 🌍 Multilingual interface (English, French, Italian, Spanish, German, Chinese, Dutch, Japanese, Greek, Czech)
- 📱 Cross-platform compatibility
- 🔧 Simple mechanical drawing capabilities
- 🎯 Easy to learn and use

### 📚 Community Libraries

FidoCadJ comes with a built-in library of components, but the community has created many additional libraries to extend its capabilities! You can download specialized libraries for various purposes:

🔗 **[Browse and Download Community Libraries](https://github.com/FidoCadJ/FidoCadJ/tree/gh-pages/libs)**

These community-contributed libraries include:
- Additional electronic components
- Specialized symbols and footprints
- Industry-specific elements
- Custom mechanical parts
- And much more!

Simply download the library files and import them into FidoCadJ to expand your component collection.

### 💻 Supported Platforms

| Platform | Requirements |
|----------|-------------|
| **Windows** | Windows 7, 8, 10, 11 with Java 9+ |
| **Linux** | All major distributions with Java 9+ |
| **macOS** | macOS 10.8+ with Java 9+ |
| **Android** | Android 4.0+ |

---

## 🚀 Getting Started

### Installation

#### Windows

Download `FidoCadJ_Windows.msi` from the [releases page](https://github.com/FidoCadJ/FidoCadJ/releases) and run the installer. Launch FidoCadJ from the Start menu.

#### macOS

1. Download `FidoCadJ_MacOSX.dmg` from the [releases page](https://github.com/FidoCadJ/FidoCadJ/releases)
2. Open the DMG file and drag FidoCadJ to your Applications folder

> 📝 **Note:** The application bundle is self-sufficient and includes everything needed to run. You don't need to install Java separately.

**⚠️ macOS Gatekeeper Issue:**

Recent macOS versions may show a **very misleading error** when trying to run FidoCadJ:

> _"FidoCadJ.app is damaged and can't be opened. You should move it to the Trash."_

**This is NOT true!** The application is not damaged. This is a security feature that prevents running apps from unidentified developers. The error message is misleading and doesn't explain the real issue.

**What's happening:**
- macOS activates the "quarantine" extended attribute on downloaded files
- The system refuses to run software with this attribute
- On some Macs (like M1/M2), you may be asked to install Rosetta first

**Example of the error (macOS Ventura 13.3):**

![macOS Gatekeeper Error](OSes/mac/error_macOS.png)

**Solution - Follow these steps:**

1. **Remove the quarantine attribute:**
   Open Terminal and type:
   ```bash
   xattr -c /Applications/FidoCadJ.app
   ```
   *(If you haven't moved it to Applications yet, use the actual path to the file)*
   
   > ⚠️ You need admin access and may need to authorize Terminal.app to modify files

2. **Right-click on FidoCadJ.app** and select **"Open"** (don't double-click)

3. **Confirm** when the system asks if you really want to run the software downloaded from an untrusted source

**Need help?** See [Issue #198](https://github.com/FidoCadJ/FidoCadJ/issues/198) for discussion and alternative solutions.

> 💡 If you know a way to solve this without using Terminal, please share it in the GitHub issue!

#### Linux

**Method 1: Using the JAR file**
```bash
java -jar fidocadj.jar
```

**Method 2: Build from source**
```bash
make
sudo make install
```

#### Android

Download the APK file from the [official GitHub releases page](https://github.com/FidoCadJ/FidoCadJ/releases) and install it on your device.

> ⚠️ **Security Note:** Only download FidoCadJ from the official GitHub repository to ensure authenticity.

### Quick Start

1. **Launch FidoCadJ** using your preferred method
2. **Create a new drawing** from the File menu
3. **Select components** from the extensive library
4. **Design your schematic** using the intuitive interface
5. **Export** to your desired format (PDF, PNG, SVG, etc.)

💡 **Pro Tip:** Expand your component collection by downloading [community libraries](https://github.com/FidoCadJ/FidoCadJ/tree/gh-pages/libs)!

📖 **Need help?** Check out the [user manual](https://github.com/FidoCadJ/FidoCadJ/releases) (available in multiple languages).

---

## 👨‍💻 For Developers

### Repository Structure

```
FidoCadJ/
├── bin/                    # Compiled classes and resources
├── busy being born/        # Screenshots and promotional materials
├── dev_tools/              # Build scripts and development tools
├── doc/                    # Javadoc documentation
├── icons/                  # Application icons
├── jar/                    # JAR and manifest files
├── manual/                 # LaTeX manual sources
├── OSes/                   # Platform-specific files (including Android)
├── src/                    # Java source files
├── test/                   # Automated test suite
├── gpl-3.0.txt            # License file
├── makefile               # Build automation
└── README.md              # This file
```

#### Important Classes

| Path | Description |
|------|-------------|
| `src/fidocadj/FidoMain.java` | Application entry point |
| `src/fidocadj/FidoFrame.java` | Main editor window |
| `src/fidocadj/primitives/*.java` | Graphic primitives |
| `src/fidocadj/dialogs/*.java` | Swing dialogs |
| `src/fidocadj/circuit/CircuitPanel.java` | Editor panel |
| `src/fidocadj/circuit/*` | Low-level editing (MVC) |

### Building from Source

#### Prerequisites

- Java Development Kit (JDK) 9 or higher
- Make (for Unix-like systems)
- Git

#### Clone Repository

```bash
git clone https://github.com/FidoCadJ/FidoCadJ.git
cd FidoCadJ
```

#### Build Commands

**Using Make (macOS/Linux):**

| Command | Description |
|---------|-------------|
| `make` | Compile FidoCadJ |
| `make run` | Run FidoCadJ |
| `make rebuild` | Clean and rebuild |
| `make createjar` | Create JAR file in `jar/` directory |
| `make createdoc` | Generate Javadoc |
| `make clean` | Remove compiled classes |
| `make cleanall` | Full cleanup |

**Using Scripts (Windows):**

```batch
cd dev_tools
winbuild.bat compile    # Compile sources
winbuild.bat run        # Run application
winbuild.bat rebuild    # Clean and rebuild
```

**Manual Compilation (Windows):**

```batch
javac -g -sourcepath src -classpath bin .\src\fidocadj\FidoMain.java -d bin
java -classpath .\bin;.\jar;.\jar\ FidoMain
```

### Coding Conventions

#### General Rules

- ✅ **Java Version:** Compatible with Java 14+
- ✅ **Indentation:** 4 spaces (no tabs)
- ✅ **Line Length:** Maximum 80 characters
- ✅ **Newlines:** Unix-style (LF) only
- ✅ **Naming Conventions:**
  - Classes: `PascalCase`
  - Methods and variables: `camelCase`
  - No underscores in names (deprecated)
- ✅ **Documentation:** Javadoc required for public classes and methods
- ✅ **Quality:** Commits must not break the build

#### Code Style Examples

**Methods:**
```java
void exampleMethod(int param1, int param2)
{
    // Method body with 4-space indentation
    System.out.println("Example");
}
```

**Control Structures:**
```java
for (int i = 0; i < 10; ++i) {
    // Loop body
    System.out.println("Iteration: " + i);
}

if (condition) {
    // If body
} else {
    // Else body
}
```

**Classes:**
```java
class ExampleClass
{
    // Class members
}
```

#### Code Quality Tools

We maintain high code quality using:

- **Checkstyle:** Enforces coding standards (`dev_tools/rules.xml`)
- **PMD:** Static code analysis (`dev_tools/pmd.sh`)
- **FindBugs:** Bug detection on compiled JAR
- **Copy/Paste Detection:** Identifies code duplication

> 🎯 **Before submitting a pull request**, run Checkstyle with `dev_tools/rules.xml` to ensure compliance!

### Testing

FidoCadJ includes comprehensive automated tests in the `test/` directory.

**Run all tests:**
```bash
./test/all_tests.sh
```

**Individual test suites:**

| Test | Purpose |
|------|---------|
| `test/export/test_export.sh` | Validates export functionality for all formats |
| `test/messages/test_messages.sh` | Checks translation file consistency |
| `test/size/test_size.sh` | Verifies element size calculations |

> 📝 **Note:** Before running tests, ensure `fidocadj.jar` is up-to-date using `make createjar`.

---

## 🤝 Contributing

We welcome contributions from the community! Whether you're fixing bugs, adding features, translating, or improving documentation, your help is appreciated.

### Translation

FidoCadJ makes translation easy—no programming skills required!

**To translate the interface:**

1. Locate language files in `bin/MessagesBundle_xx.properties` (where `xx` is the [ISO 639-1](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes) language code)
2. Copy the English reference file (`MessagesBundle_en.properties`)
3. Translate the values (keep keys unchanged)
4. Save with appropriate language code
5. Submit a pull request

**Example:**
```properties
# English (MessagesBundle_en.properties)
File = File
New = New
Open = Open file

# French (MessagesBundle_fr.properties)
File = Fichier
New = Nouveau dessin
Open = Ouvre un fichier
```

**Current translations available:** English, French, Italian, Spanish, German, Chinese, Dutch, Japanese, Greek, Czech

### Development

**Getting started:**

1. 🍴 Fork the repository
2. 📖 Read the coding conventions (Section above)
3. 💬 Open an [issue](https://github.com/FidoCadJ/FidoCadJ/issues) to discuss your plans
4. 🔨 Make your changes following our style guidelines
5. ✅ Run tests and quality checks
6. 📬 Submit a pull request

**Useful commands for developers:**

```bash
make createdoc          # Generate Javadoc documentation
dev_tools/checkstyle.sh # Run Checkstyle
dev_tools/pmd.sh        # Run PMD analysis
dev_tools/profile       # Run profiler
```

### Committer Checklist

**For all commits:**
- [ ] Code builds successfully (PC and Android)
- [ ] Follows coding style (section 4.1)
- [ ] Comments added/updated where needed
- [ ] Checkstyle passes with `dev_tools/rules.xml`

**For PC application:**
- [ ] JAR generated and automated tests pass
- [ ] PMD analysis completed

### Ways to Contribute

| Area | Description |
|------|-------------|
| 🌍 **Translation** | Translate UI or documentation to your language |
| 📝 **Documentation** | Improve or update manuals and guides |
| 🎥 **Tutorials** | Create video tutorials for YouTube |
| 🐛 **Bug Reports** | Report issues via [GitHub Issues](https://github.com/FidoCadJ/FidoCadJ/issues) |
| 💻 **Code** | Implement new features or fix bugs |
| 📦 **Packaging** | Create distribution packages (deb, RPM, etc.) |
| 🧪 **Testing** | Expand unit test coverage |

**Feature requests:**
- Export to Gerber format
- Enhanced unit testing
- Linux packaging improvements
- And more!

---

## 💬 Support

### Getting Help

- 📖 **Documentation:** [User manuals](https://github.com/FidoCadJ/FidoCadJ/releases) (PDF files)
- 💬 **GitHub Discussions:** Ask questions and share ideas
- 🐛 **Bug Reports:** [Create an issue](https://github.com/FidoCadJ/FidoCadJ/issues)
- 📧 **Email:** davbucci@tiscali.it (no attachments please)

> 💡 **Tip:** GitHub discussions are preferred for better collaboration among developers.

### Reporting Bugs

Found a bug? We appreciate your help in making FidoCadJ better!

1. Check [existing issues](https://github.com/FidoCadJ/FidoCadJ/issues) to avoid duplicates
2. Create a new issue with:
   - Clear description of the problem
   - Steps to reproduce
   - Expected vs. actual behavior
   - System information (OS, Java version)
   - Screenshots if applicable

---

## 🙏 Acknowledgments

### Contributors

**Core Development:**
- Davide Bucci
- Manuel Finessi
- josmil1
- phylum2
- Kohta Ozaki
- Dante Loi
- miklos80

**Beta Testing:**
- Kagliostro, Bruno Valente, simo85, Stefano Martini, F. Bertolazzi, Emanuele Baggetta, Celsius, Andrea D'Amore, Olaf Marzocchi, Werner Randelshofer, Zeno Martini, Electrodomus, IsidoroKZ, Gustavo, and many others!

**Documentation:**
- Carlo Stemberger
- Dante Loi

**Translations:**
- 🇮🇹 Italian: Davide Bucci, Pietro Baima
- 🇬🇧 English: Davide Bucci, Pasu, DirtyDeeds
- 🇫🇷 French: Davide Bucci, Geo Cherchetout
- 🇩🇪 German: Olaf Marzocchi
- 🇪🇸 Spanish: androu1, sbcne, simo85
- 🇨🇳 Chinese: Miles Qin "qhg007"
- 🇳🇱 Dutch: chokewood
- 🇯🇵 Japanese: Kohta Ozaki
- 🇨🇿 Czech: Chemik582

**Libraries:**
- Lorenzo Lutti, Fabrizio Mileto, DirtyDeeds, Electrodomus, IHRaM group, EY group (coordinated by simo85)

**Icons:**
- [Pictogrammers](https://pictogrammers.com/libraries/)

### External Resources

Code snippets and algorithms adapted from:
- [CenterKey Java Browser Launcher](http://www.centerkey.com/java/browser/)
- [UNSW Natural Cubic Splines](http://www.cse.unsw.edu.au/~lambert/splines/natcubic.html)
- Various Stack Overflow contributors and Java community members

> If you hold copyright to any reused code and have concerns, please [open an issue](https://github.com/FidoCadJ/FidoCadJ/issues) and we will address it promptly.

---

## 📄 License

FidoCadJ is free software licensed under **GPL v3**:

```
FidoCadJ is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

FidoCadJ is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.
```

You should have received a copy of the GNU General Public License along with FidoCadJ.  
If not, see <http://www.gnu.org/licenses/>.

**Additional Licenses:**
- `glyphlist.txt` (for PDF export): Apache License 2.0 - [View License](http://www.apache.org/licenses/LICENSE-2.0.html)

---

## ⚠️ Disclaimer

**FidoCadJ is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.**

If a Greek letter appears after the version number (e.g., 0.24.8α), you are using a preliminary/unstable version.

---

<div align="center">

**Copyright © 2007-2025 FidoCadJ Development Team**

⭐ If you find FidoCadJ useful, please consider starring this repository!

[🌐 Website](http://fidocadj.github.io/FidoCadJ/index.html) • [📦 Releases](https://github.com/FidoCadJ/FidoCadJ/releases) • [🐛 Issues](https://github.com/FidoCadJ/FidoCadJ/issues)

</div>
