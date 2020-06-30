package com.example.catalog.api;

import akka.Done;
import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.transport.Method;

import static com.lightbend.lagom.javadsl.api.Service.named;
import static com.lightbend.lagom.javadsl.api.Service.restCall;

/**
 * The product catalog serivce interface.
 * <p>
 * This describes everything that Lagom needs to know about how to serve and
 * consume the inventory service.
 */
public interface CatalogService extends Service {
    /**
     * Get the inventory level for the given product id.
     */
    ServiceCall<NotUsed, Integer> get(String productId);

    /**
     * Add inventory to the given product id.
     */
    ServiceCall<Integer, Done> add(String productId);

    @Override
    default Descriptor descriptor() {
        return named("catalog")
                .withCalls(
                        restCall(Method.GET, "/catalog/:productId", this::get),
                        restCall(Method.POST, "/catalog/:productId", this::add)
                )
                .withAutoAcl(true);
    }
}
