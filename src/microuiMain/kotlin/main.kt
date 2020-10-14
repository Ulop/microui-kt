import `microui-kt`.*
import `microui-kt`.Context as Ctx

fun main() {
    val context = Ctx(
        {
            Column(
                mutableListOf(
                    Box(id),
                    Row(
                        mutableListOf(
                            Button(id),
                            Button(id),
                            Box(id)
                        ),
                        id
                    ),
                    Row(
                        mutableListOf(
                            Button(id),
                            Button(id),
                            Button(id)
                        ),
                        id
                    )
                ),
                id
            )
        },
        Position(10, 10),
        SDLVisualizer()
    )

    /* main loop */
    while (context.mainLoop()) {
    }
}