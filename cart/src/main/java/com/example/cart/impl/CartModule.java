package com.example.cart.impl;

import com.example.cart.api.CartService;
import com.google.inject.AbstractModule;
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport;

/**
 * The module that binds the {@link CartService} so that it can be served.
 */
public class CartModule extends AbstractModule implements ServiceGuiceSupport {
    @Override
    protected void configure() {
        bindService(CartService.class, CartServiceImpl.class);
        bind(ReportRepository.class);
    }
}
