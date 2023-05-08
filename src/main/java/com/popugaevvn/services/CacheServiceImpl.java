package com.popugaevvn.services;

import com.popugaevvn.grpc.CacheServiceGrpc;
import com.popugaevvn.grpc.CacheServiceOuterClass;
import com.popugaevvn.repository.cache.Cache;
import com.popugaevvn.repository.cache.TwoQCache;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class CacheServiceImpl extends CacheServiceGrpc.CacheServiceImplBase {

    private final int SIZE_OF_CACHE = 10;

    private final Cache<String, Integer> cache = new TwoQCache<>(SIZE_OF_CACHE);

    @Override
    public void putInCache(
            CacheServiceOuterClass.PutInCacheRequest request,
            StreamObserver<CacheServiceOuterClass.PutInCacheResponse> responseObserver
    ) {
        Integer cacheValue = cache.put(request.getKey(), request.getValue());
        System.out.println("AFTER PUT");
        System.out.println(cache);

        CacheServiceOuterClass.PutInCacheResponse response = CacheServiceOuterClass.PutInCacheResponse
                .newBuilder()
                .setValue(cacheValue)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getFromCache(
            CacheServiceOuterClass.GetFromCacheRequest request,
            StreamObserver<CacheServiceOuterClass.GetFromCacheResponse> responseObserver
    ) {
        Integer responseValue = cache.get(request.getKey());
        System.out.println("AFTER GET");
        System.out.println(cache);

        if (responseValue == null) {
            responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
            return;
        }

        CacheServiceOuterClass.GetFromCacheResponse response = CacheServiceOuterClass.GetFromCacheResponse
                .newBuilder()
                .setValue(responseValue)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void removeFromCache(
            CacheServiceOuterClass.RemoveFromCacheRequest request,
            StreamObserver<CacheServiceOuterClass.RemoveFromCacheResponse> responseObserver
    ) {
        cache.remove(request.getKey());
        System.out.println("AFTER REMOVE");
        System.out.println(cache);

        CacheServiceOuterClass.RemoveFromCacheResponse response = CacheServiceOuterClass.RemoveFromCacheResponse
                .newBuilder().build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
