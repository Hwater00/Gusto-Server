package com.umc.gusto.domain.store.service;

import com.umc.gusto.domain.store.model.response.GetStoreDetailResponse;
import com.umc.gusto.domain.store.model.response.GetStoreResponse;
import com.umc.gusto.domain.store.model.response.GetStoresInMapResponse;
import com.umc.gusto.domain.user.entity.User;
import org.springframework.data.domain.Pageable;

import java.util.List;


public interface StoreService {

    GetStoreResponse getStore(User user, Long storeId);
    GetStoreDetailResponse getStoreDetail(User user, Long storeId, Long reviewId, Pageable pageable);
    List<GetStoresInMapResponse> getStoresInMap(User user, String townName, Long myCategoryId);
}