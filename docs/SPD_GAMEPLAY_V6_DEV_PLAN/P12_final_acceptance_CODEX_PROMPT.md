# Codex 执行 Prompt — P12 全量对抗性验收、15 Recipes、Fuzz/Build/实机 UX 与 FINAL Checkpoint

你现在执行 **Shattered Pixel Dungeon 自塑职业 Gameplay Components v6 — P12**。

## 输入与唯一基线

- 源码输入：`SPD_GC_V6_P11_GENERIC_PRIMITIVES_CHECKPOINT.zip`
- 冻结审计：`SPD_CLASS_GAMEPLAY_COMPONENTS_CURRENT_IMPLEMENTATION_AUDIT_v0.2.md`
- 冻结合约：`SPD_CLASS_GAMEPLAY_COMPONENTS_IMPLEMENTATION_CONTRACT_v0.2_FINAL.md`
- 开发计划：`SPD_GAMEPLAY_COMPONENTS_V6_DEVELOPMENT_PLAN_FINAL.md`

本阶段目标：**不再新增架构和玩法，只收敛缺陷；用真实玩家路径和真实 SPD Runtime 证明 Contract 达成。**

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

- 从 P11 accepted checkpoint 开始。
- 本阶段禁止新增新的 Player-exposed Variant、职业机制或内容系统；只做 Contract 收敛。
- 完整执行 Contract 第 33-36、Appendix F；每个测试保存 command trace、canonical artifacts、Runtime trace 与 test ID。
- 15 Recipe 不能使用预制 final object、preset copy 或职业标签。每个 recipe 必须能从空 Builder 重放。
- Completion matrix 每行九类证据不得为空；未达到 DoD 的节点必须明确 UNSUPPORTED/DEFERRED，不能用百分比掩盖。
- 生成完整源码最终 ZIP，重新解压后复跑所有自动 Gate 和可自动化 build；附 SHA-256、parent chain、实际修改文件列表、仍 Deferred/Unsupported 清单。
- 完成后停止，不进入 Talent/Subclass 等后续阶段。

## 本阶段明确范围

- 冻结 feature surface 与 priceVersion；只修测试、行为、文本、迁移、兼容、性能和 UI 缺陷。
- 运行 12 项 Mandatory Adversarial Player-path Suite、Localization Sweep、15 Archetype Recipe Suite。
- 运行 QA Layers A-G：Schema、Builder、Dependency/Migration/Formatter/Budget、Headless Runtime、Save/Load、Fuzz/Integrity、Manual UX。
- Fuzzer 以 BuilderCommand 序列为主，单独报告 player_command_valid_density、finalized density、runtime failures、unresolved drafts、budget exploits、causal loops、roundtrip failures。
- 运行 core tests、desktop build/run、Android debug build/真机或 AVD smoke；记录实际环境和退出码。
- 完成全量 canonical Build/Runtime roundtrip，与未保存控制组比较后续行为。
- 生成 evidence-backed completion matrix；只有 15 条 DoD 全满足的 Variant 标 IMPLEMENTED。
- 执行 final static searches、完整 checkpoint ZIP、解压复验和独立 Pro 最终审查。


## 本阶段验收 Gate

- [ ] Tests 1-12 与 localization sweep 全部 PASS，无跳过核心行为。
- [ ] 15 recipes 全部由 BuilderCommand 从空白构造并在真实 Runtime 运行；Deferred 部分以最小 primitive fixture 验证且明确不计内容库。
- [ ] Canonical Build/Runtime roundtrip diff=empty；mid-runtime control group 行为等价。
- [ ] 零职业 Domain、零 silent fallback、零 public legacy path、零 completion shortcut。
- [ ] Core/Desktop/Android/Manual Builder smoke 有实际证据；环境阻塞必须解决后才能 FINAL ACCEPT。
- [ ] 独立 Pro Reviewer 从源码和玩家路径复核通过，而不是只审报告。


## 代码处理原则

不再新增模型；DELETE 剩余临时 adapter/feature flag（migration-only 除外）；KEEP final v6 + migration reader + Runtime/QA assets。

## 必须交付

1. `SPD_GC_V6_P12_FINAL_ACCEPTED.zip`：包含完整当前工作区源码，不是 patch-only。
2. `V6_CHECKPOINT_MANIFEST.json`：父 checkpoint hash、当前 ZIP hash、Audit/Contract hash、schema/price versions、测试命令与结果。
3. `PHASE_P12_IMPLEMENTATION_REPORT.md`：实际修改文件、行为变化、Gate 逐项证据、未通过项、仍 Deferred/Unsupported 项。
4. BuilderCommand traces、canonical serialization artifacts、Runtime traces 与测试报告。
5. 对 checkpoint 重新解压后的复验结果。

完成后立即停止，不执行下一阶段。
