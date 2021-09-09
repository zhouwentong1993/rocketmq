## 构建项目源代码

进入到 rocketmq 路径下，执行 `mvn clean install -Prelease-all -Dmaven.test.skip=true` 。

**注意：本人对脚本做过更改，否则提示找不到启动类，无法正常启动。**

## 启动 name server

`cd distribution`，执行 `bash bin/mqnamesrv` 当出现下面的提示信息时，代表 name server 启动完成。

```java
Java HotSpot(TM) 64-Bit Server VM warning: Using the DefNew young collector with the CMS collector is deprecated and will likely be removed in a future release
Java HotSpot(TM) 64-Bit Server VM warning: UseCMSCompactAtFullCollection is deprecated and will likely be removed in a future release.
The Name Server boot success. serializeType=JSON
```

## 启动 broker

仍然在 distribution 目录，执行 `bash bin/mqbroker -n localhost:9876` 当出现下面的提示信息时，代表 broker 启动完成。

```java
The broker[your-desired-host-name, 192.168.0.105:10911] boot success. serializeType=JSON and name server is localhost:9876
```

此时，name server 和 broker 就启动成功了。

## 启动控制台

git clone [https://github.com/apache/rocketmq-dashboard.git](https://github.com/apache/rocketmq-dashboard.git)

在 resources/application.properties 文件中配置 name server 地址

`rocketmq.config.namesrvAddr=localhost:9876`

执行 main 方法启动项目，访问 localhost:8080