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
## 注解式
使用方式完全兼容Spring原生注解，并通过自定义注解做了一些增强,更细粒度的控制过期时间。
### 自定定义注解
使用自定义注解可以为每个Cache指定自己的ttl
```java
// package wang.liangchen.matrix.cache.sdk.annotation;
@Cacheable(value = "CacheObject", ttlMs = 1000 * 60 * 5)
```
### Spring原生注解
```java
@Cacheable(value = "CacheObject")
```
## 编程式
```java
@Inject
private MultilevelCacheManager cacheManager;
```