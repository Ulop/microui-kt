package microui

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.sprintf
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

val UNCLIPPED_RECT = Rect(0, 0, 0x1000000, 0x1000000)

val defaultStyle = Style(
        /* font | size | padding | spacing | indent */
        null, Vec2(68, 10), 5, 4, 24,
        /* title_height | scrollbar_size | thumb_size */
        24, 12, 8,
        arrayOf(
                Color(230U, 230U, 230U, 255U), /* MU_COLOR_TEXT */
                Color(25U, 25U, 25U, 255U), /* MU_COLOR_BORDER */
                Color(50U, 50U, 50U, 255U), /* MU_COLOR_WINDOWBG */
                Color(25U, 25U, 25U, 255U), /* MU_COLOR_TITLEBG */
                Color(240U, 240U, 240U, 255U), /* MU_COLOR_TITLETEXT */
                Color(0U, 0U, 0U, 0U), /* MU_COLOR_PANELBG */
                Color(75U, 75U, 75U, 255U), /* MU_COLOR_BUTTON */
                Color(95U, 95U, 95U, 255U), /* MU_COLOR_BUTTONHOVER */
                Color(115U, 115U, 115U, 255U), /* MU_COLOR_BUTTONFOCUS */
                Color(30U, 30U, 30U, 255U), /* MU_COLOR_BASE */
                Color(35U, 35U, 35U, 255U), /* MU_COLOR_BASEHOVER */
                Color(40U, 40U, 40U, 255U), /* MU_COLOR_BASEFOCUS */
                Color(43U, 43U, 43U, 255U), /* MU_COLOR_SCROLLBASE */
                Color(30U, 30U, 30U, 255U)  /* MU_COLOR_SCROLLTHUMB */
        )
)

fun expandRect(rect: Rect, n: Int): Rect {
    return Rect(rect.x - n, rect.y - n, rect.w + n * 2, rect.h + n * 2)
}

fun intersectRects(r1: Rect, r2: Rect): Rect {
    val x1: Int = max(r1.x, r2.x)
    val y1: Int = max(r1.y, r2.y)
    var x2: Int = min(r1.x + r1.w, r2.x + r2.w)
    var y2: Int = min(r1.y + r1.h, r2.y + r2.h)
    if (x2 < x1) {
        x2 = x1
    }
    if (y2 < y1) {
        y2 = y1
    }
    return Rect(x1, y1, x2 - x1, y2 - y1)
}

fun rectOverlapsVec2(r: Rect, p: Vec2): Boolean {
    return p.x >= r.x && p.x < r.x + r.w && p.y >= r.y && p.y < r.y + r.h
}

fun compareZIndex(a: Container, b: Container): Int {
    return a.zIndex - b.zIndex
}

fun drawFrame(context: Context, rect: Rect, color: Colors) {
    drawRect(context, rect, context.style.colors[color.ordinal])
    if (color == Colors.SCROLL_BASE ||
            color == Colors.SCROLL_THUMB ||
            color == Colors.TITLE_BG) {
        return; }
    /* draw border */
    if (context.style.colors[Colors.BORDER.ordinal].a.toInt() != 0) {
        drawBox(context, expandRect(rect, 1), context.style.colors[Colors.BORDER.ordinal])
    }
}

fun initContext(): Context {
    return Context(
            drawFrame = ::drawFrame,
            style = defaultStyle
    )
}

fun begin(context: Context) {
    requireNotNull(context.textWidth)
    requireNotNull(context.textHeight)
    context.scrollTarget = null
    context.hoverRoot = context.nextHoverRoot
    context.nextHoverRoot = null
    context.mouseDelta.x = context.mousePos.x - context.lastMousePos.x
    context.mouseDelta.y = context.mousePos.y - context.lastMousePos.y
    context.frame++
}

