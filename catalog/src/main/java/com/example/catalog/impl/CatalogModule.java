package com.example.catalog.impl;

import com.example.catalog.api.CatalogService;
import com.google.inject.AbstractModule;
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport;

import com.example.cart.api.CartService;

/**
 * The module that binds the {@link CatalogService} so that it can be served.
 */
public class CatalogModule extends AbstractModule implements ServiceGuiceSupport {
    @Override
    protected void configure() {
        // Bind the InventoryService service
        bindService(CatalogService.class, CatalogServiceImpl.class);
        // Bind the ShoppingCartService client
        bindClient(CartService.class);
    }
}
