# Codex 执行 Prompt — P00 基线确定、Legacy 冻结与防扩债

你现在执行 **Shattered Pixel Dungeon 自塑职业 Gameplay Components v6 — P00**。

## 输入与唯一基线

- 源码输入：`/mnt/data/shattered-pixel-dungeon.7z（原始审计源包）`
- 冻结审计：`SPD_CLASS_GAMEPLAY_COMPONENTS_CURRENT_IMPLEMENTATION_AUDIT_v0.2.md`
- 冻结合约：`SPD_CLASS_GAMEPLAY_COMPONENTS_IMPLEMENTATION_CONTRACT_v0.2_FINAL.md`
- 开发计划：`SPD_GAMEPLAY_COMPONENTS_V6_DEVELOPMENT_PLAN_FINAL.md`

本阶段目标：**建立唯一、可复现的 v6 施工基线；冻结旧 Gameplay Language 作为证据和迁移输入，防止继续向错误抽象加字段。**

## 通用执行纪律（本阶段全部适用）

1. 先完整展开输入 checkpoint；以该 checkpoint 为唯一源码基线。不得混入更早 ZIP、其它会话目录或未说明的本地副本。
2. 完整阅读：
   - `SPD_CLASS_GAMEPLAY_COMPONENTS_CURRENT_IMPLEMENTATION_AUDIT_v0.2.md`
   - `SPD_CLASS_GAMEPLAY_COMPONENTS_IMPLEMENTATION_CONTRACT_v0.2_FINAL.md`
   - 项目内 `docs/SPD_CLASS_GAMEPLAY_COMPONENTS_SPEC_v0.1.md`
   - 本阶段计划与上一阶段验收清单。
3. 这是源码实施任务。必须实际修改源码并运行测试；不得只写设计、报告或 TODO。
4. 只做本阶段。不得提前实现后续阶段，也不得自行扩展 Talent、Subclass、Specialization、Armor Ability、完整内容库或其它未冻结系统。
5. 禁止职业 Domain/标签驱动行为。不得加入 `GUNNER_CORE`、`SUMMONER_DOMAIN`、`ENGINEERING_DOMAIN`、`BLUE_MAGE_DOMAIN` 等隐藏分支。
6. v6 Player Builder 与 Player-path QA 必须走同一 `BuilderCommand`/Reducer。直接构造最终 Spec 只允许放在 `qa.runtime` 作为内部 smoke，不能充当完成证据。
7. 禁止自动补绑、首项回退、默认 Mark、默认 Mode、unknown→Standard Damage、删除后静默改绑。
8. 保留审计判定为 KEEP 的 SPD Hook、RuleRuntime 调度/因果/递归保护、真实效果 helper、HUD、Headless 与 Fuzz 基础；除非本阶段明确要求适配，不得重写这些基础。
9. 对 Contract 中未冻结的玩家可见选项，不得自行设计。应保持 `UNSUPPORTED`/`DEFERRED`、不在 Builder 暴露，并在报告中列明。
10. 每个新增或本阶段标记为 IMPLEMENTED 的 Variant，必须同步提交 Schema、Builder path、Dependency、Formatter、Budget、Save/Load、Runtime 与测试证据；缺一项不得标记 IMPLEMENTED。
11. 所有测试结果必须来自实际命令与行为断言。不得以类存在、Registry count、`implemented()==true`、关键词、无 crash 或报告文字判定 PASS。
12. 完成后生成完整当前工作区 checkpoint ZIP；重新解压该 ZIP，并在解压副本上复跑本阶段 Gate。返回 ZIP SHA-256、父 checkpoint SHA-256、实际修改文件列表、测试命令/退出码、失败项与证据路径。
13. 若无法通过全部 Gate，不得伪造 PASS。返回最大限度可复现的同阶段 checkpoint，明确标记 `PHASE_INCOMPLETE`，且不得进入下一阶段。
14. 完成当前阶段后立即停止，不继续下一阶段。