fun end(context: Context) {
    var i = 0
    var n = 0
    /* check stacks */
    require(context.containerStack.isEmpty())
    require(context.clipStack.isEmpty())
    require(context.idStack.isEmpty())
    require(context.layoutStack.isEmpty())

    /* handle scroll input */
    val scrollTarget = context.scrollTarget
    if (scrollTarget != null) {
        scrollTarget.scroll.x += context.scrollDelta.x
        scrollTarget.scroll.y += context.scrollDelta.y
    }

    /* unset focus if focus id was not touched this frame */
    if (context.updatedFocus != 0) {
        context.focus = 0U
    }
    context.updatedFocus = 0

    /* bring hover root to front if mouse was pressed */
    val nextHoverRoot = context.nextHoverRoot
    if (
            context.mousePressed != Mouse.NONE &&
            nextHoverRoot != null &&
            nextHoverRoot.zIndex < context.lastZIndex &&
            nextHoverRoot.zIndex >= 0
    ) {
        bringToFront(context, nextHoverRoot)
    }

    /* reset input state */
    context.keyPressed = Key.NONE
    context.inputText = ""
    context.mousePressed = Mouse.NONE
    context.scrollDelta = Vec2(0, 0)
    context.lastMousePos = context.mousePos

    /* sort root containers by zindex */
    n = context.rootList.lastIndex
    context.rootList.sortBy { it.zIndex }

    /* set root container jump commands */
/*    for (i in 0..n) {
        val container = context.rootList[i]
        *//* if this is the first container then make the first command jump to it.
        ** otherwise set the previous container's tail to jump to this one *//*
        if (i == 0) {
           val cmd = context.commandList
            cmd.first().
            cmd->jump.dst = (char*) cnt->head+sizeof(mu_JumpCommand)
        } else {
            mu_Container * prev = ctx->root_list.items[i-1]
            prev->tail->jump.dst = (char*) cnt->head+sizeof(mu_JumpCommand)
        }
        *//* make the last container's tail jump to the end of command list *//*
        if (i == n - 1) { cnt ->
            tail->jump.dst = ctx->command_list.items+ctx->command_list.idx
        }
    }*/
}

fun bringToFront(context: Context, container: Container) {
    container.zIndex = ++context.lastZIndex
}

fun setFocus(context: Context, id: Id) {
    context.focus = id
    context.updatedFocus = 1
}

const val HASH_INITIAL = 2166136261U

fun hash(hash: Id, data: String, size: Int): Id {
    var result = hash.hashCode().toDouble()
    data.forEach { result = (result.pow(it.hashCode().toDouble()) * 16777619) }
    return result.toUInt()
}

fun getId(context: Context, data: String): Id {
    var res = context.idStack.lastOrNull() ?: HASH_INITIAL
    res = hash(res, data, data.length)
    context.lastId = res
    return res
}

fun <T> pushId(context: Context, data: String) {
    context.idStack.addLast(getId(context, data))
}

fun popId(context: Context) {
    context.idStack.removeLast()
}

fun getClipRect(context: Context): Rect {
    return context.clipStack.last()
}

fun pushClipRect(context: Context, rect: Rect) {
    val last: Rect = getClipRect(context)
    context.clipStack.addLast(intersectRects(rect, last))
}

fun popClipRect(context: Context) {
    context.clipStack.removeLast()
}

fun checkClip(context: Context, rect: Rect): Clip {
    val cr = getClipRect(context)
    if (rect.x > cr.x + cr.w || rect.x + rect.w < cr.x ||
            rect.y > cr.y + cr.h || rect.y + rect.h < cr.y) {
        return Clip.ALL; }
    if (rect.x >= cr.x && rect.x + rect.w <= cr.x + cr.w &&
            rect.y >= cr.y && rect.y + rect.h <= cr.y + cr.h) {
        return Clip.NONE; }
    return Clip.PART
}

fun pushLayout(context: Context, body: Rect, scroll: Vec2) {
    context.layoutStack.add(Layout(
            body = Rect(body.x - scroll.x, body.y - scroll.y, body.w, body.h),
            max = Vec2(-0x1000000, -0x1000000)
    ))
}

