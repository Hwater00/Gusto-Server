package com.umc.gusto.domain.store.service;

import com.umc.gusto.domain.myCategory.entity.Pin;
import com.umc.gusto.domain.myCategory.repository.PinRepository;
import com.umc.gusto.domain.review.entity.Review;
import com.umc.gusto.domain.review.repository.ReviewRepository;
import com.umc.gusto.domain.store.entity.Category;
import com.umc.gusto.domain.store.entity.OpeningHours;
import com.umc.gusto.domain.store.entity.Store;
import com.umc.gusto.domain.store.model.response.*;
import com.umc.gusto.domain.store.repository.OpeningHoursRepository;
import com.umc.gusto.domain.store.repository.StoreRepository;
import com.umc.gusto.domain.user.entity.User;
import com.umc.gusto.global.exception.Code;
import com.umc.gusto.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService{
    private final StoreRepository storeRepository;
    private final ReviewRepository reviewRepository;
    private final PinRepository pinRepository;
    private final OpeningHoursRepository openingHoursRepository;
    private static final int PAGE_SIZE_FIRST = 3;
    private static final int PAGE_SIZE = 6;

    @Transactional(readOnly = true)
    public GetStoreResponse getStore(User user, Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new GeneralException(Code.STORE_NOT_FOUND));
        Long pinId = pinRepository.findByUserAndStoreStoreId(user, storeId);
        List<OpeningHours> openingHoursList = openingHoursRepository.findByStoreStoreId(storeId);

        Map<OpeningHours.BusinessDay, GetStoreResponse.Timing> businessDays = new LinkedHashMap<>();
        for (OpeningHours openingHours : openingHoursList) {
            GetStoreResponse.Timing timing = new GetStoreResponse.Timing(
                    openingHours.getOpenedAt(),
                    openingHours.getClosedAt()
            );
            businessDays.put(openingHours.getBusinessDay(), timing);
        }

        List<Review> top3Reviews = reviewRepository.findFirst3ByStoreOrderByLikedDesc(store);

        List<String> reviewImg = top3Reviews.stream()
                .map(review -> Optional.ofNullable(review.getImg1()).orElse(""))
                .collect(Collectors.toList());
        boolean isPinned = pinRepository.existsByUserAndStoreStoreId(user, storeId);


        return GetStoreResponse.builder()
                .pinId(pinId)
                .storeId(storeId)
                .storeName(store.getStoreName())
                .address(store.getAddress())
                .longitude(store.getLongitude())
                .latitude(store.getLatitude())
                .businessDay(businessDays)
                .reviewImg3(reviewImg)
                .pin(isPinned)
                .build();
    }


    @Transactional(readOnly = true)
    public GetStoreDetailResponse getStoreDetail(User user, Long storeId, LocalDate visitedAt, Long reviewId, Pageable pageable) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new GeneralException(Code.STORE_NOT_FOUND));
        // 가게별 기본 카테고리 값
