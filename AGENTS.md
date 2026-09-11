# 项目概览

这是一个股票数据采集、指标计算和查询服务。系统负责维护交易日历，采集股票、指数、板块及资金流向数据，计算市场水位、MACD 背离、板块指标和选股结果，并通过 REST API 对外提供查询能力。

第一阶段采用模块化单体架构。除非需求明确且已有充分依据，不要擅自拆分微服务、引入消息队列或进行大范围架构重构。

# 需求文档

- 试用期功能需求文档：`C:\Users\17\Desktop\产品组-试用期2.docx`
- 分析和实现试用期需求时，以该文档为准。

# 技术栈

- 语言与运行环境：JDK 17
- 构建工具：Maven 3.9.6，优先使用项目 Maven Wrapper
- 后端：Spring Boot 3.0.2、Spring MVC
- 微服务组件基线：Spring Cloud Alibaba 2022.0.0.0
- JSON：FastJSON2 2.0+
- 数据访问：MyBatis-Plus 3.5+、MyBatis XML
- 数据库：MySQL 8
- 缓存：Redis
- 数据库迁移：Flyway
- 接口文档：Springdoc OpenAPI、Apifox
- 测试：JUnit 5、Mockito；需要数据库集成测试时使用 Testcontainers
- 历史数据初始化：允许使用 Python、AkShare 或 CSV 作为离线辅助工具，但不能让核心服务依赖 Python 进程运行

# 依赖版本规范

- Spring Boot 固定使用 `3.0.2`，未经确认不得自行升级大版本或替换父 POM。
- Spring Cloud Alibaba 使用 `2022.0.0.0` 版本基线；引入 Nacos、Sentinel、Seata 等组件前，必须核对该版本对应的 Spring Cloud、Spring Boot 和具体组件版本。
- Spring Cloud Alibaba 组件统一通过 BOM 或项目既有依赖管理引入，禁止在业务模块中随意覆盖传递依赖版本。
- MyBatis-Plus 使用 `3.5+`，具体版本应在父 POM 或 `dependencyManagement` 中统一管理。
- FastJSON2 使用 `2.0+`，具体版本必须集中声明；禁止同一项目同时使用多个 FastJSON/FastJSON2 版本。
- 新增依赖前先检查是否与 JDK 17、Spring Boot 3.0.2 及公司组件基线兼容，禁止使用 `LATEST`、`RELEASE` 或不固定的动态版本。
- 不因为技术栈中包含 Spring Cloud Alibaba 就默认拆分微服务；只有明确业务需求时才引入对应组件。

# 架构与职责

- Controller 只负责参数校验、权限边界和响应封装，不包含业务计算、数据采集或 SQL 拼装。
- Service 负责业务编排，指标公式应下沉到可独立测试的计算组件。
- Service 负责编排外部数据源调用、限频、分批、重试、解析和数据校验，复杂操作可按职责下沉到 Face。
- Mapper 接口只定义数据访问方法，所有 SQL 统一写在 MyBatis XML 中，禁止在 Java 中拼接 SQL 或使用 SQL 注解。
- Task 只负责定时触发和流程编排，不直接堆叠采集、计算与持久化细节；复杂操作封装到 Face 或 Service。
- 不同外部数据源必须通过独立适配器隔离，禁止把第三方字段结构泄漏到领域模型和 Controller。

# 业务职责范围

- 交易日历：交易日判断、前后第 N 个交易日计算。
- 市场数据：股票基础信息、实时行情、日线、指数和资金流向。
- 板块数据：板块、成分股、自定义四大板块及板块日线。
- 指标计算：均线、涨幅、振幅、MACD、DIF、DEA。
- 信号和选股：涨速、涨停或开板提醒、指数背离及各类选股策略。
- 统计分析：市场水位、周月对比、股票家数和指数差异分布。

顶层物理包保持技术分层，不建立 `calendar`、`market` 等一级领域包。`controller` 和 `service` 内部按实际业务模块建立子包，当前模块名统一为 `tradecalendar`、`stockbasic`、`stockdailyquote`、`stockfundflow`、`marketlevel`、`stockalert`、`plate`、`indexdivergence`、`indexstyle`；Service 实现放在对应模块的 `impl` 子包。

# 目录结构

