# Controller与Service模块包结构调整

## 目标

解决所有 Controller、Service 平铺在同一包下导致模块边界不清晰的问题，并按现有业务模块拆分子包。同时为本次新增的市场水位方法补充与项目既有风格一致的 Javadoc 和关键步骤注释。

## 包结构调整

顶层继续采用技术分层，只在 `controller` 和 `service` 内按业务模块建立子包，没有改成顶层领域分包。

Controller 调整为：

```text
controller/
├─ marketlevel/
├─ stockbasic/
├─ stockdailyquote/
├─ stockfundflow/
└─ tradecalendar/
```

Service 调整为：

```text
service/
├─ marketlevel/
│  └─ impl/
├─ stockbasic/
│  └─ impl/
├─ stockdailyquote/
│  └─ impl/
├─ stockfundflow/
│  └─ impl/
└─ tradecalendar/
   └─ impl/
```

Service 接口、同步服务、数据源接口及对应实现均移动到所属模块。Face、Task、Controller 和测试代码中的导包已同步修改。

测试包按相同模块结构调整，依赖包内构造器的固定样本测试继续与对应实现处于同一包，未通过扩大构造器可见性规避测试问题。

## 市场水位注释

为本次新增的市场水位代码补充：

- Controller 查询方法 Javadoc、查询和响应转换步骤注释。
- Service 构造器、查询方法、统计日选择方法的 Javadoc 和业务步骤注释。
- 腾讯数据源构造器、实时查询、HTTP 请求、响应解析、金额解析和限频方法的 Javadoc。
- 计算组件的参数、返回值、风格判断、数据校验和关键公式步骤注释。
- Service 接口和 Mapper 新增方法的 `@param`、`@return` 说明。

注释只说明业务口径、单位、日期范围和不直观的步骤，没有给简单赋值或 Lombok 生成方法增加冗余注释。

## 保持不变

- 所有类名保持不变。
- REST 接口路径和请求响应保持不变。
- Spring Bean 默认名称保持不变。
- MyBatis Mapper namespace 和 SQL 保持不变。
- 未新增依赖、配置、数据表或迁移脚本。
- 未调整任何业务计算口径。

## 验证

包移动后首次增量测试识别到 `target/classes` 中残留的旧包 Controller 字节码。执行 Maven `clean` 清除旧构建产物后重新验证，确认不是源码或导包冲突。

验证结果：

```text
包声明与目录一致性检查：通过
旧Service导包和旧Controller/Service包声明扫描：0处
市场水位相关测试：21个通过
完整Maven测试：93个通过，0失败，0错误，0跳过
```