fun getLayout(context: Context) = context.layoutStack.last()

fun getCurrentContainer(context: Context): Container {
    return context.containerStack.last()
}

fun popContainer(context: Context) {
    val container = getCurrentContainer(context)
    val layout = getLayout(context)
    container.contentSize.x = layout.max?.x ?: 0 - layout.body.x
    container.contentSize.y = layout.max?.y ?: 0 - layout.body.y

    context.containerStack.removeLast()
    context.layoutStack.removeLast()
    popId(context)
}

fun getContainer(context: Context, id: Id, opt: Opt): Container? {
    context.containerPool ?: return null

    var container: Container? = null
    /* try to get existing container from pool */
    var idx = poolGet(context, context.containerPool, id)
    if (idx >= 0) {
        if (context.containers?.get(idx)?.open != 0 || opt == Opt.CLOSED) {
            poolUpdate(context, context.containerPool, idx)
        }
        return context.containers?.get(idx)
    }
    if (opt == Opt.CLOSED) {
        return null
    }

    /* container not found in pool: init new container */
    idx = initPool(context, context.containerPool, CONTAINER_POOL_SIZE, id)
    container = context.containers?.get(idx) ?: return null
    container.open = 1
    bringToFront(context, container)

    return container
}

fun getContainer(context: Context, name: String) =
        getContainer(context, getId(context, name), Opt.NONE)

/*============================================================================
** pool
**============================================================================*/

fun initPool(context: Context, items: Array<PoolItem>, length: Int, id: Id): Int {
    var n = -1
    var f = context.frame
    for (i in 0..length) {
        if (items[i].lastUpdate < f) {
            f = items[i].lastUpdate
            n = i
        }
    }
    require(n > -1)
    items[n].id = id
    poolUpdate(context, items, n)
    return n
}

fun poolGet(context: Context, items: Array<PoolItem>, id: Id) =
        items.indexOfFirst { it.id == id }

fun poolUpdate(context: Context, items: Array<PoolItem>, id: Int) {
    items[id].lastUpdate = context.frame
}

/*============================================================================
** input handlers
**============================================================================*/

fun inputMouseMove(context: Context, x: Int, y: Int) {
    context.mousePos = Vec2(x, y)
}


fun inputMouseDown(context: Context, x: Int, y: Int, button: Mouse) {
    inputMouseMove(context, x, y)
    context.mouseDown = Mouse.values()[context.mouseDown.value or button.value]
    context.mousePressed = Mouse.values()[context.mousePressed.value or button.value]
}


fun inputMouseUp(context: Context, x: Int, y: Int, button: Mouse) {
    inputMouseMove(context, x, y)
    context.mouseDown = Mouse.values()[context.mouseDown.value and button.value.inv()]
}


fun inputScroll(context: Context, x: Int, y: Int) {
    context.scrollDelta.x += x
    context.scrollDelta.y += y
}

fun inputKeyDown(context: Context, key: Key) {
    context.keyPressed = Key.values()[context.keyPressed.value or key.value]
    context.keyDown = Key.values()[context.keyDown.value or key.value]
}

fun inputKeyUp(context: Context, key: Key) {
    context.keyDown = Key.values()[context.keyDown.value and key.value.inv()]
}

fun inputText(context: Context, text: String) {
    context.inputText = text
}

/*============================================================================
** commandlist
**============================================================================*/

fun pushCommand(context: Context, cmd: Command): Command {
    require(context.commandList.size < COMMAND_LIST_SIZE)
    context.commandList.addLast(cmd)
    return cmd
}


fun nextCommand(context: Context, cmd: Command): Int {
    val commands = context.commandList
    for (command in commands) {
        if (command is Command.JumpCommand) return 1
    }
    return 0
}


