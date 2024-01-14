/*
 *   Copyright 2024 Benoit LETONDOR
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.benoitletondor.easybudgetapp.helper

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A flow that emits only when it has a subscriber
 */
class MutableLiveFlow<T> : Flow<T>, FlowCollector<T> {
    private val wrapped = MutableSharedFlow<T>(extraBufferCapacity = 8) // Store up to 8 events while LifeCycle is not Started before blocking sender
    private val buffer = mutableListOf<T>()
    private val mutex = Mutex()

    override suspend fun collect(collector: FlowCollector<T>) {
        wrapped
            .onSubscription {
                mutex.withLock {
                    if (buffer.isNotEmpty()) {
                        emitAll(buffer.asFlow())
                        buffer.clear()
                    }
                }
            }
            .collect(collector)
    }

    override suspend fun emit(value: T) {
        mutex.withLock {
            if (wrapped.subscriptionCount.value <= 0) {
                buffer.add(value)
            } else {
                wrapped.emit(value)
            }
        }
    }
}