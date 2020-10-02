package microui

import platform.zlib.voidp

typealias Id = UInt
typealias Font = voidp

class Vec2(var x: Int, var y: Int)
class Rect(var x: Int, var y: Int, var w: Int, var h: Int)
class Color(val r: UByte, val g: UByte, val b: UByte, val a: UByte)
class PoolItem(var id: Id, var lastUpdate: Int)

sealed class Command {
    class JumpCommand(val dst: Command): Command()
    class ClipCommand(val rect: Rect): Command()
    class RectCommand(val rect: Rect, val color: Color): Command()
    class TextCommand(val font: voidp, val pos: Vec2, val color: Color, val char: Char): Command()
    class IconCommand(val rect: Rect, val id: Int, val color: Color): Command()
}

class Layout(
        val body: Rect,
        var next: Rect? = null,
        var position: Vec2? = null,
        val size: Vec2? = null,
        val max: Vec2? = null,
        var widths: IntArray = intArrayOf(),
        var itemIndex: Int? = null,
        var nextRow: Int? = null,
        var nextType: LayoutType? = null,
        val indent: Int? = null
)

class Container(
        val head: Command? = null,
        val tail: Command,
        val rect: Rect,
        val body: Rect,
        val contentSize: Vec2,
        val scroll: Vec2,
        var zIndex: Int,
        var open: Int
)

class Style(
    val font: Font?,
    val size: Vec2,
    val padding: Int,
    val spacing: Int,
    val indent: Int,
    val title_height: Int,
    val scrollbar_size: Int,
    val thumb_size: Int,
    val colors: Array<Color>
)

class Context(
        /* callbacks */
        var textWidth: (font: Font, text: String) -> Int = { _, _ -> 0 },
        var textHeight: ((font: Font) -> Int) = { _ -> 0 },
        var drawFrame: ((ctx: Context, rect: Rect, color: Colors) -> Unit) = { _, _, _ -> },
        /* core state */
        val style: Style,
        val hover: Id = 0U,
        var focus: Id = 0U,
        var lastId: Id = 0U,
        var lastRect: Rect = UNCLIPPED_RECT,
        var lastZIndex: Int = 0,
        var updatedFocus: Int = 0,
        var frame: Int = 0,
        var hoverRoot: Container? = null,
        var nextHoverRoot: Container? = null,
        var scrollTarget: Container? = null,
        val numberEditBuf: CharArray = charArrayOf(),
        val number_edit: Id = 0U,
        /* stacks */
        val commandList: ArrayDeque<Command> = ArrayDeque(COMMAND_LIST_SIZE),
        val rootList: ArrayDeque<Container> = ArrayDeque(ROOT_LIST_SIZE),
        val containerStack: ArrayDeque<Container> = ArrayDeque(CONTAINER_STACK_SIZE),
        val clipStack: ArrayDeque<Rect> = ArrayDeque(CLIP_STACK_SIZE),
        val idStack: ArrayDeque<Id> = ArrayDeque(ID_STACK_SIZE),
        val layoutStack: ArrayDeque<Layout> = ArrayDeque(LAYOUT_STACK_SIZE),
        /* retained state pools */
        val containerPool: Array<PoolItem>? = null,
        val containers: Array<Container>? = null,
        val treeNodePool: Array<PoolItem>? = null,
        /* input state */
        var mousePos: Vec2 = Vec2(0, 0),
        var lastMousePos: Vec2 = Vec2(0, 0),
        val mouseDelta: Vec2 = Vec2(0, 0),
        var scrollDelta: Vec2 = Vec2(0, 0),
        var mouseDown: Boolean = false,
        var mousePressed: Boolean = false,
        var keyDown: Int = 0,
        var keyPressed: Int = 0,
        var inputText: String = ""
)