fun pushJump(context: Context, dst: Command): Command {
    val cmd = pushCommand(context, Command.JumpCommand(dst))
    return cmd
}


fun setClip(context: Context, rect: Rect) {
    pushCommand(context, Command.ClipCommand(rect))
}


fun drawRect(context: Context, rect: Rect, color: Color) {
    val intersectRect = intersectRects(rect, getClipRect(context))
    if (intersectRect.w > 0 && intersectRect.h > 0) {
        pushCommand(context, Command.RectCommand(intersectRect, color))
    }
}

fun drawBox(context: Context, rect: Rect, color: Color) {
    drawRect(context, Rect(rect.x + 1, rect.y, rect.w - 2, 1), color)
    drawRect(context, Rect(rect.x + 1, rect.y + rect.h - 1, rect.w - 2, 1), color)
    drawRect(context, Rect(rect.x, rect.y, 1, rect.h), color)
    drawRect(context, Rect(rect.x + rect.w - 1, rect.y, 1, rect.h), color)
}

fun drawText(context: Context, font: Font, text: String, pos: Vec2, color: Color) {
    val rect = Rect(
            pos.x,
            pos.y,
            context.textWidth(font, text),
            context.textHeight(font)
    )

    val clipped = checkClip(context, rect)
    if (clipped == Clip.ALL) {
        return
    }
    if (clipped == Clip.PART) {
        setClip(context, getClipRect(context))
    }
    /* add command */
    pushCommand(context, Command.TextCommand(font, pos, color, '0'))

    /* reset clipping if it was set */
    if (clipped != Clip.NONE) {
        setClip(context, UNCLIPPED_RECT)
    }
}


fun drawIcon(context: Context, id: Icon, rect: Rect, color: Color) {
    /* do clip command if the rect isn't fully contained within the cliprect */
    val clipped = checkClip(context, rect)
    if (clipped == Clip.ALL) {
        return
    }
    if (clipped == Clip.PART) {
        setClip(context, getClipRect(context))
    }
    /* do icon command */
    pushCommand(context, Command.IconCommand(rect, id, color))

    if (clipped != Clip.NONE) {
        setClip(context, UNCLIPPED_RECT)
    }
}

/*============================================================================
** layout
**============================================================================*/

fun layoutBeginColumn(context: Context) {
    pushLayout(context, layoutNext(context), Vec2(0, 0))
}


fun layoutEndColumn(context: Context) {
    val b = getLayout(context)
    context.layoutStack.removeLast()
    /* inherit position/next_row/max from child layout if they are greater */
    val a = getLayout(context)
    requireNotNull(a.position)
    requireNotNull(b.position)

    a.position?.x = max(a.position?.x ?: 0, b.position?.x ?: 0 + b.body.x - a.body.x)
    a.nextRow = max(a.nextRow ?: 0, b.nextRow ?: 0 + b.body.y - a.body.y)
    a.max?.x = max(a.max?.x ?: 0, b.max?.x ?: 0)
    a.max?.y = max(a.max?.y ?: 0, b.max?.y ?: 0)
}


fun layoutRow(context: Context, widths: IntArray, height: Int) {
    val layout = getLayout(context)
    if (widths.isNotEmpty()) {
        require(widths.size <= MAX_WIDTHS)
        layout.widths = widths
    }
    layout.position = Vec2(layout.indent ?: 0, layout.nextRow ?: 0)
    layout.size?.y = height
    layout.itemIndex = 0
}

fun layoutWidth(context: Context, width: Int) {
    getLayout(context).size?.x = width
}

fun layoutHeight(context: Context, height: Int) {
    getLayout(context).size?.x = height
}

fun layoutSetNext(context: Context, rect: Rect, isRelative: Boolean) {
    val layout = getLayout(context)
    layout.next = rect
    layout.nextType = if (isRelative) LayoutType.RELATIVE else LayoutType.ABSOLUTE
}

