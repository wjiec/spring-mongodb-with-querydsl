在Gradle下配置SpringBoot+QueryDSL+MongoDB支持
-------------------------------------------

公司项目新开坑，是一个简单的创建/执行任务后台系统。考虑到其中多数数据没有复杂的关联关系，而且字段在可
预见的未来会经常变动（新增或者修改格式），所以就考虑使用MongoDB数据库。

而由于完美主义还有防止出现写错字段或者出于对于人脑的不信任（笑），打算使用[QueryDSL][1]来防止低级错
误的出现。（Querydsl是一个Java开源框架用于构建类型安全的SQL查询语句。它采用API代替拼凑字符串来构造查询语句。）

这篇文章呢，主要说明如何优雅的在spring中配置querydsl+mongodb的支持。

本文所有代码和方案均可在[spring-mongodb-with-querydsl][6]中找到（可执行项目）。


#### 使用Maven配置querydsl支持

虽然标题是在Gradle环境下配置querydsl，但是这边还是走走常见的Maven配置流程。至于为啥使用Gradle，而不
直接使用Maven，这又是另外一个故事了= =

由于官方就是使用Maven作为示例的（[Querying Mongodb][2]），所以很简单。主要是增加`querydsl-apt`的依赖后，
配置下`build.plugin`就好。以下是`pom.xml`文件示例
```xml
<project>
    <properties>
        <querydsl.version>4.4.0</querydsl.version>
    </properties>

    <dependencies>
        <!-- #1 -->
        <dependency>
          <groupId>com.querydsl</groupId>
          <artifactId>querydsl-apt</artifactId>
          <version>${querydsl.version}</version>
          <scope>provided</scope>
        </dependency>
        <dependency>
          <groupId>com.querydsl</groupId>
          <artifactId>querydsl-mongodb</artifactId>
          <version>${querydsl.version}</version>
        </dependency>
    </dependencies>
    <!-- ... -->
    <build>
        <plugins>
            <plugin>
                <groupId>com.mysema.maven</groupId>
                <artifactId>apt-maven-plugin</artifactId>
                <version>1.1.3</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>process</goal>
                        </goals>
                        <configuration>
                            <outputDirectory>target/generated-sources/annotations/java</outputDirectory>
                            <!-- #2 -->
                            <processor>org.springframework.data.mongodb.repository.support.MongoAnnotationProcessor</processor>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

虽然看起来很简单，但是其中还是有几点可以说的。

【1】处就是引入了`querydsl-apt`依赖，这个依赖是负责在编译阶段处理相应的注解并生成`Q-classes`到指定
目录中。`querydsl-apt`有`classifier`的可选项（具体可以查看[Customization][3]中关于可用分类的
描述（Available classifiers），也可以在[下载处][4]查看）。

带和不带`classifier`的区别只在是否有`META-INF/services/javax.annotation.processing.Processor`
文件存在和其中注解处理器的类名。具体可以尝试下载几个不同的包看看，这里就不详细展开了。

那这里为什么我们没带`classifier`限定呢？这就是【2】处配置的作用了，在spring-data-mongodb中
附带了一个注解处理器。__该处理器主要是用于扫描并解析`@Document`注解的。__ 该处理器位于
`org.springframework.data:spring-data-mongodb`包中，
类名为`org.springframework.data.mongodb.repository.support.MongoAnnotationProcessor`。

该处理器会配置querydsl-apt来处理某些类（被`@Document`注解的类）生成相对应的`Q-classes`文件。
```java
public class MongoAnnotationProcessor extends AbstractQuerydslProcessor {
	@Override
	protected Configuration createConfiguration(@Nullable RoundEnvironment roundEnv) {
		DefaultConfiguration configuration = new DefaultConfiguration(
            processingEnv, roundEnv, Collections.emptySet(), QueryEntities.class,
            Document.class, // 该参数指定注解
            QuerySupertype.class, QueryEmbeddable.class, QueryEmbedded.class, QueryTransient.class);
		
		configuration.setUnknownAsEmbedded(true);
		return configuration;
	}
}
```

如果不使用spring-data-mongodb附带的注解处理器，而使用带了`classifier`的querydsl-apt包的话，则
需要在模型上增加对应`classifier`所声明的注解。

有了这些基础知识之后，我们再看如何在Gradle中配置querydsl。


### 在Gradle中配置querydsl支持

其实看了以上内容，在Gradle中配置也就很简单了。这里提供3种方式进行配置
 * 引入`querydsl-apt`附带`classifier: general`然后在`@Document`基础上增加`@QueryEntity`注解
 来让`querydsl-apt`处理这些类生成相对应的`Q-classes`
 * 引入不带classifier的`querydsl-apt`后配置编译器显式声明使用spring-data-mongodb的注解处理器
 * 自己生成一个子模块，在子模块中继承或者拷贝spring-data-mongodb注解处理器内容，并
 在`META-INF/services/javax.annotation.processing.Processor`中声明这个处理器。

第一种方式的配置如下
```groovy
// build.gradle
dependencies {
    annotationProcessor group: 'com.querydsl', name: 'querydsl-apt', version: "${queryDslVersion}", classifier: 'general'
    annotationProcessor group: 'org.springframework.boot', name: 'spring-boot-starter-data-mongodb'
}
```
```java
// domain/User.java
import com.querydsl.core.annotations.QueryEntity;
import java.io.Serializable;

