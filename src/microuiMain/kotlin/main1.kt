import SDL.*
import `microui-kt`.*
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import microui.*
import microui.Color
import microui.Context
import microui.Rect
import kotlin.math.max
import `microui-kt`.Context as Ctx

val logBuf = CharArray(64_000, { '\n' })
var logbuf_updated = 0
val bg = intArrayOf(90, 95, 100)

fun writeLog(text: String) = println(text)
fun getTicks() = SDL_GetTicks()

fun testWindow(ctx: Context) {
    /* do window */
    if (beginWindow(ctx, "Demo Window", Rect(40, 40, 300, 450)) != Res.NONE) {
        val win = getCurrentContainer(ctx)
        win.rect.w = max(win.rect.w, 240)
        win.rect.h = max(win.rect.h, 300)

        /* window info */
        if (header(ctx, "Window Info") != Res.NONE) {
            val win = getCurrentContainer(ctx)
            var buf = ""
            layoutRow(ctx, intArrayOf(54, -1), 0)
            label(ctx, "Position:")
            buf = "%d, %d".format(win.rect.x, win.rect.y); label(ctx, buf)
            label(ctx, "Size:")
            buf = "%d, %d".format(win.rect.w, win.rect.h); label(ctx, buf)
        }

        /* labels + buttons */
        if (headerEx(ctx, "Test Buttons", Opt.EXPANDED.value) != Res.NONE) {
            layoutRow(ctx, intArrayOf(86, -110, -1), 0)
            label(ctx, "Test buttons 1:")
            if (button(ctx, "Button 1") != 0) {
                writeLog("Pressed button 1")
            }
            if (button(ctx, "Button 2") != 0) {
                writeLog("Pressed button 2")
            }
            label(ctx, "Test buttons 2:")
            if (button(ctx, "Button 3") != 0) {
                writeLog("Pressed button 3")
            }
            if (button(ctx, "Popup") != 0) {
                openPopup(ctx, "Test Popup")
            }
            if (beginPopup(ctx, "Test Popup") != Res.NONE) {
                button(ctx, "Hello")
                button(ctx, "World")
                endPopup(ctx)
            }
        }

        /* tree */
        if (headerEx(ctx, "Tree and Text", Opt.EXPANDED.value) != Res.NONE) {
            layoutRow(ctx, intArrayOf(140, -1), 0)
            layoutBeginColumn(ctx)
            if (beginTreeNode(ctx, "Test 1") != Res.NONE) {
                if (beginTreeNode(ctx, "Test 1a") != Res.NONE) {
                    label(ctx, "Hello")
                    label(ctx, "world")
                    endTreeNode(ctx)
                }
                if (beginTreeNode(ctx, "Test 1b") != Res.NONE) {
                    if (button(ctx, "Button 1") != 0) {
                        writeLog("Pressed button 1")
                    }
                    if (button(ctx, "Button 2") != 0) {
                        writeLog("Pressed button 2")
                    }
                    endTreeNode(ctx)
                }
                endTreeNode(ctx)
            }
            if (beginTreeNode(ctx, "Test 2") != Res.NONE) {
                layoutRow(ctx, intArrayOf(54, 54), 0)
                if (button(ctx, "Button 3") != 0) {
                    writeLog("Pressed button 3")
                }
                if (button(ctx, "Button 4") != 0) {
                    writeLog("Pressed button 4")
                }
                if (button(ctx, "Button 5") != 0) {
                    writeLog("Pressed button 5")
                }
                if (button(ctx, "Button 6") != 0) {
                    writeLog("Pressed button 6")
                }
                endTreeNode(ctx)
            }
            if (beginTreeNode(ctx, "Test 3") != Res.NONE) {
                val checks = intArrayOf(1, 0, 1)
                checkBox(ctx, "Checkbox 1", checks[0])
                checkBox(ctx, "Checkbox 2", checks[1])
                checkBox(ctx, "Checkbox 3", checks[2])
                endTreeNode(ctx)
            }
            layoutEndColumn(ctx)

            layoutBeginColumn(ctx)
            layoutRow(ctx, intArrayOf(-1), 0)
            text(
                ctx, "Lorem ipsum dolor sit amet, consectetur adipiscing " +
                        "elit. Maecenas lacinia, sem eu lacinia molestie, mi risus faucibus " +
                        "ipsum, eu varius magna felis a nulla."
            )
            layoutEndColumn(ctx)
        }

        /* background color sliders */
        if (headerEx(ctx, "Background Color", Opt.EXPANDED.value) != Res.NONE) {
            println("Background color slider")
            layoutRow(ctx, intArrayOf(-78, -1), 74)
            /* sliders */
            layoutBeginColumn(ctx)
            layoutRow(ctx, intArrayOf(46, -1), 0)
            label(ctx, "Red:"); slider(ctx, bg[0].toFloat(), 0f, 255f)
            label(ctx, "Green:"); slider(ctx, bg[1].toFloat(), 0f, 255f)
            label(ctx, "Blue:"); slider(ctx, bg[2].toFloat(), 0f, 255f)
            layoutEndColumn(ctx)
            /* color preview */
            val r = layoutNext(ctx)
            drawRect(ctx, r, Color(bg[0].toUByte(), bg[1].toUByte(), bg[2].toUByte(), 255U))
            val buf = "#%02X%02X%02X".format(bg[0], bg[1], bg[2])
            drawControlText(ctx, buf, r, Colors.TEXT, Opt.ALIGN_CENTER.value)
        }

        endWindow(ctx)
    }
}

