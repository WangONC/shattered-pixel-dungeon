# Codex 执行 Prompt — P01 Stable Identity、声明/状态分离、Dependency 与 Canonical Save Core

你现在执行 **Shattered Pixel Dungeon 自塑职业 Gameplay Components v6 — P01**。

## 输入与唯一基线

- 源码输入：`SPD_GC_V6_P00_BASELINE_CHECKPOINT.zip`
- 冻结审计：`SPD_CLASS_GAMEPLAY_COMPONENTS_CURRENT_IMPLEMENTATION_AUDIT_v0.2.md`
- 冻结合约：`SPD_CLASS_GAMEPLAY_COMPONENTS_IMPLEMENTATION_CONTRACT_v0.2_FINAL.md`
- 开发计划：`SPD_GAMEPLAY_COMPONENTS_V6_DEVELOPMENT_PLAN_FINAL.md`

本阶段目标：**先建立所有后续组件共同依赖的 v6 身份、声明、引用、依赖诊断和无损序列化基础，彻底阻断 v6 自动补绑。**

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

- 从 P00 accepted checkpoint 开始；不得直接重新使用原始 7z。
- 按 Contract 第 4、5、6.1-6.3、8.1、9.1、10.1、25、26.1-26.3 实现 v6 identity/declaration/save core。
- 迁移框架只能建立 skeleton 与 Resource 基础映射；不要提前实现 Skill Effect、Entity Runtime 或 Builder UI。
- 为所有引用类型建立参数化 stable-reference unit suite，结果先归 Layer A/C/E，不冒充 Player-path acceptance。
- 删除/禁用 v6 对 `resolvePendingBindings()` 的所有依赖；添加自动化搜索/architecture test，确保 v6 package 无调用。
- 输出 canonical JSON 示例、unresolved diagnostic 示例和 schema-version load matrix。

## 本阶段明确范围

- 实现 `StableId`、可注入/确定性 `IdGenerator`、`DisplayName` 验证、所有 Contract typed Ref。
- 实现 `DependencyState`、Resolver、Diagnostic、重复 ID/类型错误/非法循环/Unsupported 检测；Resolver 只读，不修改 Spec。
- 建立 `ClassBuildSpec` schema 6 和独立 `ClassRuntimeState`；玩家顺序与 node IDs 保存。
- 建立 Resource、Mark、ModeGroup、Mode、EntityCapacity、Entity、AbilityPool、Property、Recipe 等声明类型的 v6 数据边界；运行能力可暂为 DECLARED/UNSUPPORTED。
- 实现 rename/delete/rebind 的纯语义服务：rename 不改 ID，delete 不级联，rebind 必须显式。
- 实现 canonical semantic serializer/deserializer 与深等价 oracle；覆盖全部本阶段字段，禁止弱 fingerprint。
- Hero Bundle 分离 `class_build_spec_v6` 与 `class_runtime_state_v6`；未知更高版本明确 UNSUPPORTED。
- 建立 v5→v6 migration 框架、确定性 ID namespace 与 `MigrationReport` 骨架；具体 Variant 映射随阶段补齐。
- 新 v6 代码中零 `resolvePendingBindings`、零 first-item fallback；旧调用只能留在明确 legacy 隔离区。


## 本阶段验收 Gate

- [ ] ID 格式、唯一性、duplicate 新 ID、rename ID 不变、save/load 字节等价测试通过。
- [ ] Resource/Mark/Mode/Entity/Capacity/AbilityPool/Property 的 delete→UNRESOLVED、同名重建不接管、显式 rebind 测试通过。
- [ ] Canonical roundtrip 覆盖每个字段；修改任一语义字段必然改变 canonical output。
- [ ] Load/Resolver/Validator 对 Spec 零 mutation，有对象深拷贝或 immutable 断言。
- [ ] 未知 schema/variant 不会映射成默认效果。


## 代码处理原则

REWORK `ClassBuild`/Resource declaration-state；新建 typed refs/resolver/serializer；旧 `ClassBuild`、旧字段袋进入 LEGACY；v6 路径禁止自动补绑。

## 必须交付

1. `SPD_GC_V6_P01_IDENTITY_CHECKPOINT.zip`：包含完整当前工作区源码，不是 patch-only。
2. `V6_CHECKPOINT_MANIFEST.json`：父 checkpoint hash、当前 ZIP hash、Audit/Contract hash、schema/price versions、测试命令与结果。
3. `PHASE_P01_IMPLEMENTATION_REPORT.md`：实际修改文件、行为变化、Gate 逐项证据、未通过项、仍 Deferred/Unsupported 项。
4. BuilderCommand traces、canonical serialization artifacts、Runtime traces 与测试报告。
5. 对 checkpoint 重新解压后的复验结果。

完成后立即停止，不执行下一阶段。