fun layoutNext(context: Context): Rect {
    val layout = getLayout(context)
    val style = context.style
    val res: Rect?

    val position = layout.position
    if (layout.nextType != null) {
        /* handle rect set by `mu_layout_set_next` */
        val type = layout.nextType
        layout.nextType = LayoutType.NONE
        res = layout.next!!
        if (type == LayoutType.ABSOLUTE) {
            context.lastRect = res
            return res
        }
    } else {
        /* handle next row */
        if (layout.itemIndex == layout.widths.size) {
            layoutRow(context, intArrayOf(), layout.size?.y ?: 0)
        }

        res = Rect(
                position?.x ?: 0,
                position?.y ?: 0,
                if (layout.widths.isNotEmpty()) layout.widths.last() else layout.size?.x ?: 0,
                layout.size?.y ?: 0
        )
        if (res.w == 0) {
            res.w = style.size.x + style.padding * 2
        }
        if (res.h == 0) {
            res.h = style.size.y + style.padding * 2
        }
        if (res.w < 0) {
            res.w += layout.body.w - res.x + 1; }
        if (res.h < 0) {
            res.h += layout.body.h - res.y + 1; }

        layout.itemIndex?.inc()
    }

    /* update position */
    if (position != null)
        position.x += res.w + style.spacing
    layout.nextRow = max(layout.nextRow ?: 0, res.y + res.h + style.spacing)

    /* apply body offset */
    res.x += layout.body.x
    res.y += layout.body.y

    /* update max position */
    layout.max?.x = max(layout.max?.x ?: 0, res.x + res.w)
    layout.max?.y = max(layout.max?.y ?: 0, res.y + res.h)

    context.lastRect = res
    return res
}

/*============================================================================
** controls
**============================================================================*/

fun inHoverRoot(context: Context): Boolean {
    var i = context.containerStack.size
    while (i-- != 0) {
        if (context.containerStack[i] == context.hoverRoot) {
            return true
        }
        /* only root containers have their `head` field set; stop searching if we've
        ** reached the current root container */
        if (context.containerStack[i].head != null) {
            break
        }
    }
    return false
}

fun drawControlFrame(context: Context, id: Id, rect: Rect, color: Colors, opt: Opt) {
    var colorId = color.ordinal
    if (opt == Opt.NO_FRAME) {
        return; }
    colorId += if (context.focus == id) 2 else if (context.hover == id) 1 else 0
    context.drawFrame(context, rect, Colors.values()[colorId])
}


fun drawControlText(context: Context, str: String, rect: Rect, color: Colors, opt: Opt) {
    val font = context.style.font
    val tw = context.textWidth(font!!, str)
    pushClipRect(context, rect)
    val posY = rect.y + (rect.h - context.textHeight(font)) / 2
    val posX = when (opt) {
        Opt.ALIGN_CENTER -> rect.x + (rect.w - tw) / 2
        Opt.ALIGN_RIGHT -> rect.x + rect.w - tw - context.style.padding
        else -> rect.x + context.style.padding
    }
    drawText(context, font, str, Vec2(posX, posY), context.style.colors[color.ordinal])
    popClipRect(context)
}

fun mouseOver(context: Context, rect: Rect): Boolean {
    return (rectOverlapsVec2(rect, context.mousePos)
            && rectOverlapsVec2(getClipRect(context), context.mousePos)
            && inHoverRoot(context))
}

fun updateControl(context: Context, id: Id, rect: Rect, opt: Opt) {
    val mouseOver = mouseOver(context, rect)

    if (context.focus == id) {
        context.updatedFocus = 1
    }
    if (opt == Opt.NO_INTERACT) {
        return
    }
    if (mouseOver && context.mouseDown == Mouse.NONE) {
        context.hover = id
    }

    if (context.focus == id) {
        if (context.mousePressed != Mouse.NONE && !mouseOver) {
            setFocus(context, 0U)
        }
        if (context.mouseDown == Mouse.NONE && opt.value.inv() == Opt.HOLD_FOCUS.value) {
            setFocus(context, 0U)
        }
    }

    if (context.hover == id) {
        if (context.mousePressed != Mouse.NONE) {
            setFocus(context, id)
        } else if (!mouseOver) {
            context.hover = 0U
        }
    }
}


