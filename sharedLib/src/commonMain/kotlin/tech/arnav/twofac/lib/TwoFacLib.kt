package tech.arnav.twofac.lib

import tech.arnav.twofac.lib.storage.Storage

class TwoFacLib private constructor(
    val storage: Storage
) {

    companion object {

        fun initialise(storage: Storage): TwoFacLib {
            // Initialize any necessary components here
            return TwoFacLib(storage)
        }
    }
}