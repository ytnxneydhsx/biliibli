# ===== 第一阶段：构建 WAR =====
FROM maven:3.8.7-eclipse-temurin-8 AS builder

WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests

# ===== 第二阶段：运行 Tomcat =====
FROM tomcat:9-jdk8

# 删除默认应用
RUN rm -rf /usr/local/tomcat/webapps/*

# 复制 WAR
COPY --from=builder /build/target/bilibili.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080