fun text(context: Context, text: String) {
    var p = text
    val width = -1
    val font = context.style.font!!
    val color = context.style.colors[Colors.TEXT.ordinal]
    layoutBeginColumn(context)
    layoutRow(context, intArrayOf(width), context.textHeight(font))
    do {
        val r = layoutNext(context)
        var w = 0
        val start = p
        var end = p
        do {
            val word = p
            while (p.firstOrNull() != null && p.first() != ' ' && p.first() != '\n') {
                p = p.drop(1)
            }
            w += context.textWidth(font, word)
            if (w > r.w && end != start) {
                break
            }
            w += context.textWidth(font, p)
            end = p.drop(1)
        } while (end != "\n")
        drawText(context, font, start, Vec2(r.x, r.y), color)
        p = end + 1
    } while (end.isNotEmpty())
    layoutEndColumn(context)
}


fun label(context: Context, text: String) {
    drawControlText(context, text, layoutNext(context), Colors.TEXT, Opt.NONE)
}


fun buttonEx(context: Context, label: String, icon: Icon, opt: Opt): Int {
    var res = 0
    val id = if (label.isNotEmpty())
        getId(context, label)
    else
        getId(context, icon.name)

    val r = layoutNext(context)
    updateControl(context, id, r, opt)
    /* handle click */
    if (context.mousePressed == Mouse.LEFT && context.focus == id) {
        res = res or Res.SUBMIT.value
    }
    /* draw */
    drawControlFrame(context, id, r, Colors.BUTTON, opt)
    if (label.isNotEmpty()) {
        drawControlText(context, label, r, Colors.TEXT, opt); }
    if (icon != Icon.NONE) {
        drawIcon(context, icon, r, context.style.colors[Colors.TEXT.ordinal])
    }
    return res
}


fun checkBox(context: Context, label: String, state: String): Int {
    var res = 0
    val id = getId(context, state)
    var r = layoutNext(context)
    val box = Rect(r.x, r.y, r.h, r.h)
    updateControl(context, id, r, Opt.NONE)
    /* handle click */
    if (context.mousePressed == Mouse.LEFT && context.focus == id) {
        res = res or Res.CHANGE.value
        // *state = !* state
    }
    /* draw */
    drawControlFrame(context, id, box, Colors.BASE, Opt.NONE)
    if (state.isNotEmpty()) {
        drawIcon(context, Icon.CHECK, box, context.style.colors[Colors.TEXT.ordinal])
    }
    r = Rect(r.x + box.w, r.y, r.w - box.w, r.h)
    drawControlText(context, label, r, Colors.TEXT, Opt.NONE)
    return res
}


fun textBoxRaw(context: Context, buffer: String, bufferSize: Int, id: Id, rect: Rect, opt: Opt): Int {
    var buff = buffer
    var res = 0
    updateControl(context, id, rect, Opt.values()[opt.value or Opt.HOLD_FOCUS.value])

    if (context.focus == id) {
        /* handle text input */
        var len = buffer.length
        var n = min(bufferSize - len - 1, context.inputText.length)
        if (n > 0) {
            len += n
            // buffer[len] = '\0'
            res = res or Res.CHANGE.value
        }
        /* handle backspace */
        if (context.keyPressed == Key.BACKSPACE && len > 0) {
            /* skip utf-8 continuation bytes */
            buff = buff.drop(1)
            res = res or Res.CHANGE.value
        }
        /* handle return */
        if (context.keyPressed == Key.RETURN) {
            setFocus(context, 0U)
            res = res or Res.SUBMIT.value
        }
    }

    /* draw */
    drawControlFrame(context, id, rect, Colors.BASE, opt)
    if (context.focus == id) {
        val color = context.style.colors[Colors.TEXT.ordinal]
        val font = context.style.font!!
        val textw = context.textWidth(font, buff)
        val texth = context.textHeight(font)
        val ofx = rect.w - context.style.padding - textw - 1
        val textx = rect.x + min(ofx, context.style.padding)
        val texty = rect.y + (rect.h - texth) / 2
        pushClipRect(context, rect)
        drawText(context, font, buff, Vec2(textx, texty), color)
        drawRect(context, Rect(textx + textw, texty, 1, texth), color)
        popClipRect(context)
    } else {
        drawControlText(context, buff, rect, Colors.TEXT, opt)
    }

    return res
}

