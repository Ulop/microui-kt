import `microui-kt`.*
import `microui-kt`.Context as Ctx

fun main() {
    val context = Ctx(
        {
            Column(
                mutableListOf(
                    Row(
                        mutableListOf(
                            Button(),
                            Button()
                        ),
                        222
                    ),
                    Row(
                        mutableListOf(
                            Button(),
                            Button(),
                            Button()
                        ),
                        222
                    )
                ),
                111
            )
        },
        Position(10, 10),
        SDLVisualizer()
    )

    /* main loop */
    while (context.mainLoop()) {
    }
}