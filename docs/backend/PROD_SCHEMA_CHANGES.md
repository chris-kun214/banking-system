# 生产环境手动建表/加列清单

项目目前没有引入 Flyway/Liquibase 迁移工具，`application-prod.yml` 用 `HIBERNATE_DDL_AUTO=validate`（不自动改表结构）。本地/测试环境 `ddl-auto=update` 会自动建表加列，但**生产环境每次实体加字段都需要手动执行下面的 DDL**，否则应用启动时 Hibernate 校验会失败。

这是已知的技术债（见 [CLAUDE.md](../../CLAUDE.md) Roadmap），后续引入 Flyway 后这份清单可以废弃。

## `transaction` 表新增列（LLM 交易描述推荐功能）

```sql
ALTER TABLE transaction ADD COLUMN description VARCHAR(255);
ALTER TABLE transaction ADD COLUMN ai_description VARCHAR(255);
ALTER TABLE transaction ADD COLUMN ai_category VARCHAR(50);
```

## `reconciliation_discrepancy` 表（每日对账 Lambda）

不需要手动建表——`infra/lambda/daily_reconciliation/handler.py` 每次运行都会先执行 `CREATE TABLE IF NOT EXISTS`，首次调用自动建表。