fun logWindow(ctx: Context) {
    if (beginWindow(ctx, "Log Window", Rect(0, 40, 300, 200)) != Res.NONE) {
        /* output text panel */
        layoutRow(ctx, intArrayOf(-1), 25)
        /* input textbox + submit button */
        var buf = Buffer("")
        var submitted = 0

        button(ctx, "Hello")
        if (submitted != 0) {
            writeLog(buf.value)
            buf.value = ""
        }

        endWindow(ctx)
    }
}

fun uint8Slider(context: Context, value: Float, low: Int, high: Int): Int {
    var innerValue = value
    var tmp = 0f
    pushId(context, innerValue.toString())
    tmp = innerValue
    val res = sliderEx(context, tmp, low.toFloat(), high.toFloat(), 0F, "%.0f", Opt.ALIGN_CENTER.value)
    innerValue = tmp
    popId(context)
    return res
}

fun styleWindow(ctx: Context) {
    val colors = mapOf(
        "text:" to Colors.TEXT,
        "border:" to Colors.BORDER,
        "windowbg:" to Colors.WINDOW_BG,
        "titlebg:" to Colors.TITLE_BG,
        "titletext:" to Colors.TITLE_TEXT,
        "panelbg:" to Colors.PANEL_BG,
        "button:" to Colors.BUTTON,
        "buttonhover:" to Colors.BUTTON_HOVER,
        "buttonfocus:" to Colors.BUTTON_FOCUS,
        "base:" to Colors.BASE,
        "basehover:" to Colors.BASE_HOVER,
        "basefocus:" to Colors.BASE_FOCUS,
        "scrollbase:" to Colors.SCROLL_BASE,
        "scrollthumb:" to Colors.SCROLL_THUMB,
    )

    if (beginWindow(ctx, "Style Editor", Rect(350, 250, 300, 240)) != Res.NONE) {
        val sw = (getCurrentContainer(ctx).body.w * 0.14).toInt()
        layoutRow(ctx, intArrayOf(80, sw, sw, sw, sw, -1), 0)
        var i = 0
        colors.forEach { (label, idx) ->
            label(ctx, label)
            uint8Slider(ctx, ctx.style.colors[i].r.toFloat(), 0, 255)
            uint8Slider(ctx, ctx.style.colors[i].g.toFloat(), 0, 255)
            uint8Slider(ctx, ctx.style.colors[i].b.toFloat(), 0, 255)
            uint8Slider(ctx, ctx.style.colors[i].a.toFloat(), 0, 255)
            drawRect(ctx, layoutNext(ctx), ctx.style.colors[i])
            i++
        }
        endWindow(ctx)
    }
}

