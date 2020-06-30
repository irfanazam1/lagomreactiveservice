package com.example.catalog.impl;

import akka.Done;
import akka.NotUsed;
import akka.stream.javadsl.Flow;
import com.example.cart.api.CartView;
import com.lightbend.lagom.javadsl.api.ServiceCall;

import com.example.cart.api.CartService;
import com.example.catalog.api.CatalogService;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of the ProductCatalogService.
 */
@Singleton
public class CatalogServiceImpl implements CatalogService {
    private final ConcurrentMap<String, AtomicInteger> catalog = new ConcurrentHashMap<>();

    @Inject
    public CatalogServiceImpl(CartService shoppingCartService) {

        // Subscribe to the shopping cart topic
        shoppingCartService.shoppingCartTopic().subscribe()
            // Since this is at least once event handling, we really should track by shopping cart, and
            // not update inventory if we've already seen this shopping cart. But this is an in memory
            // inventory tracker anyway, so no need to be that careful.
            .atLeastOnce(
                // Create a flow that emits a Done for each message it processes
                Flow.<CartView>create().map(cart -> {
                    cart.getItems().forEach(item ->
                        getCatalog(item.getItemId()).addAndGet(-item.getQuantity())
                    );
                    return Done.getInstance();
                })
            );

    }

    private AtomicInteger getCatalog(String productId) {
        return catalog.computeIfAbsent(productId, k -> new AtomicInteger());
    }

    @Override
    public ServiceCall<NotUsed, Integer> get(String productId) {
        return notUsed -> CompletableFuture.completedFuture(
            getCatalog(productId).get()
        );
    }

    @Override
    public ServiceCall<Integer, Done> add(String productId) {
        return quantity -> {
            getCatalog(productId).addAndGet(quantity);
            return CompletableFuture.completedFuture(Done.getInstance());
        };
    }
}