@QueryEntity
class User implements Serializable {
    // ...
}
```

第二种配置只需要修改`build.gradle`文件（需要引入`spring-data-mongodb`依赖）其实这就
是Maven方案的翻版。

__注意：这里会覆盖掉原本的注解处理器（比如lombok），使用这种方式需要手动配置其他的注解处理器。__

```groovy
// build.gradle
compileJava {
    doFirst {
        options.compilerArgs = [
            '-processor', 'org.springframework.data.mongodb.repository.support.MongoAnnotationProcessor'
        ]
    }
}
```

第三种配置需要新开个项目，然后新增`MongoAnnotationProcessor`类（其实这个类就是抄spring-data-mongodb中
附带的那个，只是去掉了日志输出【别问，问就是强迫症】）
```java
import com.querydsl.apt.AbstractQuerydslProcessor;
import com.querydsl.apt.Configuration;
import com.querydsl.apt.DefaultConfiguration;
import com.querydsl.core.annotations.*;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import java.util.Collections;

@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes({ "com.querydsl.core.annotations.*", "org.springframework.data.mongodb.core.mapping.*" })
public class MongoAnnotationProcessor extends AbstractQuerydslProcessor {
    @Override
    protected Configuration createConfiguration(RoundEnvironment roundEnv) {
        DefaultConfiguration configuration = new DefaultConfiguration(processingEnv, roundEnv, Collections.emptySet(),
            QueryEntities.class, Document.class, QuerySupertype.class, QueryEmbeddable.class, QueryEmbedded.class,
            QueryTransient.class);
        configuration.setUnknownAsEmbedded(true);

        return configuration;
    }
}
```

然后在`META-INF/services/javax.annotation.processing.Processor`中配置该类名
```plain
com.example.xxx.yyy.MongoAnnotationProcessor
```

### 结语

在这个项目中我选择的是第三种方式。因为公司有自己的`nexus3`私有仓库，写个包利人利己的事为啥不干（因为有技
术贡献【滑稽】），而且又满足了我的强迫症（没有添加奇怪的注解）。简直一石几十鸟啊！

至于为啥spring-data-mongodb官方不添加一个`javax.annotation.processing.Processor`文件来声明
注解处理器，其实官方论坛里也有讨论过（[传送门][5]）。简单来说就是程序员的浪漫（狗头）



 [1]: http://www.querydsl.com/
 [2]: http://www.querydsl.com/static/querydsl/latest/reference/html_single/#mongodb_integration
 [3]: http://www.querydsl.com/static/querydsl/latest/reference/html_single/#d0e2281
 [4]: https://repo1.maven.org/maven2/com/querydsl/querydsl-apt/4.4.0/
 [5]: https://github.com/spring-projects/spring-data-mongodb/issues/2740
 [6]: https://github.com/wjiec/spring-mongodb-with-querydsl
