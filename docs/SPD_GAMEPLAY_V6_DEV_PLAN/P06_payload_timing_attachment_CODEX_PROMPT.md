# Codex 执行 Prompt — P06 统一 EffectChain/Payload、Delay/Echo 与 Action Attachment

你现在执行 **Shattered Pixel Dungeon 自塑职业 Gameplay Components v6 — P06**。

## 输入与唯一基线

- 源码输入：`SPD_GC_V6_P05_MARK_MODE_CHECKPOINT.zip`
- 冻结审计：`SPD_CLASS_GAMEPLAY_COMPONENTS_CURRENT_IMPLEMENTATION_AUDIT_v0.2.md`
- 冻结合约：`SPD_CLASS_GAMEPLAY_COMPONENTS_IMPLEMENTATION_CONTRACT_v0.2_FINAL.md`
- 开发计划：`SPD_GAMEPLAY_COMPONENTS_V6_DEVELOPMENT_PLAN_FINAL.md`

本阶段目标：**把即时、延迟、回声和下一次行动统一成同一 EffectChain 语义，修复 Primary/Secondary 被拆分和存档重复执行。**

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

- 从 P05 accepted checkpoint 开始。
- 依据 Contract 第 15、18 中 Delay/Echo、21、33.7、33.8 实施。
- 不实现 Device/Trap/Field；本阶段只完成载荷结构和时间/附件运行时，给 P07 复用。
- 迁移旧 Delay 时必须按实际旧语义记录 warning；不能把两个 legacy scheduled actors 静默伪装为原子链而不报告差异。
- 用至少两种不同 Primary（World/Mark/Resource 中已实现者）证明 Delay 是任意 EffectChain wrapper。
- 返回 scheduled/attachment 中途 save 的 canonical diff=empty 证据。

## 本阶段明确范围

- 完成 EffectChain primary+optional secondary 及 activation：Immediate、DelayAfterPrimarySuccess、OnNextActionAfterPrimarySuccess。
- 完成 `PayloadSpec`，Skill immediate execution 也通过统一 payload/chain 执行。
- 实现 TargetBindingPolicy：ACTOR_ID_LIVE、CELL_SNAPSHOT、SELECTOR_REEVALUATE。
- 重做 ScheduledPayloadState、DelayModifier、EchoModifier，保存整个 chain、origin IDs、cause、remaining、execution count。
- 实现 ActionAttachmentDelivery/State 与下一次行动 charge/expiry；不得复制已支付 Cost。
- 固定 wrapper 执行顺序、Primary 成功条件、同一 actor identity、save/load 中间态和递归 guard。
- 适配现有 RuleDelayedPayload/RuleContext/Trace，保留其 Actor/Bundle/causality 基础。
- 同步 Builder Payload/Secondary editor、Formatter、Budget、Localization、Migration。


## 本阶段验收 Gate

- [ ] Mandatory Test 6 完整通过：Selected Cell、3 turns、任意 supported EffectChain、单 ScheduledPayload、save/load 后精确一次。
- [ ] Mandatory Test 7 完整通过：受击减伤、下一次 ATTACK_HIT 附加 Poison、charge/expiry/save-load/不递归。
- [ ] Primary BLOCKED/FAILED 时 Secondary 不创建；Immediate secondary 保持 actor identity。
- [ ] Delay/Echo 对任意已支持 Effect 不需要新增专用 enum。
- [ ] Canonical RuntimeState 能检测 remaining、binding、origin、attachment charge 的任何丢失。
- [ ] Martial Defender Recipe 的 Counter Attachment 最低机制完成，从而关闭 Recipe 1。


## 代码处理原则

KEEP/ADAPT RuleDelayedPayload 与因果 guard；REPLACE Primary/Secondary 分拆调度；NEW unified Payload/Attachment runtime。

## 必须交付

1. `SPD_GC_V6_P06_PAYLOAD_TIMING_CHECKPOINT.zip`：包含完整当前工作区源码，不是 patch-only。
2. `V6_CHECKPOINT_MANIFEST.json`：父 checkpoint hash、当前 ZIP hash、Audit/Contract hash、schema/price versions、测试命令与结果。
3. `PHASE_P06_IMPLEMENTATION_REPORT.md`：实际修改文件、行为变化、Gate 逐项证据、未通过项、仍 Deferred/Unsupported 项。
4. BuilderCommand traces、canonical serialization artifacts、Runtime traces 与测试报告。
5. 对 checkpoint 重新解压后的复验结果。

完成后立即停止，不执行下一阶段。
