package microui

import SDL.*
import kotlinx.cinterop.*
import platform.opengl32.*
import kotlin.math.min

fun get_SDL_Error() = SDL_GetError()!!.toKString()

const val BUFFER_SIZE = 16384
val texBuf = FloatArray(BUFFER_SIZE * 8)
val vertBuf = FloatArray(BUFFER_SIZE * 8)
val colorBuf = UByteArray(BUFFER_SIZE * 8)
val indexBuf = UIntArray(BUFFER_SIZE * 8)

const val width = 800
const val height = 600
var bufIdx = 0

var window: CPointer<cnames.structs.SDL_Window>? = null
var renderer: CPointer<cnames.structs.SDL_Renderer>? = null

fun initRendere() = memScoped {
    window = SDL_CreateWindow(
        "Microu UI Kt",
        SDL_WINDOWPOS_UNDEFINED.convert(),
        SDL_WINDOWPOS_UNDEFINED.convert(),
        width,
        height,
        SDL_WINDOW_SHOWN or SDL_WINDOW_ALLOW_HIGHDPI
    )
    if (window == null) {
        println("SDL_CreateWindow Error: ${get_SDL_Error()}")
        SDL_Quit()
        throw Error()
    }
    renderer = SDL_CreateRenderer(window, -1, SDL_RENDERER_ACCELERATED or SDL_RENDERER_PRESENTVSYNC)
    if (renderer == null) {
        SDL_DestroyWindow(window)
        println("SDL_CreateRenderer Error: ${get_SDL_Error()}")
        SDL_Quit()
        throw Error()
    }
}

fun flush() = memScoped {
    if (bufIdx == 0) {
        return; }

    glViewport(0, 0, width, height)
    glMatrixMode(GL_PROJECTION)
    glPushMatrix()
    glLoadIdentity()
    glOrtho(0.0, width.toDouble(), height.toDouble(), 0.0, -1.0, +1.0)
    glMatrixMode(GL_MODELVIEW)
    glPushMatrix()
    glLoadIdentity()

    glTexCoordPointer(2, GL_FLOAT, 0, texBuf.toCValues())
    glVertexPointer(2, GL_FLOAT, 0, vertBuf.toCValues())
    glColorPointer(4, GL_UNSIGNED_BYTE, 0, colorBuf.toCValues())
    glDrawElements(GL_TRIANGLES, bufIdx * 6, GL_UNSIGNED_INT, indexBuf.toCValues())

    glMatrixMode(GL_MODELVIEW)
    glPopMatrix()
    glMatrixMode(GL_PROJECTION)
    glPopMatrix()

    bufIdx = 0
}


fun pushQuad(dst: Rect, src: Rect, color: Color) {
    if (bufIdx == BUFFER_SIZE) {
        println("flush")
        flush()
    }

    val texvert_idx = bufIdx * 8
    val color_idx = bufIdx * 16
    val element_idx = bufIdx * 4
    val index_idx = bufIdx * 6
    bufIdx++

    /* update texture buffer */
    val x = src.x.toFloat() // / ATLAS_WIDTH.toFloat()
    val y = src.y.toFloat() // / ATLAS_HEIGHT.toFloat()
    val w = src.w.toFloat() // / ATLAS_WIDTH.toFloat()
    val h = src.h.toFloat() // / ATLAS_HEIGHT.toFloat()
    texBuf[texvert_idx + 0] = x
    texBuf[texvert_idx + 1] = y
    texBuf[texvert_idx + 2] = x + w
    texBuf[texvert_idx + 3] = y
    texBuf[texvert_idx + 4] = x
    texBuf[texvert_idx + 5] = y + h
    texBuf[texvert_idx + 6] = x + w
    texBuf[texvert_idx + 7] = y + h

    /* update vertex buffer */
    vertBuf[texvert_idx + 0] = (dst.x).toFloat()
    vertBuf[texvert_idx + 1] = (dst.y).toFloat()
    vertBuf[texvert_idx + 2] = (dst.x + dst.w).toFloat()
    vertBuf[texvert_idx + 3] = (dst.y).toFloat()
    vertBuf[texvert_idx + 4] = (dst.x).toFloat()
    vertBuf[texvert_idx + 5] = (dst.y + dst.h).toFloat()
    vertBuf[texvert_idx + 6] = (dst.x + dst.w).toFloat()
    vertBuf[texvert_idx + 7] = (dst.y + dst.h).toFloat()

    /* update color buffer */
    colorBuf[color_idx + 0] = color.r
    colorBuf[color_idx + 4] = color.g
    colorBuf[color_idx + 8] = color.b
    colorBuf[color_idx + 12] = color.a

    /* update index buffer */
    val ubyteIdx = element_idx.toUByte()
    indexBuf[index_idx + 0] = ubyteIdx + 0U
    indexBuf[index_idx + 1] = ubyteIdx + 1U
    indexBuf[index_idx + 2] = ubyteIdx + 2U
    indexBuf[index_idx + 3] = ubyteIdx + 2U
    indexBuf[index_idx + 4] = ubyteIdx + 3U
    indexBuf[index_idx + 5] = ubyteIdx + 1U
    println("Quad pushed")
}

private fun stretch(value: Int) = (value.toFloat() * 1 + 0.5).toInt()

fun drawRect(rect: Rect, color: Color) {
    println("Render $rect with $color")
    /// pushQuad(rect.copy(w = 20), atlas[ATLAS_WHITE] ?: error("Atlas not found"), color)
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

fun drawText(text: String, pos: Vec2, color: Color) {
    println("render text $text")
    val dst = Rect(pos.x, pos.y, 0, 0)
/*for (p in text) {
    if ((p.toInt() and 0xc0) == 0x80) {
        continue; }
    val chr = min(p.toInt(), 127)
    println("Char code $chr")
    val src = atlas[ATLAS_FONT + chr] ?: error("Atlas ATLAS_FONT + $chr not found")
    println(src)
    dst.w = src.w
    dst.h = src.h
    pushQuad(dst, src, color)
    dst.x += dst.w
}*/
}

fun drawIcon(id: Icon, rect: Rect, color: Color) {
/*val src = atlas[id.ordinal] ?: error("Atlas $id not found")
val x = rect.x + (rect.w - src.w) / 2
val y = rect.y + (rect.h - src.h) / 2
pushQuad(Rect(x, y, src.w, src.h), src, color)*/
}

fun getTextWidth(text: String): Int {
    println(text + "length ${text.length}")
    var res = 0
    for (p in text) {
        if ((p.toInt() and 0xc0) == 0x80 || p.toInt() == 0) {
            continue
        }
        val chr = min(p.toInt(), 127)
        /*val rect = atlas[ATLAS_FONT + chr] ?: error("Atlas ATLAS_FONT + $chr not found")
        res += rect.w*/
    }
    return res
}

fun getTextHeight() = 18

fun setClipRect(rect: Rect) {
    flush()
    glScissor(rect.x, height - (rect.y + rect.h), rect.w, rect.h)
}

fun clear(color: Color) {
    flush()
    glClearColor(
        color.r.toFloat() / 255f,
        color.g.toFloat() / 255f,
        color.b.toFloat() / 255f,
        color.a.toFloat() / 255f
    )
    glClear(GL_COLOR_BUFFER_BIT)
}

fun present() {
    flush()
    SDL_GL_SwapWindow(window)
}