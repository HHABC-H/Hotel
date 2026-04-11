# 酒店管理系统 - 后端

## 项目结构

```
Hotel/
├── src/main/java/com/hotel/
│   ├── common/          # 公共类
│   │   ├── Constant.java      # 常量定义
│   │   ├── PageQuery.java     # 分页查询
│   │   ├── PageResult.java    # 分页结果
│   │   └── Result.java        # 统一响应
│   ├── config/          # 配置类
│   │   ├── MybatisPlusConfig.java
│   │   └── SecurityConfig.java
│   ├── controller/      # 控制器层
│   │   ├── AuthController.java
│   │   ├── UserController.java
│   │   ├── RoomTypeController.java
│   │   ├── RoomController.java
│   │   ├── CustomerController.java
│   │   └── OrderController.java
│   ├── entity/          # 实体类
│   │   ├── User.java
│   │   ├── RoomType.java
│   │   ├── Room.java
│   │   ├── Customer.java
│   │   └── Order.java
│   ├── mapper/          # Mapper接口
│   │   ├── UserMapper.java
│   │   ├── RoomTypeMapper.java
│   │   ├── RoomMapper.java
│   │   ├── CustomerMapper.java
│   │   └── OrderMapper.java
│   ├── service/         # 服务层
│   │   ├── impl/        # 服务实现类
│   │   └── *.java       # 服务接口
│   ├── util/            # 工具类
│   │   └── JwtUtil.java
│   └── HotelApplication.java
├── src/main/resources/
│   └── application.yml
└── pom.xml
```

## 技术栈

- Spring Boot 2.7.18
- MyBatis-Plus 3.5.4.1
- MySQL 8.0
- JWT (jjwt)
- Lombok

## 快速开始

### 1. 数据库初始化

执行 `hotel.sql` 文件初始化数据库和数据。

### 2. 配置数据库连接

修改 `src/main/resources/application.yml` 中的数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/hotel?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

### 3. 启动项目

```bash
cd Hotel
mvn spring-boot:run
```

或直接运行：

```bash
mvn clean package
java -jar target/hotel-management-1.0.0.jar
```

### 4. 访问API

默认端口：8080

API前缀：`/api`

## API接口

### 认证接口
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/logout` - 用户登出
- `GET /api/auth/current-user` - 获取当前用户信息

### 用户管理
- `GET /api/users` - 用户列表
- `GET /api/users/{id}` - 用户详情
- `POST /api/users` - 新增用户
- `PUT /api/users/{id}` - 更新用户
- `DELETE /api/users/{id}` - 删除用户

### 客房类型
- `GET /api/room-types` - 类型列表
- `GET /api/room-types/{id}` - 类型详情
- `POST /api/room-types` - 新增类型
- `PUT /api/room-types/{id}` - 更新类型
- `DELETE /api/room-types/{id}` - 删除类型

### 客房管理
- `GET /api/rooms` - 客房列表
- `GET /api/rooms/{id}` - 客房详情
- `POST /api/rooms` - 新增客房
- `PUT /api/rooms/{id}` - 更新客房
- `DELETE /api/rooms/{id}` - 删除客房
- `PUT /api/rooms/{id}/status` - 修改客房状态

### 客户管理
- `GET /api/customers` - 客户列表
- `GET /api/customers/{id}` - 客户详情
- `POST /api/customers` - 新增客户
- `PUT /api/customers/{id}` - 更新客户
- `DELETE /api/customers/{id}` - 删除客户

### 订单管理
- `GET /api/orders` - 订单列表
- `GET /api/orders/{id}` - 订单详情
- `POST /api/orders` - 创建订单
- `PUT /api/orders/{id}` - 更新订单
- `DELETE /api/orders/{id}` - 删除订单
- `PUT /api/orders/{id}/status` - 修改订单状态

## 默认数据

系统初始化时会创建以下数据：

- **管理员账户**: 
  - 用户名: admin
  - 密码: admin (BCrypt加密)

- **客房类型**:
  - 标准间 (¥280/晚)
  - 大床房 (¥320/晚)
  - 豪华套房 (¥580/晚)

- **客房**: 5间客房

## 项目说明

- 后端采用 Spring Boot + MyBatis-Plus 构建
- 数据库使用 MySQL 8.0
- 使用 JWT 进行身份认证
- 使用 BCrypt 加密密码
- 支持分页查询
- 统一响应格式
