package com.example.cart.impl;

import akka.Done;
import akka.NotUsed;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import akka.japi.Pair;
import com.example.cart.api.*;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.api.transport.BadRequest;
import com.lightbend.lagom.javadsl.api.transport.NotFound;
import com.lightbend.lagom.javadsl.broker.TopicProducer;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;

import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the {@link CartService}.
 */
public class CartServiceImpl implements CartService {

    private final PersistentEntityRegistry persistentEntityRegistry;

    private final ReportRepository reportRepository;

    private final ClusterSharding clusterSharing;

    @Inject
    public CartServiceImpl(ClusterSharding clusterSharing,
                           PersistentEntityRegistry persistentEntityRegistry,
                           ReportRepository reportRepository) {
        this.clusterSharing = clusterSharing;
        this.persistentEntityRegistry = persistentEntityRegistry;
        this.reportRepository = reportRepository;

        // register entity on shard
        this.clusterSharing.init(
                Entity.of(
                        CartEntity.ENTITY_TYPE_KEY,
                        CartEntity::create
                )
        );
    }

    private EntityRef<CartEntity.Command> entityRef(String id) {
        return clusterSharing.entityRefFor(CartEntity.ENTITY_TYPE_KEY, id);
    }

    private final Duration askTimeout = Duration.ofSeconds(5);

    @Override
    public ServiceCall<NotUsed, CartView> get(String id) {
        return request ->
                entityRef(id)
                        .ask(CartEntity.Get::new, askTimeout)
                        .thenApply(summary -> asShoppingCartView(id, summary));
    }

    @Override
    public ServiceCall<NotUsed, CartReportView> getReport(String id) {
        return request -> reportRepository.findById(id).thenApply(report -> {
            if (report != null)
                return new CartReportView(id, report.getCreationDate(), report.getCheckoutDate());
            else
                throw new NotFound("Couldn't find a shopping cart report for '" + id + "'");
        });
    }

    @Override
    public ServiceCall<CartItem, Done> addItem(String cartId) {
        return item ->
                entityRef(cartId)
                .<CartEntity.Confirmation>ask(replyTo ->
                        new CartEntity.AddItem(item.getItemId(), item.getQuantity(), replyTo), askTimeout)
                .thenApply(this::handleConfirmation)
                .thenApply(accepted -> Done.getInstance());
    }

    @Override
    public ServiceCall<NotUsed, CartView> removeItem(String cartId, String itemId) {
        return request ->
            entityRef(cartId)
                .<CartEntity.Confirmation>ask(replyTo ->
                    new CartEntity.RemoveItem(itemId, replyTo), askTimeout)
                    .thenApply(this::handleConfirmation)
                    .thenApply(accepted -> asShoppingCartView(cartId, accepted.getSummary()));
    }

    @Override
    public ServiceCall<Quantity, CartView> adjustItemQuantity(String cartId, String itemId) {
        return quantity ->
            entityRef(cartId)
                .<CartEntity.Confirmation>ask(replyTo ->
                        new CartEntity.AdjustItemQuantity(itemId, quantity.getQuantity(), replyTo), askTimeout)
                .thenApply(this::handleConfirmation)
                .thenApply(accepted -> asShoppingCartView(cartId, accepted.getSummary()));
    }

    @Override
    public ServiceCall<NotUsed, Done> checkout(String cartId) {
        return request -> entityRef(cartId).ask(CartEntity.Checkout::new, askTimeout)
                .thenApply(this::handleConfirmation)
                .thenApply(accepted -> Done.getInstance());
    }

    @Override
    public Topic<CartView> shoppingCartTopic() {
        // We want to publish all the shards of the shopping cart events
        return TopicProducer.taggedStreamWithOffset(CartEntity.Event.TAG.allTags(), (tag, offset) ->
                // Load the event stream for the passed in shard tag
                persistentEntityRegistry.eventStream(tag, offset)
                        // We only want to publish checkout events
                        .filter(pair -> pair.first() instanceof CartEntity.CheckedOut)
                        // Now we want to convert from the persisted event to the published event.
                        // To do this, we need to load the current shopping cart state.
                        .mapAsync(4, eventAndOffset -> {
                            CartEntity.CheckedOut checkedOut = (CartEntity.CheckedOut) eventAndOffset.first();
                            return entityRef(checkedOut.getShoppingCartId()).ask(CartEntity.Get::new, askTimeout)
                                    .thenApply(summary -> Pair.create(asShoppingCartView(checkedOut.getShoppingCartId(), summary),
                                            eventAndOffset.second()));
                        }));
    }


    /**
     * Try to converts Confirmation to a Accepted
     *
     * @throws BadRequest if Confirmation is a Rejected
     */
    private CartEntity.Accepted handleConfirmation(CartEntity.Confirmation confirmation) {
        if (confirmation instanceof CartEntity.Accepted) {
            CartEntity.Accepted accepted = (CartEntity.Accepted) confirmation;
            return accepted;
        }

        CartEntity.Rejected rejected = (CartEntity.Rejected) confirmation;
        throw new BadRequest(rejected.getReason());
    }

    private CartView asShoppingCartView(String id, CartEntity.Summary summary) {
        List<CartItem> items = new ArrayList<>();
        for (Map.Entry<String, Integer> item : summary.getItems().entrySet()) {
            items.add(new CartItem(item.getKey(), item.getValue()));
        }
        return new CartView(id, items, summary.getCheckoutDate());
    }

}
