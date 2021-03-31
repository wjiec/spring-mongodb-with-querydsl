package com.example.demo.querydsl.support;

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

/**
 * @see org.springframework.data.mongodb.repository.support.MongoAnnotationProcessor
 */
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
