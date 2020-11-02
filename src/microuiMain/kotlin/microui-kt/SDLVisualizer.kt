package `microui-kt`

import SDL.*
import cnames.structs.SDL_Renderer
import cnames.structs.SDL_Window
import kotlinx.cinterop.*

fun get_SDL_Error() = SDL_GetError()!!.toKString()

class SDLVisualizer : Visualizer {
    private val width = 800
    private val height = 600

    private val window: CPointer<SDL_Window>
    private val renderer: CPointer<SDL_Renderer>

    init {
        if (SDL_Init(SDL_INIT_EVERYTHING) != 0) {
            throw Error("SDL_Init Error: ${get_SDL_Error()}")
        }

        window = (SDL_CreateWindow(
            "Microu UI Kt",
            SDL_WINDOWPOS_UNDEFINED.convert(),
            SDL_WINDOWPOS_UNDEFINED.convert(),
            width,
            height,
            SDL_WINDOW_SHOWN or SDL_WINDOW_ALLOW_HIGHDPI
        ) ?: run {
            println("SDL_CreateWindow Error: ${get_SDL_Error()}")
            SDL_Quit()
            throw Error()
        })

        renderer = (SDL_CreateRenderer(
            window,
            -1,
            SDL_RENDERER_ACCELERATED or SDL_RENDERER_PRESENTVSYNC
        ) ?: run {
            SDL_DestroyWindow(window)
            println("SDL_CreateRenderer Error: ${get_SDL_Error()}")
            SDL_Quit()
            throw Error()
        })
    }

    override fun draw(bloc: Visualizer.() -> Unit) {
        SDL_RenderClear(renderer)
        SDL_Delay(1000 / 60)
        bloc(this)
        SDL_RenderPresent(renderer)
    }

    override fun drawRect(rect: `microui-kt`.Rect, color: `microui-kt`.Color) {
        val (r, g, b, a) = color
        SDL_SetRenderDrawColor(renderer, r, g, b, a)
        memScoped {
            val stretchedRect = alloc<SDL_Rect>()
            stretchedRect.w = (rect.w)
            stretchedRect.h = (rect.h)
            stretchedRect.x = (rect.x)
            stretchedRect.y = (rect.y)
            SDL_RenderFillRect(renderer, stretchedRect.ptr.reinterpret())
        }
        SDL_SetRenderDrawColor(renderer, 0, 0, 0, SDL_ALPHA_OPAQUE.toUByte())
    }

    override fun readCommands(): List<UserCommand> {
        val commands = mutableListOf<UserCommand>()
        memScoped {
            val event = alloc<SDL_Event>()
            while (SDL_PollEvent(event.ptr.reinterpret()) != 0) {
                when (event.type) {
                    SDL_QUIT -> {
                        commands.add(UserCommand.Exit)
                    }
                    SDL_MOUSEMOTION -> {
                        commands.add(
                            UserCommand.Mouse(
                                MouseEvent.Move(
                                    Position(
                                        event.motion.x,
                                        event.motion.y
                                    )
                                )
                            )
                        )
                        // println("${event.motion.x} ${event.motion.y}")
                    }
                    SDL_MOUSEBUTTONDOWN,
                    SDL_MOUSEBUTTONUP -> {
                        val button = when (event.button.button.toInt()) {
                            SDL_BUTTON_LEFT -> MouseButton.Left
                            SDL_BUTTON_MIDDLE -> MouseButton.Middle
                            SDL_BUTTON_RIGHT -> MouseButton.Right
                            else -> null
                        }

                        if (button != null) {
                            val constructor = if (event.type == SDL_MOUSEBUTTONUP) MouseEvent::Up else MouseEvent::Down
                            val mouseEvent = constructor(button, Position(event.button.x, event.button.y))

                            commands.add(UserCommand.Mouse(mouseEvent))
                        }
                    }
                }
            }
        }
        return commands
    }

}