```text
src/main/java/cn/djct/stockdemo/
├─ common/              # Service 与 Task 可复用的公共内容
├─ config/              # 配置类
├─ constant/            # 常量和枚举
├─ controller/          # Web 接口，按业务模块分子包
│  ├─ indexdivergence/
│  ├─ indexstyle/
│  ├─ marketlevel/
│  ├─ plate/
│  ├─ stockalert/
│  ├─ stockbasic/
│  ├─ stockdailyquote/
│  ├─ stockfundflow/
│  └─ tradecalendar/
├─ face/                # Task 相关的复杂操作和计算封装
├─ mapper/              # MyBatis Mapper 接口
├─ pojo/
│  ├─ dto/              # Web请求参数及中间层传输对象，名称以 Dto 结尾
│  ├─ entity/           # 数据源和数据库实体
│  └─ vo/               # Web响应对象，名称以 Vo 或 RespVo 结尾
├─ service/             # Service接口，按业务模块分子包
│  ├─ indexdivergence/
│  │  └─ impl/
│  ├─ indexstyle/
│  │  └─ impl/
│  ├─ marketlevel/
│  │  └─ impl/
│  ├─ plate/
│  │  └─ impl/
│  ├─ stockalert/
│  │  └─ impl/
│  ├─ stockbasic/
│  │  └─ impl/
│  ├─ stockdailyquote/
│  │  └─ impl/
│  ├─ stockfundflow/
│  │  └─ impl/
│  └─ tradecalendar/
│     └─ impl/
├─ task/                # 定时任务触发和编排
└─ util/                # 无状态工具类

src/main/resources/
├─ application.yml
├─ db/migration/
└─ mapper/              # MyBatis XML

src/test/java/cn/djct/stockdemo/  # 按被测分层和业务模块组织

scripts/                # 历史数据回补及其固定样本测试
plan/                   # 项目推进记录和当前状态
```

第一层包名统一以 `cn.djct.{模块名称标识}` 为前缀，本项目根包固定为 `cn.djct.stockdemo`。只创建当前功能实际需要的分层包，不要为了形式创建空目录或空接口。

# 代码风格

- 类名使用 UpperCamelCase，如 `TradeCalendarService`。
- 方法和变量使用 lowerCamelCase，如 `findPreviousTradeDate`。
- 常量使用 UPPER_SNAKE_CASE，如 `MAX_SYMBOLS_PER_REQUEST`。
- 包名全部小写，使用 `cn.djct.stockdemo` 作为根包。
- 前端传给后端的Web请求参数统一使用DTO接收，放在 `pojo.dto`，类名以 `Dto` 结尾；`@RequestBody` 禁止使用VO。
- Service与Controller之间的结果若与接口VO字段、类型和业务含义一致，直接使用VO，允许Service及计算组件直接构造VO，禁止仅为分层复制一套DTO并逐字段转换。确有内部字段、单位转换、脱敏或不同响应结构时才保留独立DTO；禁止直接传递数据库实体作为公共API响应。
- 数据源和数据库实体放在 `pojo.entity`。
- 后端返回前端的Web响应对象统一使用VO，放在 `pojo.vo`，类名以 `Vo` 或 `RespVo` 结尾。
- 定时任务类放在 `task`，复杂计算和操作放在 `face`，禁止在 Task 中堆积业务实现。
- REST 接口路径的每个路径段使用 lowerCamelCase，例如 `/api/tradeCalendar/previousTradingDay`；禁止使用连字符或下划线分隔单词。
- 优先使用构造器注入，禁止字段注入。
- 注入的依赖字段及对应构造器参数使用完整类型名的 lowerCamelCase 形式，保留业务前缀，不使用 `mapper`、`service`、`calculator` 等泛称或缩写。例如 `PlatformBreakoutCalculator platformBreakoutCalculator`、`StockDailyQuoteMapper stockDailyQuoteMapper`；测试中的对应依赖也遵循此规则。
- 使用明确的请求DTO、响应VO和领域对象，禁止直接把数据库实体作为请求参数或公共API响应。
- 公共接口中的日期使用 `LocalDate`，分钟时间使用 `LocalDateTime`；明确采用 `Asia/Shanghai` 时区。
- 金额、价格、比例和指标计算使用 `BigDecimal`；禁止用 `double` 直接存储或比较金融数据。
- 股票代码使用字符串，保留前导零；市场前缀转换集中封装，禁止散落硬编码。
- 避免无意义注释：不得只翻译Java或SQL语法、重复类名方法名，或描述一眼可见的赋值和流程。
- 对业务目的、数据来源、单位转换、日期边界、异常原因、完整性守门和非直观流程，应添加适量注释帮助理解。
- 复杂指标和统计公式必须说明数据区间、计算口径、分母含义、边界是否包含以及最终单位。
- 公共接口和Mapper方法使用简洁Javadoc说明职责；参数、返回值或空值语义不直观时补充 `@param`、`@return`。
- MyBatis XML中的每个SQL语句前使用简洁的 `<!-- ... -->` 注释说明其数据访问目的；排序、时间范围、状态过滤或幂等策略属于关键口径时一并说明，但不要逐句翻译SQL。
- 全部源文件使用 UTF-8 编码。