fun numberTextBox(context: Context, value: Float, rect: Rect, id: Id): Int {
    if (context.mousePressed == Mouse.LEFT && context.keyDown == Key.SHIFT && context.hover == id) {
        context.numberEdit = id
        context.numberEditBuf = value.toString()
    }
    if (context.numberEdit == id) {
        val res = textBoxRaw(context, context.numberEditBuf, context.numberEditBuf.length, id, rect, Opt.NONE)
        if (res == Res.SUBMIT.value || context.focus != id) {
            context.numberEdit = 0U
        } else {
            return 1
        }
    }
    return 0
}

fun textBoxEx(context: Context, buffer: String, bufferSize: Int, opt: Opt): Int {
    val id = getId(context, buffer)
    val r = layoutNext(context)
    return textBoxRaw(context, buffer, bufferSize, id, r, opt)
}

fun sliderEx(context: Context, value: Float, low: Float, high: Float, step: Float, fmt: String, opt: Opt): Int {
    var innerValue = value
    var res = 0
    val last = value
    var v = last
    val id = getId(context, value.toString())
    val base = layoutNext(context)
    /* handle text input mode */
    if (numberTextBox(context, v, base, id) != 0) {
        return res; }

    /* handle normal mode */
    updateControl(context, id, base, opt)

    /* handle input */
    if (context.focus == id &&
            (context.mouseDown.value or context.mousePressed.value) == Mouse.LEFT.value) {
        v = low + (context.mousePos.x - base.x) * (high - low) / base.w
        if (step != 0f) {
            v = (((v + step / 2) / step)) * step; }
    }
    /* clamp and store value, update res */
    innerValue = clamp(v.toInt(), low.toInt(), high.toInt()).toFloat()
    if (last != v) {
        res = res or Res.CHANGE.value; }

    /* draw base */
    drawControlFrame(context, id, base, Colors.BASE, opt)
    /* draw thumb */
    val w = context.style.thumbSize
    val x = (v - low) * (base.w - w) / (high - low)
    val thumb = Rect(base.x + x.toInt(), base.y, w, base.h)
    drawControlFrame(context, id, thumb, Colors.BUTTON, opt)
    /* draw text  */
    memScoped {
        val buffer = allocArray<ByteVar>(MAX_FMT)
        sprintf(buffer, fmt, v)
        drawControlText(context, buffer.toKString(), base, Colors.TEXT, opt)
    }

    return res
}

fun numberEx(context: Context, value: Float, step: Float, fmt: String, opt: Opt): Int {
    var innerValue = value
    var res = 0
    val id = getId(context, value.toString())
    val base = layoutNext(context)
    val last = value

    /* handle text input mode */
    if (numberTextBox(context, value, base, id) != 0) {
        return res
    }

    /* handle normal mode */
    updateControl(context, id, base, opt)

    /* handle input */
    if (context.focus == id && context.mouseDown == Mouse.LEFT) {
        innerValue += context.mouseDelta.x * step
    }
    /* set flag if value changed */
    if (innerValue != last) {
        res = res or Res.CHANGE.value
    }

    /* draw base */
    drawControlFrame(context, id, base, Colors.BASE, opt)
    /* draw text  */
    memScoped {
        val buffer = allocArray<ByteVar>(MAX_FMT)
        sprintf(buffer, fmt, innerValue)
        drawControlText(context, buffer.toKString(), base, Colors.TEXT, opt)
    }

    return res
}

