package tech.arnav.twofac.lib

import tech.arnav.twofac.lib.storage.MemoryStorage
import tech.arnav.twofac.lib.storage.Storage

class TwoFacLib private constructor(
    val storage: Storage
) {

    companion object {

        fun initialise(storage: Storage = MemoryStorage()): TwoFacLib {
            // Initialize any necessary components here
            return TwoFacLib(storage)
        }
    }
}