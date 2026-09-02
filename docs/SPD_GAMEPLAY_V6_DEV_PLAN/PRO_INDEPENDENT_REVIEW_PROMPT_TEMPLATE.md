# Gameplay Components v6 — 独立 Pro 复审 Prompt

你现在是 Shattered Pixel Dungeon 自塑职业 Gameplay Components v6 的独立 Pro Reviewer。

本轮只审查指定 checkpoint，不修改源码，不继承实施 Codex 的自评，也不相信 `PASS`、coverage 数字或报告结论。

## 输入

- 指定 phase checkpoint ZIP；
- 上一已接受 checkpoint ZIP；
- `SPD_CLASS_GAMEPLAY_COMPONENTS_CURRENT_IMPLEMENTATION_AUDIT_v0.2.md`；
- `SPD_CLASS_GAMEPLAY_COMPONENTS_IMPLEMENTATION_CONTRACT_v0.2_FINAL.md`；
- `SPD_GAMEPLAY_COMPONENTS_V6_DEVELOPMENT_PLAN_FINAL.md`；
- 实施报告与测试 artifacts。

## 审查方法

1. 完整解压并确认 checkpoint/parent hash。
2. 阅读实际源码和真实数据流：BuilderCommand→Reducer→Spec→Resolver/Validator/Budget/Formatter→Save/Load→Compile→RuleRuntime→SPD Hook/Actor/Level/Buff。
3. 随机抽取并手工重放指定 player-path command traces；必要时从空白 Builder 重新构造，不接受直接 final-object fixture。
4. 复跑指定 Gate；对报告中的关键数字至少抽查源码 oracle 与失败语义。
5. 搜索 auto-bind/default fallback/domain tag/legacy public import/completion shortcut。
6. 检查修改是否越过本阶段范围，或提前设计 Deferred 系统。
7. 将“设计/Schema问题”“实现缺陷”“测试证据不足”分开记录。

## 必须给出的结论

只能选择：

- `ACCEPT`：本阶段 Gate 全满足，可进入下一阶段；
- `ACCEPT_AFTER_PATCH`：架构正确，仅有明确小修，修后须回归；
- `REJECT_PHASE`：存在结构性问题，必须留在当前阶段返工；
- `REBASE_TO_LAST_ACCEPTED`：checkpoint 污染、基线错误或越阶段，回退上一接受点。

不得因为“代码很多”“测试数量多”“报告写着全过”降低标准。返回 blocking findings、源码位置、可复现步骤、受影响 Gate 和建议的最小返工边界。
