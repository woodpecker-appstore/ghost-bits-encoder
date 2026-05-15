# ghost-bits-encoder

<p>
  <img title="artifact" alt="artifact version" src="https://img.shields.io/badge/artifact-1.0--SNAPSHOT-blue.svg" />
  <img title="plugin" alt="plugin version" src="https://img.shields.io/badge/plugin-2.0-yellow.svg" />
  <img title="java" alt="java version" src="https://img.shields.io/badge/java-8+-orange.svg" />
  <img title="license" alt="license" src="https://img.shields.io/badge/license-MIT-red.svg" />
</p>

`ghost-bits-encoder` 是一个面向 [Woodpecker](https://github.com/woodpecker-framework/woodpecker-framwork-release/releases) 的辅助插件，围绕 Ghost Bits / Unicode 高位包装思路，提供通用编解码、JSON 相关编码、URL 相关编码以及若干专项辅助能力。

简体中文 | [English](README_en.md)

---

## 项目概览

- **项目名**：`ghost-bits-encoder`
- **Maven 坐标**：`me.gv7:ghost-bits-encoder:1.0-SNAPSHOT`
- **编译目标**：Java 8
- **插件入口**：`src/main/java/me/gv7/woodpecker/plugin/WoodpeckerPluginManager.java`
- **Woodpecker 界面名称**：`GhostBits 编解码器`
- **Woodpecker 插件版本**：`2.0`

插件的核心做法是：将文本的低 8 位信息映射到不同的 Unicode 字符中，或基于特定解析逻辑生成可用于测试的变体；同时提供对应的解码与验证能力，方便在 Woodpecker 中直接操作和比对结果。

## 核心能力

项目当前实现的能力不止“编码/解码”两个标签页，还包含多种针对不同输入形式的辅助编码方式：

| 分类 | 标签页 | 说明 |
| --- | --- | --- |
| 基础 | `编码` | 输入任意文本，随机生成 5 个不同 Unicode 区块的 GhostBits 变体 |
| 基础 | `解码` | 对 GhostBits 文本逐字符取低 8 位，还原原始内容 |
| JSON | `FastJSON Ghost` | 对普通文本或 JSON 字符串字面量做 `\\uXXXX` 风格的多文种数字混淆 |
| JSON | `Jackson Ghost` | 对普通文本全文 Ghost 编码；若输入像 JSON，则仅编码字符串字面量内容 |
| URL | `Jetty Hex编码` | 生成 `%XX` 风格的变体，十六进制位可替换为 Jetty 可接受的非标准字符 |
| URL | `Ghost URL编码` | Jetty Hex 与高位包装的混合变体 |
| URL | `全角URL编码` | 将 ASCII 可打印字符映射为全角字符后再做 URL 编码 |
| 其他 | `Base64 Ghost` | 先做 Base64，再对结果逐字符进行高位包装 |
| 其他 | `CRLF注入编码` | 使用 `[CRLF]` 作为占位符，输出包含 Ghost 化 `CRLF` 的结果 |
| 其他 | `BCEL Ghost` | 支持对 `$$BCEL$$` 前缀之后的内容做 Ghost 包装 |
| 文档 | `使用帮助` | 在插件界面内显示项目内置帮助说明 |

## 编码实现摘要

核心实现位于 `src/main/java/util/GhostBits.java` 与 `src/main/java/util/Json.java`，目前包含以下几类逻辑：

1. **固定基址编码与解码**
   - 默认使用 `U+4E00`（CJK）作为基址。
   - 解码时逐字符取低 8 位恢复原文。

2. **逐字符随机高位包装**
   - 预计算安全码点表，按字节值随机选择可见、安全的 Unicode 字符。

3. **多语言区块变体生成**
   - 内置多组可用基底，覆盖拉丁扩展、希腊、西里尔、希伯来、阿拉伯、天城文、孟加拉、泰文、缅甸、CJK、数学/几何符号等区块。
   - `编码` 标签页默认生成 **5 个**随机变体。

4. **面向特定场景的辅助编码**
   - Jetty Hex / Ghost URL / 全角 URL
   - FastJSON / Jackson
   - Base64 / CRLF / BCEL

> 说明：README 只描述当前仓库中已经实现的编码行为与插件功能，不对外部环境的兼容性或实际效果做额外保证。实际测试效果请以目标应用链路为准。

## 已注册的 Helper 标签页

根据 `WoodpeckerPluginManager`，插件启动时会注册以下 11 个标签页：

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

## 使用示例

### 1. 编码

默认输入示例：

```text
../../../
```

执行后会输出原文和 5 个随机变体，格式类似：

```text
原文: ../../../

贮败赵贰贰贲赥
ぐぜちぢぢづど
αεθικικιμ
...
```

### 2. 解码

默认输入示例：

```text
丮丮丯丮丮丯丮丮丯
```

输出：

```text
../../../
```

### 3. FastJSON Ghost

默认输入：

```json
{"@type":"java.awt.Rectangle"}
```

当输入看起来像 JSON 时，插件会保留 JSON 结构字符，只对字符串字面量内容进行编码；若输入为普通文本，则会整体编码为 `\\uXXXX` 风格结果。

### 4. Jackson Ghost

默认输入：

```json
{"@type":"com.sun.rowset.JdbcRowSetImpl"}
```

若识别为 JSON，则仅对字符串字面量内容做 Ghost 包装；数字、布尔值和 JSON 结构字符保持不变。

### 5. Jetty Hex编码 / Ghost URL编码 / 全角URL编码

这三个标签页的默认输入都是：

```text
../
```

- `Jetty Hex编码`：输出 `%XX` 风格变体。
- `Ghost URL编码`：混合 `%XX` 片段与 Ghost 字符。
- `全角URL编码`：先转全角，再输出 URL 编码后的结果。

### 6. Base64 Ghost

默认输入：

```text
1ue
```

处理流程为：先对原文做标准 Base64，再对 Base64 结果逐字符做 Ghost 化。

### 7. CRLF注入编码

默认输入：

```text
attacker[CRLF]DATA[CRLF]Subject: PWNED
```

其中 `[CRLF]` 是占位标记，编码后对应位置会被替换为 Ghost 化的 `\r\n`。

### 8. BCEL Ghost

默认输入：

```text
$$BCEL$$$l$8b$I$A$A$A$A$A$A$A$9c$bc$db$d2$ab$ca$96
```

如果输入以 `$$BCEL$$` 开头，插件会保留此前缀，只对后续内容做编码。

## 构建与安装

### 环境要求

- JDK 8 或更高版本
- Maven 3.x
- 可加载 Woodpecker Helper 插件的运行环境

### 本地构建

```bash
git clone https://github.com/woodpecker-appstore/ghost-bits-encoder.git
cd ghost-bits-encoder
mvn clean package
```

构建完成后会在 `target/` 下生成：

```text
target/ghost-bits-encoder-1.0-SNAPSHOT.jar
```

### 安装到 Woodpecker

```bash
cp target/ghost-bits-encoder-1.0-SNAPSHOT.jar <woodpecker-plugins-dir>/
```

## 项目结构

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

## 测试情况

仓库中已经包含基础测试与示例程序，覆盖了以下内容：

- 通用 Ghost 编码/解码往返
- Jetty Hex 编码逻辑
- Base64 Ghost 往返验证
- CRLF 编码结果校验
- 全角 URL / Ghost URL 基本输出验证
- Jackson / FastJSON 场景下的 JSON 结构保留

当前仓库状态下可直接执行：

```bash
mvn test
```

## 依赖说明

`pom.xml` 当前主要依赖包括：

- `me.gv7.woodpecker:woodpecker-bcel:0.1.0`
- `me.gv7.woodpecker:woodpecker-sdk:0.3.0`
- `me.gv7.woodpecker:woodpecker-requests:0.2.0`
- `me.gv7.woodpecker:woodpecker-tools:0.1.0.beta1`
- `com.alibaba:fastjson:1.2.57`（测试作用域）

## 贡献者（排名不分先后）

- [whwlsfb](https://github.com/whwlsfb/)@SgLab
- [Xenc](https://github.com/rsxenc-maker/)@SgLab

<img src="./images/sglab.svg" width="300" alt="SgLab">

## License

MIT
