package com.example.cart.impl;

import com.google.common.collect.ImmutableMap;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import com.lightbend.lagom.javadsl.persistence.ReadSideProcessor;
import com.lightbend.lagom.javadsl.persistence.jpa.JpaReadSide;
import org.pcollections.PSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;

public class CartReportProcessor extends ReadSideProcessor<CartEntity.Event> {

    private final JpaReadSide jpaReadSide;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    public CartReportProcessor(JpaReadSide jpaReadSide) {
        this.jpaReadSide = jpaReadSide;
    }

    @Override
    public ReadSideHandler<CartEntity.Event> buildHandler() {
        return jpaReadSide.<CartEntity.Event>builder("shopping-cart-report").setGlobalPrepare(this::createSchema)
                .setEventHandler(CartEntity.ItemAdded.class, this::createReport)
                .setEventHandler(CartEntity.CheckedOut.class, this::addCheckoutTime).build();
    }

    private void createSchema(@SuppressWarnings("unused") EntityManager ignored) {
        Persistence.generateSchema("default", ImmutableMap.of("hibernate.hbm2ddl.auto", "update"));
    }

    private void createReport(EntityManager entityManager, CartEntity.ItemAdded evt) {

        logger.debug("Received ItemUpdate event: " + evt);
        if (findReport(entityManager, evt.shoppingCartId) == null) {
            logger.debug("Creating report for CartID: " + evt.shoppingCartId);
            CartReport report = new CartReport();
            report.setId(evt.shoppingCartId);
            report.setCreationDate(evt.eventTime);
            entityManager.persist(report);
        }
    }

    private void addCheckoutTime(EntityManager entityManager, CartEntity.CheckedOut evt) {
        CartReport report = findReport(entityManager, evt.shoppingCartId);

        logger.debug("Received CheckedOut event: " + evt);
        if (report != null) {
            logger.debug("Adding checkout time (" + evt.eventTime + ") for CartID: " + evt.shoppingCartId);
            report.setCheckoutDate(evt.eventTime);
            entityManager.persist(report);
        } else {
            throw new RuntimeException("Didn't find cart for checkout. CartID: " + evt.shoppingCartId);
        }
    }

    private CartReport findReport(EntityManager entityManager, String cartId) {
        return entityManager.find(CartReport.class, cartId);
    }

    @Override
    public PSequence<AggregateEventTag<CartEntity.Event>> aggregateTags() {
        return CartEntity.Event.TAG.allTags();
    }

}