fun header(context: Context, label: String, isTreeNode: Boolean, opt: Opt): Res {
    val id = getId(context, label)
    val idx = poolGet(context, context.treeNodePool!!, id)
    val width = -1
    layoutRow(context, intArrayOf(1), width)

    var active = (idx >= 0)
    val expanded = if (opt == Opt.EXPANDED) !active else active
    val r = layoutNext(context)
    updateControl(context, id, r, Opt.NONE)

    /* handle click */
    active = active xor (context.mousePressed == Mouse.LEFT && context.focus == id)

    /* update pool ref */
    if (idx >= 0) {
        if (active) {
            poolUpdate(context, context.treeNodePool, idx); } else {
            context.treeNodePool[idx] = PoolItem(0U, 0)
        }
    } else if (active) {
        initPool(context, context.treeNodePool, TREE_NODE_POOL_SIZE, id)
    }

    /* draw */
    if (isTreeNode) {
        if (context.hover == id) {
            context.drawFrame(context, r, Colors.BUTTON_HOVER); }
    } else {
        drawControlFrame(context, id, r, Colors.BUTTON, Opt.NONE)
    }
    drawIcon(
            context, if (expanded) Icon.EXPANDED else Icon.COLLAPSED,
            Rect(r.x, r.y, r.h, r.h), context.style.colors[Colors.TEXT.ordinal])
    r.x += r.h - context.style.padding
    r.w -= r.h - context.style.padding
    drawControlText(context, label, r, Colors.TEXT, Opt.NONE)

    return if (expanded) Res.ACTIVE else Res.NONE
}

fun headEx(context: Context, label: String, opt: Opt): Res {
    return header(context, label, false, opt)
}

fun beginTreeNodeEx(context: Context, label: String, opt: Opt): Res {
    val res = header(context, label, true, opt)
    if (res == Res.ACTIVE) {
        val layout = getLayout(context)
        if (layout.indent != null) {
            layout.indent = (layout.indent ?: 0) + context.style.indent
        }
        context.idStack.addLast(context.lastId)
    }
    return res
}

fun endTreeNode(context: Context) {
    val layout = getLayout(context)
    if (layout.indent != null) {
        layout.indent = (layout.indent ?: 0) - context.style.indent
    }
    popId(context)
}

fun scrollbar(context: Context, cnt: Container, b: Rect, cs: Rect, x: Int, y: Int, w: Int, h: Int) {
    /* only add scrollbar if content size is larger than body */
    val maxScroll = cs.y - b.h

    if (maxScroll > 0 && b.h > 0) {
        val id = getId(context, "!scrollbar$y")

        /* get sizing / positioning */
        val base = b
        base.x = b.x + b.w
        base.w = context.style.scrollbarSize

        /* handle input */
        updateControl(context, id, base, 0)
        if (context.focus == id && context.mouseDown == Mouse.LEFT) {
            cnt.scroll.y += context.mouseDelta.y * cs.y / base.h
        }
        /* clamp scroll to limits */
        cnt.scroll.y = clamp(cnt.scroll.y, 0, maxScroll)

        /* draw base and thumb */
        context.drawFrame(context, base, Colors.SCROLL_BASE)
        val thumb = base
        thumb.h = max(context.style.thumbSize, base.h * b.h / cs.y)
        thumb.y += cnt.scroll.y * (base.h - thumb.h) / maxScroll
        context.drawFrame(context, thumb, Colors.SCROLL_THUMB)

        /* set this as the scroll_target (will get scrolled on mousewheel) */
        /* if the mouse is over it */
        if (mouseOver(context, b)) {
            context.scrollTarget = cnt; }
    } else {
        cnt.scroll.y = 0
    }
}