/*
 * Copyright 2013 Stefan Domnanovits
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codebullets.sagalib.storage;

import com.codebullets.sagalib.SagaState;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Stores saga states in memory.
 */
public class MemoryStorage implements StateStorage {
    private final Object sync = new Object();

    private final Map<String, SagaState> storedStates = new HashMap<>();
    private final Multimap<SagaMultiKey, SagaState> instanceKeyMap = HashMultimap.create();

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final SagaState state) {
        checkNotNull(state, "State not allowed to be null.");
        checkNotNull(state.getSagaId(), "State saga id not allowed to be null.");
        checkNotNull(state.getType(), "Saga type must not be null.");

        synchronized (sync) {
            storedStates.put(state.getSagaId(), state);
            instanceKeyMap.put(SagaMultiKey.create(state), state);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SagaState load(final String sagaId) {
        checkNotNull(sagaId, "Saga id key must be set.");

        SagaState state;
        synchronized (sync) {
            state = storedStates.get(sagaId);
        }

        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final String sagaId) {
        checkNotNull(sagaId, "Saga id key must be set.");

        synchronized (sync) {
            SagaState removedItem = storedStates.remove(sagaId);
            if (removedItem != null) {
                instanceKeyMap.remove(SagaMultiKey.create(removedItem), removedItem);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends SagaState> load(final String type, final String instanceKey) {
        Collection<? extends SagaState> items;

        synchronized (sync) {
            items = Collections.unmodifiableCollection(instanceKeyMap.get(SagaMultiKey.create(type, instanceKey)));
        }

        return items;
    }

    private static final class SagaMultiKey {
        private final String type;
        private final String instanceKey;

        public SagaMultiKey(final String type, final String instanceKey) {
            checkNotNull(type, "type must not be null");

            this.type = type;
            this.instanceKey = instanceKey;
        }

        @Override
        public boolean equals(final Object o) {
            boolean isEqual = false;

            if (o != null && o instanceof SagaMultiKey) {
                SagaMultiKey other = (SagaMultiKey)o;
                isEqual = Objects.equals(type, other.type) &&
                          Objects.equals(instanceKey, other.instanceKey);
            }

            return isEqual;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, instanceKey);
        }

        public static SagaMultiKey create(SagaState sagaState) {
            return new SagaMultiKey(sagaState.getType(), sagaState.instanceKey());
        }

        public static SagaMultiKey create(final String type, final String instanceKey) {
            return new SagaMultiKey(type, instanceKey);
        }
    }
}