package microui

import SDL.*
import kotlinx.cinterop.*
import platform.opengl32.*
import kotlin.math.min
import platform.opengl32.glEnable as glEnable1

const val BUFFER_SIZE = 16384
val texBuf = FloatArray(BUFFER_SIZE * 8)
val vertBuf = FloatArray(BUFFER_SIZE * 8)
val colorBuf = UByteArray(BUFFER_SIZE * 8)
val indexBuf = UIntArray(BUFFER_SIZE * 8)

const val width = 800
const val height = 600
var bufIdx = 0

var window: CPointer<cnames.structs.SDL_Window>? = null

fun initRendere() = memScoped {
    window = SDL_CreateWindow(
        "Microu UI Kt",
        SDL_WINDOWPOS_UNDEFINED.convert(),
        SDL_WINDOWPOS_UNDEFINED.convert(),
        width,
        height,
        SDL_WINDOW_OPENGL
    )
    SDL_GL_CreateContext(window)

    /* init gl */
    glEnable1(GL_BLEND)
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
    glDisable(GL_CULL_FACE)
    glDisable(GL_DEPTH_TEST)
    glEnable1(GL_SCISSOR_TEST)
    glEnable1(GL_TEXTURE_2D)
    glEnableClientState(GL_VERTEX_ARRAY)
    glEnableClientState(GL_TEXTURE_COORD_ARRAY)
    glEnableClientState(GL_COLOR_ARRAY)

    /* init texture */
    val id: UIntVar = alloc()
    glGenTextures(1, id.ptr)
    glBindTexture(GL_TEXTURE_2D, id.value)
    glTexImage2D(
        GL_TEXTURE_2D, 0, GL_ALPHA, ATLAS_WIDTH, ATLAS_HEIGHT, 0,
        GL_ALPHA, GL_UNSIGNED_BYTE, atlasTexture.toCValues()
    )
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
    assert(glGetError() == 0U)
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
        flush(); }

    val texvert_idx = bufIdx * 8
    val color_idx = bufIdx * 16
    val element_idx = bufIdx * 4
    val index_idx = bufIdx * 6
    bufIdx++

    /* update texture buffer */
    val x = src.x / ATLAS_WIDTH.toFloat()
    val y = src.y / ATLAS_HEIGHT.toFloat()
    val w = src.w / ATLAS_WIDTH.toFloat()
    val h = src.h / ATLAS_HEIGHT.toFloat()
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
}

fun drawRect(rect: Rect, color: Color) {
    pushQuad(rect, atlas[ATLAS_WHITE] ?: error("Atlas not found"), color)
}

fun drawText(text: String, pos: Vec2, color: Color) {
    val dst = Rect(pos.x, pos.y, 0, 0)
    for (p in text) {
        if ((p.toInt() and 0xc0) == 0x80) {
            continue; }
        val chr = min(p.toInt(), 127)
        val src = atlas[ATLAS_FONT + chr] ?: error("Atlas ATLAS_FONT + $chr not found")
        dst.w = src.w
        dst.h = src.h
        pushQuad(dst, src, color)
        dst.x += dst.w
    }
}

fun drawIcon(id: Icon, rect: Rect, color: Color) {
    val src = atlas[id.ordinal] ?: error("Atlas $id not found")
    val x = rect.x + (rect.w - src.w) / 2
    val y = rect.y + (rect.h - src.h) / 2
    pushQuad(Rect(x, y, src.w, src.h), src, color)
}

fun getTextWidth(text: String): Int {
    var res = 0
    for (p in text) {
        if ((p.toInt() and 0xc0) == 0x80) {
            continue; }
        val chr = min(p.toInt(), 127)
        val rect = atlas[ATLAS_FONT + chr] ?: error("Atlas ATLAS_FONT + $chr not found")
        res += rect.w
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