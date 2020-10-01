package microui

import kotlin.math.max
import kotlin.math.min

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
        if (color == Colors.SCROLL_BASE  ||
                color == Colors.SCROLL_THUMB ||
                color == Colors.TITLE_BG) { return; }
        /* draw border */
        if (context.style.colors[Colors.BORDER.ordinal].a.toInt() == 0) {
                drawBox(context, expandRect(rect, 1), context.style.colors[Colors.BORDER.ordinal])
        }
}

fun drawBox(context: Context, expandRect: Rect, color: Color) {
        TODO("Not yet implemented")
}

fun drawRect(context: Context, rect: Rect, color: Color) {
        TODO("Not yet implemented")
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