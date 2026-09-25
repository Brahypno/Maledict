# 测试

`src/test/java` 里是纯逻辑单测（JUnit 5，不启动 Minecraft）。在项目根目录显式执行：

```powershell
.\gradlew.bat test
.\gradlew.bat test --tests '*DamageAdaptationTest'   # 只跑一个类
```

`check` 与 `build` 不依赖 `test`，不会自动跑测试。HTML 报告在 `build/reports/tests/test/index.html`，
首次运行需要下载测试依赖，缓存齐全后可以加 `--offline`。

需要真事件、真实体、真世界或渲染的行为不在单测范围内，只能在游戏里验证。
