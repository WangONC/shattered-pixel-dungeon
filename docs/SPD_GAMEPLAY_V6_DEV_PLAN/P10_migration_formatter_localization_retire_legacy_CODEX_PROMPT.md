# Codex 执行 Prompt — P10 完整 v5→v6 Migration、Typed Formatter/Localization Sweep 与旧公开路径退役

你现在执行 **Shattered Pixel Dungeon 自塑职业 Gameplay Components v6 — P10**。

## 输入与唯一基线

- 源码输入：`SPD_GC_V6_P09_VOW_BUDGET_CHECKPOINT.zip`
- 冻结审计：`SPD_CLASS_GAMEPLAY_COMPONENTS_CURRENT_IMPLEMENTATION_AUDIT_v0.2.md`
- 冻结合约：`SPD_CLASS_GAMEPLAY_COMPONENTS_IMPLEMENTATION_CONTRACT_v0.2_FINAL.md`
- 开发计划：`SPD_GAMEPLAY_COMPONENTS_V6_DEVELOPMENT_PLAN_FINAL.md`

本阶段目标：**在新增复杂 Primitive 前清除双模型污染：旧数据可迁移，但旧 Schema/Builder/QA 不再驱动新玩家系统或完成结论。**

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

- 从 P09 accepted checkpoint 开始。
- 按 Contract 第 26.4-26.5、28、35、38 与 Appendix F 搜索要求执行。
- 不允许把 legacy class 继续作为 v6 runtime 中间表示；migration 输出必须直接是 v6 typed spec/state。
- 旧 fire payload 按实际旧 Runtime 行为迁移；若实际为 DirectDamage，必须 warning，不能擅自改成 Burning。
- 任何空 legacy ref 迁移为 UNRESOLVED；UI 可给候选，但不得自动确认。
- 生成静态搜索结果文件、migration corpus 结果、全量 localization report，并打包 checkpoint 后复验。

## 本阶段明确范围

- 完成所有已实现 v5→v6 mapping：Resource、Skill/Class Convert、Mark、Mode、Entity payload、AREA、Consumable、旧 Constraint、auto-bound 空引用。
- Migration 必须可重放、幂等、确定性，输出 MigrationReport；行为收紧/不一致明确 warning，不静默美化。
- 完成 typed formatter 和至少 en/zh_CN 的所有 exposed Variant、字段、诊断、Budget、Builder 页面与 Build Sheet。
- 完成 resolved/unresolved/unsupported/min/default/max/中文自定义名的 localization sweep。
- 旧 Builder 退役或转为只读 migration entry；`PlayerBuildAssembler` final-object interface 不再进入玩家/acceptance 包。
- 移除所有非 legacy/migration 的 `resolvePendingBindings`、first-resource/mode、Mark fallback、fixed carrier payload、AREA modifier、Java class-name cost、completion shortcut。
- 完成当前范围的 Full Save/Load roundtrip 与 migration fixtures；新 P11 states 可后续扩展。
- 更新 completion matrix 证据结构，但不得提前宣告 final complete。


## 本阶段验收 Gate

- [ ] Mandatory localization sweep 当前全部 exposed surface 通过：零 `NO TEXT FOUND`、`�`、raw enum、Java class、placeholder。
- [ ] 代表性 v5 saves 构建→迁移→v6 load→canonical→runtime 行为验证通过；unknown 保持 unsupported/unresolved。
- [ ] 静态搜索在 legacy/migration 之外零命中：`resolvePendingBindings`、player-facing `RuleMark.Type`、carrier payload string、AREA modifier、first-item fallback、`implemented() { return true; }` completion shortcut。
- [ ] 旧 UI/QA 不再能产生 v6 `IMPLEMENTED` 行。
- [ ] Checkpoint 解压后 migration、localization、core roundtrip 重跑通过。


## 代码处理原则

LEGACY 旧字段袋/enum/string 仅留 `migration/v5`；DELETE 所有 public/Runtime fallback；REWORK Formatter/Localization；RETIRE v5 Builder。

## 必须交付

1. `SPD_GC_V6_P10_MIGRATION_LOCALIZATION_CHECKPOINT.zip`：包含完整当前工作区源码，不是 patch-only。
2. `V6_CHECKPOINT_MANIFEST.json`：父 checkpoint hash、当前 ZIP hash、Audit/Contract hash、schema/price versions、测试命令与结果。
3. `PHASE_P10_IMPLEMENTATION_REPORT.md`：实际修改文件、行为变化、Gate 逐项证据、未通过项、仍 Deferred/Unsupported 项。
4. BuilderCommand traces、canonical serialization artifacts、Runtime traces 与测试报告。
5. 对 checkpoint 重新解压后的复验结果。

完成后立即停止，不执行下一阶段。
