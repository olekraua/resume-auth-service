# Runtime Dependencies From Parent (E1-T1-S1 / S1-6)

Source files:
- `target/audit/dependency-mapping-runtime.tsv`
- `target/audit/effective-pom.xml`

| artifact | dependency_path (runtime tree) | effective-pom ref |
|---|---|---|
| `com.github.ben-manes.caffeine:caffeine` | `net.devstudy:resume-auth-service -> com.github.ben-manes.caffeine:caffeine` | `L398-L399` |
| `com.googlecode.libphonenumber:libphonenumber` | `net.devstudy:resume-auth-service -> com.googlecode.libphonenumber:libphonenumber` | `L8085-L8086` |
| `com.moparisthebest:junidecode` | `net.devstudy:resume-auth-service -> com.moparisthebest:junidecode` | `L8079-L8080` |
| `io.micrometer:micrometer-registry-prometheus` | `net.devstudy:resume-auth-service -> io.micrometer:micrometer-registry-prometheus` | `L5215-L5216` |
| `net.coobird:thumbnailator` | `net.devstudy:resume-auth-service -> net.coobird:thumbnailator` | `L8067-L8068` |
| `org.apache.commons:commons-lang3` | `net.devstudy:resume-auth-service -> org.apache.commons:commons-lang3` | `L444-L445` |
| `org.ehcache:ehcache` | `net.devstudy:resume-auth-service -> org.ehcache:ehcache` | `L509-L510` |
| `org.flywaydb:flyway-core` | `net.devstudy:resume-auth-service -> org.flywaydb:flyway-core` | `L568-L569` |
| `org.flywaydb:flyway-database-postgresql` | `net.devstudy:resume-auth-service -> org.flywaydb:flyway-database-postgresql` | `L608-L609` |
| `org.hibernate.orm:hibernate-jcache` | `net.devstudy:resume-auth-service -> org.hibernate.orm:hibernate-jcache` | `L753-L754` |
| `org.jsoup:jsoup` | `net.devstudy:resume-auth-service -> org.jsoup:jsoup` | `L8073-L8074` |
| `org.postgresql:postgresql` | `net.devstudy:resume-auth-service -> org.postgresql:postgresql` | `L1401-L1402` |
| `org.springframework.boot:spring-boot-configuration-processor` | `net.devstudy:resume-auth-service -> org.springframework.boot:spring-boot-configuration-processor` | `L1511-L1512` |
| `org.springframework.boot:spring-boot-devtools` | `net.devstudy:resume-auth-service -> org.springframework.boot:spring-boot-devtools` | `L1516-L1517` |
| `org.springframework.boot:spring-boot-starter-actuator` | `net.devstudy:resume-auth-service -> org.springframework.boot:spring-boot-starter-actuator` | `L1561-L1562` |
| `org.springframework.boot:spring-boot-starter-aop` | `net.devstudy:resume-auth-service -> org.springframework.boot:spring-boot-starter-aop` | `L1571-L1572` |
| `org.springframework.boot:spring-boot-starter-cache` | `net.devstudy:resume-auth-service -> org.springframework.boot:spring-boot-starter-cache` | `L1586-L1587` |
| `org.springframework.boot:spring-boot-starter-data-elasticsearch` | `net.devstudy:resume-auth-service -> org.springframework.boot:spring-boot-starter-data-elasticsearch` | `L1611-L1612` |
| `org.springframework.boot:spring-boot-starter-data-jpa` | `net.devstudy:resume-auth-service -> org.springframework.boot:spring-boot-starter-data-jpa` | `L1621-L1622` |
| `org.springframework.boot:spring-boot-starter-data-redis` | `net.devstudy:resume-auth-service -> org.springframework.boot:spring-boot-starter-data-redis` | `L1651-L1652` |
| `org.springframework.boot:spring-boot-starter-freemarker` | `net.devstudy:resume-auth-service -> org.springframework.boot:spring-boot-starter-freemarker` | `L1666-L1667` |
| `org.springframework.boot:spring-boot-starter-mail` | `net.devstudy:resume-auth-service -> org.springframework.boot:spring-boot-starter-mail` | `L1726-L1727` |
| `org.springframework.boot:spring-boot-starter-oauth2-resource-server` | `net.devstudy:resume-auth-service -> org.springframework.boot:spring-boot-starter-oauth2-resource-server` | `L1746-L1747` |
| `org.springframework.boot:spring-boot-starter-security` | `net.devstudy:resume-auth-service -> org.springframework.boot:spring-boot-starter-security` | `L1776-L1777` |
| `org.springframework.boot:spring-boot-starter-validation` | `net.devstudy:resume-auth-service -> org.springframework.boot:spring-boot-starter-validation` | `L1801-L1802` |
| `org.springframework.boot:spring-boot-starter-web` | `net.devstudy:resume-auth-service -> org.springframework.boot:spring-boot-starter-web` | `L1806-L1807` |
| `org.springframework.session:spring-session-data-redis` | `net.devstudy:resume-auth-service -> org.springframework.session:spring-session-data-redis` | `L7601-L7602` |
