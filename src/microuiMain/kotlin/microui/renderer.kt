package microui

import SDL.SDL_CreateWindow
import SDL.SDL_GL_CreateContext
import SDL.SDL_WINDOWPOS_UNDEFINED
import SDL.SDL_WINDOW_OPENGL
import kotlinx.cinterop.*
import platform.opengl32.*
import platform.opengl32.glEnable as glEnable1

const val BUFFER_SIZE = 16384
val textBuf = FloatArray(BUFFER_SIZE * 8)
val vertBuf = FloatArray(BUFFER_SIZE * 8)
val colorBuf = UByteArray(BUFFER_SIZE * 8)
val indexBuff = UIntArray(BUFFER_SIZE * 8)

const val width = 800
const val height = 600
var bufIdx = 0

var window: CPointer<cnames.structs.SDL_Window>? = null

fun init() = memScoped {
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