### 本阶段必须完成

- 生成 `V6_BASELINE_DECISION.md`，说明最终选择的源码基线、理由、KEEP 资产清单、旧公开层清单、验证命令和 hash。
- 生成机器可读 `V6_CHECKPOINT_MANIFEST.json`，至少含：phase、parent/source hash、contract hash、audit hash、schema target=6、test commands/results、build environment、known blockers。
- 建立 legacy/v6 边界及相应 architecture tests；本阶段不得开始重写 Effect、Builder 或 Runtime。
- 为当前关键旧行为建立 `LEGACY_SMOKE` 测试分类，至少覆盖 Resource、Mark、Mode、Entity、Delay、Save/Load、Headless 启动；这些测试只能证明旧行为未被意外破坏。
- 生成完整 checkpoint，并从解压副本复验。

## 本阶段明确范围

- 验证源包/仓库身份，记录源码 SHA、Gradle/SDK 环境与可运行任务。若存在 Git 历史，可按 Audit 的代码地标寻找更干净基线；只有在 KEEP 资产齐全、回归测试通过且有独立复审时才允许切换。否则使用当前审计快照。不得编造 commit。
- 建立单一长期分支（建议 `feature/gameplay-components-v6`）和阶段 tag/checkpoint 规则。
- 将现有“210/210”“coverage complete”等报告明确标为 `LEGACY_EVIDENCE_NOT_PLAYER_PATH`；保留原文件，不篡改历史结果。
- 建立 `legacy.v5` 与 `contract.v6` 的依赖边界/包骨架；旧 Builder 和旧 Runtime 行为本阶段不改语义。
- 加入 architecture guard：禁止再向旧 `EffectSpec` 字段袋、固定 `RuleMark.Type`、carrier payload string、mega ClassGameplayComponent、旧 completion shortcut 增加新的 public gameplay 条目。
- 创建 `V6_CONTRACT_SYMBOL_IMPLEMENTATION_MAP`：逐项列出 Contract 中已完全冻结、可映射到 SPD 原生语义、仅允许最窄内部默认、必须保持 UNSUPPORTED/DEFERRED 的 support type。不得借此新增玩法。
- 冻结当前可运行的 Runtime smoke/headless/fuzz/存档样本，标注只用于 Legacy 回归。
- 引入 v6 开发 feature flag 或等价隔离入口，但不实现新 Gameplay 行为。


## 本阶段验收 Gate

- [ ] 源基线、父来源和所有文档 hash 可复现；若采用历史 commit，有完整选择证据和独立批准。
- [ ] 现有可运行测试/构建与输入基线等价；环境无法下载依赖时，必须记录准确失败点，不能记为代码 PASS。
- [ ] 旧报告不会被新 completion matrix 读取为玩家完成证据。
- [ ] v6 包/模块不依赖职业标签；legacy 与 v6 import 边界有自动检查。
- [ ] 没有 Gameplay 语义变化；checkpoint 解压复验通过。


## 代码处理原则

KEEP 全部 Runtime/HUD/Headless 基础；LEGACY 标记当前公开 Schema/Builder/报告；本阶段不 DELETE 行为代码，仅加入防扩债边界。

## 必须交付

1. `SPD_GC_V6_P00_BASELINE_CHECKPOINT.zip`：包含完整当前工作区源码，不是 patch-only。
2. `V6_CHECKPOINT_MANIFEST.json`：父 checkpoint hash、当前 ZIP hash、Audit/Contract hash、schema/price versions、测试命令与结果。
3. `PHASE_P00_IMPLEMENTATION_REPORT.md`：实际修改文件、行为变化、Gate 逐项证据、未通过项、仍 Deferred/Unsupported 项。
4. BuilderCommand traces、canonical serialization artifacts、Runtime traces 与测试报告。
5. 对 checkpoint 重新解压后的复验结果。

完成后立即停止，不执行下一阶段。
