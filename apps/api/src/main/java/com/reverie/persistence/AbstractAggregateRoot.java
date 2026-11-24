package com.reverie.persistence;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;

/**
 * Base for aggregate roots that use application-generated UUID identifiers
 * (ADR-002).
 *
 * <p>Spring Data JDBC needs to know whether a {@code save} is an INSERT or an
 * UPDATE. With database-generated keys it uses "id is null" to decide, but our
 * identifiers are assigned in Java before the first save, so the id is never
 * null. Implementing {@link Persistable} lets each aggregate answer that
 * question explicitly through {@link #isNew()}.
 *
 * <p>The rule is simple: a freshly constructed aggregate is new, and an
 * aggregate materialized from the database is not. Every constructor leaves
 * {@code isNew} true, and {@link AggregateStateCallback} flips it to false right
 * after an aggregate is loaded or saved. The {@code isNew} field is
 * {@link Transient}, so it is never mapped to a column.
 *
 * <p>No {@code @Version} column is used in Milestone 1 (ADR-002).
 */
public abstract class AbstractAggregateRoot implements Persistable<UUID> {

    @Id
    private final UUID id;

    @Transient
    private boolean isNew = true;

    protected AbstractAggregateRoot(UUID id) {
        this.id = id;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    /** Marks this aggregate as already persisted. Called only by the state callback. */
    void markNotNew() {
        this.isNew = false;
    }
}
