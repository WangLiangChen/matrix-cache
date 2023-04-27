# matrix-cache

A transparent multilevel cache framework based on spring cache.

# Maven依赖

## 配置repository
因SNAPSHOT版本未同步至中央仓库，故需配置如下仓库

```xml
<repositories>
    <repository>
        <id>matrix-snapshot</id>
        <url>https://s01.oss.sonatype.org/content/repositories/snapshots/</url>
        <snapshots>
            <enabled>true</enabled>
            <updatePolicy>always</updatePolicy>
        </snapshots>
    </repository>
</repositories>
```
## Maven依赖
```xml

<dependency>
    <groupId>wang.liangchen.matrix.cache</groupId>
    <artifactId>matrix-cache-sdk-spring-boot-starter</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```
## 其它可选依赖
组件引用到的依赖项，部分为provided依赖，需要根据实际的使用场景自行引入
### 仅使用Caffeine本地缓存
```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```
### 仅使用Redis分布式缓存
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>    
</dependency>
```
### 使用多级缓存
```xml
<dependencies>
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
    </dependency>
</dependencies>
```
### Redis使用Protostuff序列化
```xml
<dependencies>
    <dependency>
        <groupId>io.protostuff</groupId>
        <artifactId>protostuff-runtime</artifactId>
    </dependency>
    <dependency>
        <groupId>io.protostuff</groupId>
        <artifactId>protostuff-core</artifactId>
    </dependency>
</dependencies>
```

# 使用方式

## 启用多级缓存

使用注解@EnableMatrixCaching启用多级缓存

```java
@EnableMatrixCaching
public class ApplicatonConfiguration {}
```

## 注解式

完全兼容Spring原生注解,增强使用@CacheExpire注解指定Cache级别的过期时间

### Spring原生注解

```java
import java.util.concurrent.TimeUnit;

public class ExampleClass{
    @Cacheable(value = "CacheObject")
    // 使用该注解为Cache指定过期时间
    @CacheExpire(ttl = 5, timeUnit = TimeUnit.SECONDS)
    public String exampleMethod(){}
}
```

## 编程式

```java
public class ExampleClass {
    @Inject
    private MultilevelCacheManager matrixCacheManager;
}
```