# 数据访问规范

- 默认一张表对应一个 Mapper，Mapper 名称体现所负责的表；禁止将多张表各自独立的增删改查集中到一个业务功能 Mapper 中。例如板块与板块成分关系分别由 `StockPlateMapper`、`StockPlateMemberMapper` 负责。
- 跨表业务编排和事务由 Service 负责，通过调用各表对应的 Mapper 完成；不能为了减少 Service 依赖而混合 Mapper 职责。
- 确需关联查询时，SQL 放在主要查询结果所属的 Mapper 中，允许关联其他表获取字段或筛选条件；不要为了机械满足一表一 Mapper 将合理关联查询拆成多次请求。
- 新增 Mapper 方法前，必须检查现有 Mapper 的查询条件、返回字段、排序和状态过滤是否可复用。相同数据访问语义优先复用已有方法；字段不足时评估补充字段并统一返回类型，禁止仅因业务功能不同复制近似 SQL 或重复创建传输对象。
- Mapper 方法按数据访问含义命名，明确查询条件，避免用调用它的业务功能命名通用查询。例如按股票代码和交易日期查询日线使用 `selectByStockCodesAndTradeDates`，不命名为 `selectTechnologyStockQuotes` 或含义模糊的 `selectQuotes`。
- 查询范围、状态过滤、排序或必要字段确有差异的方法可以保留，并在注释中说明用途；不为追求方法数量少而合并成复杂的万能查询。合并重复方法时同步更新调用方、XML和测试，删除本次修改产生的废弃方法及对象。

- 所有 SQL 必须写在 `src/main/resources/mapper/` 下的 MyBatis XML 文件中，包括简单 CRUD 和复杂聚合查询。
- 查询 SQL 只负责筛选并返回业务计算所需的数据，禁止在 SQL 中使用 `SUM`、`AVG`、`ROUND`、`CASE`、`GROUP BY` 等实现业务汇总、指标公式或业务分类；相关计算统一放在 Service 或可独立测试的计算组件中。
- `COUNT`、`MAX`、`MIN` 仅可用于存在性判断、分页总数、数据完整性统计、最新或最早记录定位等数据访问语义，不得用于实现业务指标计算。
- 禁止使用 `@Select`、`@Insert`、`@Update`、`@Delete`、`@SelectProvider` 等注解方式定义 SQL。
- Mapper 接口的方法签名、参数名、返回类型必须与对应 XML 的 namespace、statement id 和 result mapping 保持一致。
- 对外列表查询必须分页，并设置合理的最大分页大小。
- 批处理任务必须使用分页、游标或分块读取，禁止一次性加载全表。
- 日线数据以 `(stock_code, trade_date)` 建立唯一约束；分钟数据以代码、交易日和分钟时间建立唯一约束。
- 定时采集和计算任务必须幂等，重复执行不得产生重复数据。
- 批量写入优先使用数据库批处理，避免在循环中逐条提交事务。
- 事务边界放在 Service 层；外部 HTTP 请求不得长时间占用数据库事务。
- 表结构变更必须新增 Flyway 迁移，禁止直接修改已发布迁移脚本。

# 外部数据采集规范

