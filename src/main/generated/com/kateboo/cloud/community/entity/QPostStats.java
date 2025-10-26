package com.kateboo.cloud.community.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPostStats is a Querydsl query type for PostStats
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPostStats extends EntityPathBase<PostStats> {

    private static final long serialVersionUID = -1072466574L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPostStats postStats = new QPostStats("postStats");

    public final NumberPath<Integer> commentCount = createNumber("commentCount", Integer.class);

    public final NumberPath<Integer> likesCount = createNumber("likesCount", Integer.class);

    public final QPost post;

    public final ComparablePath<java.util.UUID> postId = createComparable("postId", java.util.UUID.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> viewsCount = createNumber("viewsCount", Long.class);

    public QPostStats(String variable) {
        this(PostStats.class, forVariable(variable), INITS);
    }

    public QPostStats(Path<? extends PostStats> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPostStats(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPostStats(PathMetadata metadata, PathInits inits) {
        this(PostStats.class, metadata, inits);
    }

    public QPostStats(Class<? extends PostStats> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.post = inits.isInitialized("post") ? new QPost(forProperty("post"), inits.get("post")) : null;
    }

}

