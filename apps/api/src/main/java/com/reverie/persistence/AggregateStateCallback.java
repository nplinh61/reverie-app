package com.reverie.persistence;

import org.springframework.data.relational.core.mapping.event.AfterConvertCallback;
import org.springframework.data.relational.core.mapping.event.AfterSaveCallback;
import org.springframework.stereotype.Component;

/**
 * Keeps the {@code isNew} state of {@link AbstractAggregateRoot} correct.
 *
 * <p>After an aggregate is converted from a database row (a load) or saved, it
 * exists in the database, so it must report {@link AbstractAggregateRoot#isNew()}
 * as false. Without this, a loaded-then-saved aggregate would be treated as new
 * and Spring Data JDBC would attempt a second INSERT and hit the primary-key
 * constraint.
 *
 * <p>Spring Data invokes a callback for every aggregate whose type is assignable
 * to the callback's generic type, so declaring it on the base class covers all
 * aggregate roots.
 */
@Component
class AggregateStateCallback
        implements AfterConvertCallback<AbstractAggregateRoot>, AfterSaveCallback<AbstractAggregateRoot> {

    @Override
    public AbstractAggregateRoot onAfterConvert(AbstractAggregateRoot aggregate) {
        aggregate.markNotNew();
        return aggregate;
    }

    @Override
    public AbstractAggregateRoot onAfterSave(AbstractAggregateRoot aggregate) {
        aggregate.markNotNew();
        return aggregate;
    }
}
