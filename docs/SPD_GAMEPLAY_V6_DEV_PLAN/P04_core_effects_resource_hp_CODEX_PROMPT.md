# Codex 执行 Prompt — P04 核心 Effect Families、Resource Transaction、HP 边界与基础 Class Components

你现在执行 **Shattered Pixel Dungeon 自塑职业 Gameplay Components v6 — P04**。

## 输入与唯一基线

- 源码输入：`SPD_GC_V6_P03_TYPED_SKILL_CHECKPOINT.zip`
- 冻结审计：`SPD_CLASS_GAMEPLAY_COMPONENTS_CURRENT_IMPLEMENTATION_AUDIT_v0.2.md`
- 冻结合约：`SPD_CLASS_GAMEPLAY_COMPONENTS_IMPLEMENTATION_CONTRACT_v0.2_FINAL.md`
- 开发计划：`SPD_GAMEPLAY_COMPONENTS_V6_DEVELOPMENT_PLAN_FINAL.md`

本阶段目标：**完成现阶段通用战斗语言的核心执行面，并以 Rage -2→Focus +5 证明强类型参数、原子事务和玩家路径真实成立。**

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

- 从 P03 accepted checkpoint 开始。
- 优先完成 Contract Test 1；不要只给旧 Convert UI 增字段，必须让 Class Flow、Skill Effect、ClassOperation 共用 `ResourceOperationSpec` 与 transaction。
- 对每个 core family 建立最小但真实的 player-exposed variant set，严格按 Contract required schema；未定义 policy 保持隐藏。
- 所有行为测试必须来自 command-built ClassBuild；direct object 测试只能作为额外 runtime smoke。
- 为所有 exposed fields 建 NumberStepper/selector/ref picker；禁止 generic strength/power 页面。
- 输出核心 Variant completion matrix，并逐行列出实际 test IDs；缺证据的行保持 DECLARED/UNSUPPORTED。

## 本阶段明确范围

- 完成 required Damage：Direct、PercentMaxHP、MissingHP、Execute；未冻结/未测试 policy 不暴露。
- 完成 Status 白名单与 ApplyStatus；禁止 Java Buff class player schema。
- 完成 Movement：Push/Pull/Throw/Dash/Teleport/SwapPosition，并统一合法 cell/碰撞失败语义。
- 完成 Recovery/Defense：Heal、Barrier、TemporaryHP、Mitigate、Redirect、Cleanse，冻结堆叠/结算顺序。
- 完成 ResourceOperation：Gain/Drain/Set/Clear/Convert/Reserve/Suppress；Hero holder 与 HUD 适配。
- 实现 `ResourceTransaction`，Convert/未来 Transfer 复用；EXACT_ATOMIC 是唯一 Convert 模式。
- 实现 BuiltinStatRef/ValueSpec/conditions 与 HP Cost；HP 不得进入 Resource declarations。
- 完成本阶段所需 Delivery、Targeting、filters、Repeat/Intensity/Extend/Pierce/Bounce、Resource/HP/Action/Cooldown/Item Cost。Delay/Echo 留 P06。
- 实现 BasicAttackComponent、ResourceFlowComponent、ActiveResourceOperationComponent、Resource ClassOperation 与稳定 HUD ID。
- 同步每项 formatter、en/zh、Budget entries、migration mapping、save/load 和行为测试。


## 本阶段验收 Gate

- [ ] Mandatory Test 1 的 Skill/ClassOperation 核心部分通过：Rage 5→3、Focus 0→5；source不足/target空间不足均原子不变。Device Payload 同 executor 的最终条款在 P07 关闭，因此本阶段不得把 Test 1 报为最终 COMPLETE。
- [ ] HP Cost/Low HP/Missing HP/Heal/Barrier/TempHP 全部证明不创建 HP Resource；Barrier/TempHP 不代付 HP Cost。
- [ ] 核心 Effect 每个 IMPLEMENTED row 都有 Layer A-E 证据；无 giant boolean coverage。
- [ ] Ammo Gunner、Blood Berserker、Area Caster 的最低通用 recipe 可由 commands 构造并真实运行；不使用职业标签。
- [ ] Resource HUD 使用 declaration+state 分离，rename 后显示更新且引用不变。


## 代码处理原则

REWORK `SkillEffectRuntime` 为 typed executors；KEEP SPD 原生 helper；REWORK ResourceSpec/State、Class components/operations；旧 Convert/HP 路径仅迁移。

## 必须交付

1. `SPD_GC_V6_P04_CORE_EFFECTS_CHECKPOINT.zip`：包含完整当前工作区源码，不是 patch-only。
2. `V6_CHECKPOINT_MANIFEST.json`：父 checkpoint hash、当前 ZIP hash、Audit/Contract hash、schema/price versions、测试命令与结果。
3. `PHASE_P04_IMPLEMENTATION_REPORT.md`：实际修改文件、行为变化、Gate 逐项证据、未通过项、仍 Deferred/Unsupported 项。
4. BuilderCommand traces、canonical serialization artifacts、Runtime traces 与测试报告。
5. 对 checkpoint 重新解压后的复验结果。

完成后立即停止，不执行下一阶段。
