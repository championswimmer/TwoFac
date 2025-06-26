package tech.arnav.twofac.lib

class TwoFacLib private constructor() {

    companion object {

        fun initialise(): TwoFacLib {
            // Initialize any necessary components here
            return TwoFacLib()
        }
    }
}