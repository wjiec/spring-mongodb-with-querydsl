package com.example.demo.domain;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@Document
public class User implements Serializable {
    private static final long serialVersionUID = -3146311959979265965L;

    /**
     * 主键Id
     */
    @Id
    private String id;

    /**
     * 用户名
     */
    @Indexed(unique = true)
    private String username;

    /**
     * 用户密码
     */
    private String password;

    /**
     * 创建日期
     */
    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    /**
     * 最后更新时间
     */
    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 更新乐观锁
     */
    @Version
    @Field("_version")
    private Long _version;

}
