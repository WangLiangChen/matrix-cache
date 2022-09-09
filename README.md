# matrix-cache

A transparent multilevel cache framework based on spring cache.

# Maven依赖

因SNAPSHOT版本未同步至中央仓库，故需配置repository

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

```xml

<dependency>
    <groupId>wang.liangchen.matrix.cache</groupId>
    <artifactId>matrix-cache-sdk-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

# 使用方式

## 启用多级缓存

使用注解@EnableMatrixCaching启用多级缓存

```java
@EnableMatrixCaching
```

## 注解式

完全兼容Spring原生注解,增强使用@CacheExpire注解指定Cache级别的过期时间

### Spring原生注解

```java
import java.util.concurrent.TimeUnit;

@Cacheable(value = "CacheObject")
// 使用该注解为Cache指定过期时间
@CacheExpire(ttl = 5, timeUnit = TimeUnit.SECONDS)
```

## 编程式

```java
@Inject
private MultilevelCacheManager cacheManager;
```