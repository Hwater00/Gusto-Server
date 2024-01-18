package com.umc.gusto.domain.review.entity;

import com.umc.gusto.domain.review.enums.PublishReview;
import com.umc.gusto.domain.store.entity.Store;
import com.umc.gusto.domain.user.entity.User;
import com.umc.gusto.global.common.BaseEntity;
import com.umc.gusto.global.common.BaseTime;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Review extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @Column(nullable = false)
    private Integer taste;

    private Integer spiciness;

    private Integer mood;

    private Integer toilet;

    private Integer parking;

    private LocalDate visitedAt;

    private String img1;

    private String img2;

    private String img3;

    private String img4;

    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR DEFAULT 'PUBLIC'")
    private PublishReview publishReview;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer liked;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    private User user;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "storeId")
//    private Store store;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Tagging> taggingSet = new HashSet<>();          // 중복 허용x
}