- 严格遵守数据源的调用频率和单次代码数量限制；腾讯批量行情每次最多拼接 200 个代码。
- 所有请求必须设置连接超时和读取超时，并记录数据源、批次、耗时和失败原因。
- 重试必须限制次数并使用退避策略，禁止无上限重试或高频轮询。
- 解析第三方文本时显式处理字符集、单位和空值；原始单位转换必须集中管理。
- 禁止在测试中直接高频调用公开行情网站；解析测试应使用脱敏后的固定样本。
- 数据不完整时应标记采集失败或部分成功，禁止静默写入零值冒充真实行情。
- 新增网络请求或更换数据源前，必须先确认必要性、可用性和合规边界。

# 交易日与定时任务规范

- 所有盘中、盘后任务执行前必须校验交易日，不能只判断周末。
- 交易日历是最终判断依据，并支持人工修正特殊休市日期。
- 盘中任务还必须校验交易时间段：09:30-11:30、13:00-15:00。
- 定时任务必须记录开始时间、结束时间、状态、处理数量和错误摘要。
- 多实例部署时必须使用分布式锁，避免同一任务重复执行。
- 下游计算任务必须确认上游数据已完整落库，禁止仅依赖固定延时猜测数据就绪。

# 测试规范

- 行为变化必须新增或更新测试。
- 均线、N 日涨幅、振幅、MACD、背离和选股条件必须优先编写纯单元测试。
- 日期相关测试必须覆盖周末、节假日、跨月、跨年和前后第 N 个交易日。
- 采集解析必须包含正常数据、空响应、字段缺失、乱码和数据源格式变化测试。
- 数据库唯一约束、幂等写入和分页查询应使用集成测试验证。
- 新增核心业务代码的测试覆盖率目标不低于 80%，不得通过无断言测试凑覆盖率。

# 安全与配置

- 禁止在代码、配置、日志或测试样本中提交数据库密码、Token、Cookie、代理凭证和内网敏感信息。
- 数据库、Redis 和第三方服务配置通过环境变量或本地未跟踪配置提供。
- 提供示例配置时仅使用占位值。
- 日志使用 SLF4J，禁止使用 `System.out.println`。
- 日志中不得输出完整凭证、Cookie、数据库连接串或大段第三方原始响应。

# 实现范围控制

- 严格围绕用户当前明确提出的需求实现；用户提供代码样例时，优先保持样例的结构、命名和复杂度。
- 默认采用满足当前需求的最小可用实现，不主动扩展需求范围。
- 不以“未来可能需要”为理由提前增加抽象层、扩展点、依赖、异常体系或基础设施。
- 能通过修改现有文件完成时，不为形式完整而新增无实际用途的类、接口或目录。
- 如确有必要引入超出当前需求的公共设计，必须先说明具体原因和影响，得到用户确认后再实现。
- 可以提出后续优化建议，但未经确认只记录建议，不直接写入代码。

# 工作流程

1. 先阅读相关模块、数据表和测试，确认现有风格与数据口径。
2. 编码前确认接口入参、返回结构、计算公式、交易日范围和数据来源。
3. 优先进行小范围、易审查的改动，避免无关重构。
4. 完成代码后补充单元测试和必要的集成测试。
5. 优先运行最快且最相关的测试，再运行完整检查：

```bash
./mvnw test
```

Windows PowerShell 可运行：

```powershell
.\mvnw.cmd test
```

6. 涉及接口变更时同步更新 OpenAPI 注解或 Apifox 文档；涉及表结构时提交对应 Flyway 脚本。
7. 提交信息使用 `type(scope): description`，例如 `feat(market): 新增股票日线采集任务`。
8. 每次实现功能、推进项目阶段、调整架构或解决重要问题后，必须在根目录 `plan` 下新增一份 `YYYY-MM-DD-NNN-topic.md` 推进记录，并同步更新 `plan/PROJECT_STATUS.md`；仅文字修正、格式整理等无行为变化的小改动可以不单独记录。

# 禁止事项

- 禁止硬编码数据库连接、Redis 地址、敏感凭证和生产环境地址。
- 禁止绕过 Service 层从 Controller 直接调用 Mapper。
- 禁止在业务代码中复制粘贴指标公式或股票市场前缀判断。
- 禁止未经确认改变复权方式、涨跌停规则、成交额单位或交易日口径。
- 禁止用固定 `sleep` 代替限频器、任务状态或数据就绪判断。
- 禁止在未补充测试的情况下修改公共计算组件和公共工具类。
- 禁止频繁请求第三方行情接口进行开发调试。
- 禁止在未确认需求的情况下擅自重构现有模块或引入重量级基础设施。
