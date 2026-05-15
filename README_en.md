# ghost-bits-encoder

<p>
  <img title="artifact" alt="artifact version" src="https://img.shields.io/badge/artifact-1.0--SNAPSHOT-blue.svg" />
  <img title="plugin" alt="plugin version" src="https://img.shields.io/badge/plugin-2.0-yellow.svg" />
  <img title="java" alt="java version" src="https://img.shields.io/badge/java-8+-orange.svg" />
  <img title="license" alt="license" src="https://img.shields.io/badge/license-MIT-red.svg" />
</p>

`ghost-bits-encoder` is a helper plugin for [Woodpecker](https://github.com/woodpecker-framework/woodpecker-framwork-release/releases). It focuses on Ghost Bits / Unicode high-byte wrapping ideas and provides general encode/decode helpers, JSON-oriented encoders, URL-oriented encoders, and several scenario-specific utilities.

English | [简体中文](README.md)

---

## Overview

- **Project**: `ghost-bits-encoder`
- **Maven coordinates**: `me.gv7:ghost-bits-encoder:1.0-SNAPSHOT`
- **Compilation target**: Java 8
- **Plugin entry**: `src/main/java/me/gv7/woodpecker/plugin/WoodpeckerPluginManager.java`
- **Woodpecker UI name**: `GhostBits 编解码器`
- **Woodpecker plugin version**: `2.0`

The core idea of this plugin is to map the low 8 bits of text into visible Unicode characters, or to generate variants based on specific parsing behaviors. It also provides matching decode and verification helpers so the workflow can be completed directly inside Woodpecker.

## Capabilities

The current project contains more than just the basic `encode` / `decode` tabs. It also includes multiple helpers for different input and transformation styles:

| Category | Helper tab | Description |
| --- | --- | --- |
| Basic | `编码` | Takes arbitrary input and generates 5 GhostBits variants using different Unicode blocks |
| Basic | `解码` | Restores the original content by taking the low 8 bits of each character |
| JSON | `FastJSON Ghost` | Applies `\uXXXX`-style multilingual digit obfuscation to plain text or JSON string literals |
| JSON | `Jackson Ghost` | Encodes plain text as Ghost text; for JSON-like input, only string literal content is transformed |
| URL | `Jetty Hex编码` | Produces `%XX`-style output where hex digits may be replaced with Jetty-acceptable non-standard characters |
| URL | `Ghost URL编码` | Hybrid form that mixes Jetty Hex with Ghost high-byte wrapping |
| URL | `全角URL编码` | Maps printable ASCII characters into fullwidth characters and then URL-encodes them |
| Other | `Base64 Ghost` | Base64-encodes the input first, then wraps each resulting character |
| Other | `CRLF注入编码` | Uses `[CRLF]` as a placeholder and outputs Ghost-wrapped CRLF positions |
| Other | `BCEL Ghost` | Supports wrapping the content after the `$$BCEL$$` prefix |
| Docs | `使用帮助` | Displays built-in help text in the plugin UI |

## Implementation Summary

The core logic lives in `src/main/java/util/GhostBits.java` and `src/main/java/util/Json.java`. At the moment it covers the following areas:

1. **Fixed-base encoding and decoding**
   - Uses `U+4E00` (CJK) as the default base.
   - Decoding restores the original data by taking the low 8 bits of each character.

2. **Per-character random high-byte wrapping**
   - Precomputes a safe code point table.
   - Randomly selects visible, safe Unicode characters for each byte value.

3. **Variant generation across multiple Unicode blocks**
   - Includes bases from Latin Extended, Greek, Cyrillic, Hebrew, Arabic, Devanagari, Bengali, Thai, Myanmar, CJK, and several math/geometric blocks.
   - The `编码` tab generates **5** random variants by default.

4. **Scenario-oriented helper encoders**
   - Jetty Hex / Ghost URL / Fullwidth URL
   - FastJSON / Jackson
   - Base64 / CRLF / BCEL

> Note: This README documents the behaviors and helper features currently implemented in this repository. It does not guarantee compatibility or effectiveness in external environments. Real-world results depend on the target processing chain.

## Registered Helper Tabs

According to `WoodpeckerPluginManager`, the plugin registers these 11 helper tabs at startup:

1. `编码`
2. `解码`
3. `FastJSON Ghost`
4. `Jetty Hex编码`
5. `全角URL编码`
6. `Ghost URL编码`
7. `Base64 Ghost`
8. `CRLF注入编码`
9. `BCEL Ghost`
10. `Jackson Ghost`
11. `使用帮助`

## Usage Examples

### 1. Encode

Default input:

```text
../../../
```

The helper prints the original input and 5 randomly generated variants, for example:

```text
原文: ../../../

贮败赵贰贰贲赥
ぐぜちぢぢづど
αεθικικιμ
...
```

### 2. Decode

Default input:

```text
丮丮丯丮丮丯丮丮丯
```

Output:

```text
../../../
```

### 3. FastJSON Ghost

Default input:

```json
{"@type":"java.awt.Rectangle"}
```

If the input looks like JSON, the helper preserves JSON structure characters and only transforms string literal content. For plain text input, it encodes the whole input into `\uXXXX`-style output.

### 4. Jackson Ghost

Default input:

```json
{"@type":"com.sun.rowset.JdbcRowSetImpl"}
```

If the input is recognized as JSON, only string literal content is transformed. Numbers, booleans, and JSON structure characters stay unchanged.

### 5. Jetty Hex / Ghost URL / Fullwidth URL

These three helper tabs all use the same default input:

```text
../
```

- `Jetty Hex编码`: outputs a `%XX`-style variant.
- `Ghost URL编码`: mixes `%XX` fragments with Ghost characters.
- `全角URL编码`: converts to fullwidth characters first, then URL-encodes the result.

### 6. Base64 Ghost

Default input:

```text
1ue
```

Processing flow: standard Base64 first, then Ghost-wrap each character of the Base64 output.

### 7. CRLF Injection Encoding

Default input:

```text
attacker[CRLF]DATA[CRLF]Subject: PWNED
```

`[CRLF]` works as a placeholder. During encoding, those positions are replaced by Ghost-wrapped `\r\n`.

### 8. BCEL Ghost

Default input:

```text
$$BCEL$$$l$8b$I$A$A$A$A$A$A$A$9c$bc$db$d2$ab$ca$96
```

If the input starts with `$$BCEL$$`, the helper keeps that prefix unchanged and only transforms the remaining content.

## Build and Install

### Requirements

- JDK 8 or later
- Maven 3.x
- A Woodpecker environment that can load Helper plugins

### Build locally

```bash
git clone https://github.com/woodpecker-appstore/ghost-bits-encoder.git
cd ghost-bits-encoder
mvn clean package
```

After a successful build, the artifact is generated at:

```text
target/ghost-bits-encoder-1.0-SNAPSHOT.jar
```

### Install into Woodpecker

```bash
cp target/ghost-bits-encoder-1.0-SNAPSHOT.jar <woodpecker-plugins-dir>/
```

## Project Structure

```text
src/main/java/me/gv7/woodpecker/plugin/
├── WoodpeckerPluginManager.java
└── helpers/
    ├── Base64GhostHelper.java
    ├── BcelGhostHelper.java
    ├── CrlfGhostHelper.java
    ├── FastjsonGhostHelper.java
    ├── FullwidthUrlHelper.java
    ├── GhostUrlHelper.java
    ├── HelpHelper.java
    ├── JacksonGhostHelper.java
    └── JettyHexHelper.java

src/main/java/util/
├── GhostBits.java
└── Json.java

src/test/java/
├── BcelTest.java
├── DigitTest.java
├── FastJsonTest.java
├── GhostBitsTest.java
└── JsonTest.java
```

## Test Coverage

The repository already includes basic tests and sample programs covering:

- generic Ghost encode/decode round trips
- Jetty Hex conversion logic
- Base64 Ghost round-trip validation
- CRLF encoding checks
- Fullwidth URL / Ghost URL basic output validation
- JSON structure preservation for Jackson / FastJSON related flows

You can run the current test suite with:

```bash
mvn test
```

## Dependencies

The main dependencies currently listed in `pom.xml` are:

- `me.gv7.woodpecker:woodpecker-bcel:0.1.0`
- `me.gv7.woodpecker:woodpecker-sdk:0.3.0`
- `me.gv7.woodpecker:woodpecker-requests:0.2.0`
- `me.gv7.woodpecker:woodpecker-tools:0.1.0.beta1`
- `com.alibaba:fastjson:1.2.57` (test scope)

## Contributors

- [whwlsfb](https://github.com/whwlsfb/) @ SgLab
- [Xenc](https://github.com/rsxenc-maker/) @ SgLab

<img src="./images/sglab.svg" width="300" alt="SgLab">

## License

MIT

