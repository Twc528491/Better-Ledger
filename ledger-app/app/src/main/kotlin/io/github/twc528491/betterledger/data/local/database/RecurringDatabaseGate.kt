package io.github.twc528491.betterledger.data.local.database

import kotlinx.coroutines.sync.Mutex

/** Serializes recurring generation against whole-database restore operations in this process. */
object RecurringDatabaseGate {
    val mutex = Mutex()
}