fun processFrame(context: Context) {
    begin(context)
    //styleWindow(context)
    //logWindow(context)
    //context.drawFrame(context, Rect(10, 10, 200, 80), Colors.PANEL_BG)
    if (beginWindow(context, "My Window", Rect(10, 10, 300, 400)) != Res.NONE) {
        layoutRow(context, intArrayOf(60), 0)
        if (button(context, "My Button") != 0) {
            println("'My Button' was pressed\n")
        }
        endWindow(context)
    }
    end(context)
}


val buttonMap = mapOf(
    (SDL_BUTTON_LEFT and 0xff) to Mouse.LEFT,
    (SDL_BUTTON_RIGHT and 0xff) to Mouse.RIGHT,
    (SDL_BUTTON_MIDDLE and 0xff) to Mouse.MIDDLE,
)

val keyMap = mapOf(
    (SDLK_LSHIFT and 0xffU) to Key.SHIFT,
    (SDLK_RSHIFT and 0xffU) to Key.SHIFT,
    (SDLK_LCTRL and 0xffU) to Key.CTRL,
    (SDLK_RCTRL and 0xffU) to Key.CTRL,
    (SDLK_LALT and 0xffU) to Key.ALT,
    (SDLK_RALT and 0xffU) to Key.ALT,
    (SDLK_RETURN and 0xffU) to Key.RETURN,
    (SDLK_BACKSPACE and 0xffU) to Key.BACKSPACE,
)


fun textWidth(font: Font?, text: String): Int {
    return getTextWidth(text)
}

fun textHeight(font: Font?) = getTextHeight()

fun main() = memScoped {
    /* init SDL and renderer */
    SDL_Init(SDL_INIT_EVERYTHING)
    initRendere()

    /* init microui */
    val ctx = initContext()
    ctx.textWidth = ::textWidth
    ctx.textHeight = ::textHeight

    val context = Ctx(
        {
            Column(
                mutableListOf(
                    Box(),
                    Row(
                        mutableListOf(
                            Box(),
                            Column(
                                mutableListOf(
                                    Button(),
                                    Box(),
                                    Button(),
                                    Box()
                                ),
                                333
                            ),
                            Box()
                        ),
                        222
                    ),
                    Box()
                ),
                111
            )
        },
        Position(10, 10),
        object : Visualizer {
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
                                    if (event.type == SDL_MOUSEBUTTONUP)
                                        commands.add(
                                            UserCommand.Mouse(
                                                MouseEvent.Up(
                                                    Position(
                                                        event.button.x,
                                                        event.button.y
                                                    )
                                                )
                                            )
                                        )
                                    else
                                        commands.add(
                                            UserCommand.Mouse(
                                                MouseEvent.Down(
                                                    button,
                                                    Position(event.button.x, event.button.y)
                                                )
                                            )
                                        )
                                }
                            }
                        }
                    }
                }
                return commands
            }

        }
    )

    /* main loop */
    while (context.mainLoop()) {
        /* handle SDL events */


        /* process frame */
        //
        //processFrame(ctx)

        /* render */
        //clear(Color(bg[0].convert(), bg[1].convert(), bg[2].convert(), 255U))
        /*var holder = CommandHolder()
        while (nextCommand(ctx, holder) != 0) {
            val cmd = holder.comand
            println("Next command $cmd")
            SDL_RenderClear(renderer)
            SDL_Delay(1000 / 60)
            when (cmd) {
                is Command.TextCommand -> {
                    drawText(cmd.str, cmd.pos, cmd.color)
                }
                is Command.RectCommand -> {
                    drawRect(cmd.rect, cmd.color)
                }
                is Command.IconCommand -> {
                    drawIcon(cmd.id, cmd.rect, cmd.color)
                }
                is Command.ClipCommand -> {
                    setClipRect(cmd.rect)
                }
            }
            SDL_RenderPresent(renderer)
        }*/
    }
}