//        Category category = storeRepository.findCategoryByStoreId(storeId)
//                .orElseThrow(() -> new GeneralException(Code.CATEGORY_NOT_FOUND));
        Long pinId = pinRepository.findByUserAndStoreStoreId(user, storeId);

        List<Review> top4Reviews = reviewRepository.findFirst4ByStoreOrderByLikedDesc(store);

        List<String> reviewImg = top4Reviews.stream()
                .map(review -> Optional.ofNullable(review.getImg1()).orElse(""))
                .collect(Collectors.toList());

        // reviews 페이징 처리 (3,6,6...)
        int pageSize;
        int pageNumber = pageable.getPageNumber();
        List<Review> reviews;

        if (reviewId != null && visitedAt != null) {
            pageSize = PAGE_SIZE;
            reviews = reviewRepository.findReviewsAfterIdByStore(store, visitedAt, reviewId, PageRequest.of(pageNumber, pageSize));
        } else {
            pageSize = PAGE_SIZE_FIRST;
            reviews = reviewRepository.findFirstReviewsByStore(store, PageRequest.of(pageNumber, pageSize));
        }

        List<GetReviewsResponse> getReviews = reviews.stream()
                .map(review -> {
                    User reviewer = review.getUser();
                    return GetReviewsResponse.builder()
                        .reviewId(review.getReviewId())
                        .visitedAt(review.getVisitedAt())
                        .profileImage(reviewer.getProfileImage())
                        .nickname(reviewer.getNickname())
                        .liked(review.getLiked())
                        .comment(review.getComment())
                        .img1(review.getImg1())
                        .img2(review.getImg2())
                        .img3(review.getImg3())
                        .img4(review.getImg4())
                        .build();
                })
                .toList();

        boolean isPinned = pinRepository.existsByUserAndStoreStoreId(user, storeId);

        return GetStoreDetailResponse.builder()
                .pinId(pinId)
                .storeId(storeId)
                .categoryString(store.getCategoryString())
                .storeName(store.getStoreName())
                .address(store.getAddress())
                .reviewImg4(reviewImg)
                .pin(isPinned)
                .reviews(getReviews)
                .build();
    }

    @Transactional(readOnly = true)
    public List<GetStoresInMapResponse> getStoresInMap(User user, String townName, Long myCategoryId) {
        List<Long> storeIds;
        List<Store> stores;

        if (myCategoryId == null) {
            storeIds = pinRepository.findStoreIdsByUser(user);
        } else {
            storeIds = pinRepository.findStoreIdsByUserAndMyCategoryId(user, myCategoryId);      // 내 카테고리 별 내가 찜한 가게의 id 리스트
        }

        stores = storeRepository.findByTownNameAndStoreIds(townName, storeIds);


        return stores.stream()
                .map(store -> GetStoresInMapResponse.builder()
                        .storeId(store.getStoreId())
                        .storeName(store.getStoreName())
                        .longitude(store.getLongitude())
                        .latitude(store.getLatitude())
                        .build())
                .collect(Collectors.toList());

    }

    @Transactional(readOnly = true)
    public List<GetPinStoreResponse> getPinStoresByCategoryAndLocation(User user, Long myCategoryId, String townName) {

        List<Pin> pins = pinRepository.findPinsByUserAndMyCategoryIdAndTownNameAndPinIdDESC(user, myCategoryId, townName);
        if(myCategoryId == null){
            pins = pinRepository.findPinsByUserAndTownNameAndPinIdDESC(user, townName);
        }
        List<GetStoreInfoResponse> visitedStoresInfo = new ArrayList<>();
        List<GetStoreInfoResponse> unvisitedStoresInfo = new ArrayList<>();

        for (Pin pin : pins){
            Store store = pin.getStore();
            Optional<Review> topReviewOptional = reviewRepository.findFirstByStoreOrderByLikedDesc(store);
            String reviewImg = topReviewOptional.map(Review::getImg1).orElse("");
            boolean hasVisited = reviewRepository.existsByStoreAndUserNickname(store, user.getNickname());

            GetStoreInfoResponse getStoreInfoResponse = GetStoreInfoResponse.builder()
                    .storeId(store.getStoreId())
                    .categoryString(store.getCategoryString())
                    .storeName(store.getStoreName())
                    .address(store.getAddress())
                    .reviewImg(reviewImg)
                    .build();

            if(!hasVisited){
                unvisitedStoresInfo.add(getStoreInfoResponse);
            }else {
                visitedStoresInfo.add(getStoreInfoResponse);
            }
        }

        // 방문하지 않은 가게 리스트
        UnvisitedStoresResponse unvisitedStoresResponse = UnvisitedStoresResponse.builder()
                .numPinStores(unvisitedStoresInfo.size())
                .unvisitedStores(unvisitedStoresInfo)
                .build();

        // 방문한 가게 리스트
        VisitedStoresResponse visitedStoresResponse = VisitedStoresResponse.builder()
                .numPinStores(visitedStoresInfo.size())
                .visitedStores(visitedStoresInfo)
                .build();

        GetPinStoreResponse pinStoreResponse = GetPinStoreResponse.builder()
                .nickname(user.getNickname())
                .numPinStores(pins.size())
                .unvisitedStores(Collections.singletonList(unvisitedStoresResponse))
                .visitedStores(Collections.singletonList(visitedStoresResponse))
                .build();

        return Collections.singletonList(pinStoreResponse);
    }

    @Override
    public List<GetStoreInfoResponse> searchStore(String keyword) {
        List<Store> searchResult = storeRepository.findTop5ByStoreNameContains(keyword);

        return searchResult.stream()
                .map(result -> {
                    Optional<Review> review = reviewRepository.findFirstByStoreOrderByLikedDesc(result);
                    String reviewImg = review.map(Review::getImg1).orElse("");
                    return GetStoreInfoResponse.builder()
                            .storeId(result.getStoreId())
                            .storeName(result.getStoreName())
                            .categoryString(result.getCategoryString())
                            .address(result.getAddress())
                            .reviewImg(reviewImg)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
