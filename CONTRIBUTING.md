# 参与开发指南（CONTRIBUTING）

感谢你参与技能卡 SkillCards 的开发！请先阅读本文件。

## 许可与贡献条款

本项目采用 **MIT** 许可。提交即表示你同意将贡献内容按本仓库 LICENSE（MIT）发布。

## 环境搭建

1. 安装 **JDK 25** 与 IntelliJ IDEA（导入 Gradle 项目即可）
2. `git clone https://github.com/TakaraMiyuki/SkillCards.git` —— 克隆后即可直接编译：
   联动依赖（Manhunt / Flying / Curios）的 jar 已收入 `libs/`，不依赖兄弟仓库路径
3. 本地试玩：将本模组构建 jar 与 [Manhunt](https://github.com/TakaraMiyuki/Manhunt/releases) 0.3.3+ 一起放入 Manhunt 开发实例的 `run/mods/`，或独立安装 Curios 验证被动卡
4. 启动：`./gradlew runClient` / `./gradlew runServer`

## 如何新增一张技能卡

1. **`registry/Card.java`**：加枚举条目
   `新卡ID("id", Grade.品级, Type.ACTIVE 或 PASSIVE, 冷却秒数, 新卡类::activate)`
   —— id 全小写；主动卡填冷却秒数，被动卡填 0
2. **`card/impl/新卡类.java`**：实现 `activate(ServerPlayer)`（返回 false = 发动无效、不进冷却）；
   被动卡的效果写在 `event/CardEvents` 的被动扫描/事件路径中（参考攻势/法轮的写法）
3. **`registry/ModItems.java`**：加 `public static final DeferredItem<SkillCardItem> 新卡 = register(Card.新卡);`
4. **语言文件**：`assets/skillcards/lang/zh_cn.json` 与 `en_us.json` 各加三条
   `item.skillcards.<id>`（卡名）、`item.skillcards.<id>.brief`（一句话概述）、
   `item.skillcards.<id>.desc`（Shift 展开的完整描述）
5. **贴图**：把 `tools/gen_assets.py` 的 CARDS 表加上新条目并运行生成占位贴图
   （或直接放置 16×16 png 到 `assets/skillcards/textures/item/<id>.png` + 同名模型 JSON）
6. **仅被动卡**：把 `skillcards:<id>` 加入 `data/curios/tags/item/skill_passive.json`
   （Manhunt 的被动饰品栏以此校验物品）

## 修改规则

- **平衡数值只改** `CardConfig.java`
- 与 Manhunt 的判定联动通过 `compat/ManhuntHook`（引用收敛在 `compat/ManhuntBridge`，
  manhunt 未加载时回退为敌对生物目标）；Curios 引用收敛在 `compat/CuriosBridge`
- 编译依赖的联动 jar 在 `libs/`（manhunt/flying/curios）——两侧接口变更后需手动替换对应 jar
- 卡面描述、冷却等改动必须同步中英语言文件

## 提交前自查

- [ ] `./gradlew build` 通过
- [ ] `./gradlew runServer` 无 ERROR/FATAL，新卡可获得并正常发动（配合 Manhunt 的
  `/manhunt admin debug card <品级>`）
- [ ] 中英语言文件完整、贴图就位
- [ ] 提交信息使用中文一句话说明行为变化（参照 `git log` 风格）

## 协作流程

1. 从 `main` 拉出功能分支（`feat/xxx` 或 `fix/xxx`）
2. 开发并按自查清单验证
3. 发起 Pull Request 到 `main`，由仓库所有者审核合并
4. 版本号变更与 GitHub Release 由所有者统一发布
