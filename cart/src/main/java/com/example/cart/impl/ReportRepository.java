package com.example.cart.impl;

import com.lightbend.lagom.javadsl.persistence.ReadSide;
import com.lightbend.lagom.javadsl.persistence.jpa.JpaSession;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletionStage;

@Singleton
public class ReportRepository {

    private final JpaSession jpaSession;

    @Inject
    public ReportRepository(ReadSide readSide, JpaSession jpaSession) {
        this.jpaSession = jpaSession;
        readSide.register(CartReportProcessor.class);
    }

    CompletionStage<CartReport> findById(String cartId) {
        return jpaSession.withTransaction(em -> em.find(CartReport.class, cartId));
    }

}
