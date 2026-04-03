package tech.arnav.twofac.cli.runtime

enum class CliMode {
    INTERACTIVE,
    NON_INTERACTIVE,
}

fun interface CliModeResolver {
    fun resolve(inputInteractive: Boolean, outputInteractive: Boolean): CliMode
}

object DefaultCliModeResolver : CliModeResolver {
    override fun resolve(inputInteractive: Boolean, outputInteractive: Boolean): CliMode {
        return if (inputInteractive && outputInteractive) {
            CliMode.INTERACTIVE
        } else {
            CliMode.NON_INTERACTIVE
        }
    }
}
