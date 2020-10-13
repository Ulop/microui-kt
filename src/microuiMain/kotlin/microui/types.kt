package microui

import platform.zlib.voidp

typealias Id = UInt
typealias Font = voidp

data class Vec2(var x: Int, var y: Int)
data class Rect(var x: Int, var y: Int, var w: Int, var h: Int)
data class Color(val r: UByte, val g: UByte, val b: UByte, val a: UByte)
class PoolItem(var id: Id, var lastUpdate: UInt)
data class Buffer(var value: String)

sealed class Command {
    class BaseCommand : Command()
    class JumpCommand(var dst: Command?) : Command()
    class ClipCommand(val rect: Rect) : Command()
    class RectCommand(val rect: Rect, val color: Color) : Command()
    data class TextCommand(val font: Font?, val pos: Vec2, val color: Color, val str: String) : Command()
    class IconCommand(val rect: Rect, val id: Icon, val color: Color) : Command()
}

class CommandHolder(var comand: Command? = null)

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
    var indent: Int? = null
)

class Container(
    var head: Command? = null,
    var tail: Command? = null,
    var rect: Rect = UNCLIPPED_RECT,
    var body: Rect = UNCLIPPED_RECT,
    val contentSize: Vec2 = Vec2(0, 0),
    val scroll: Vec2 = Vec2(0, 0),
    var zIndex: Int = 0,
    var open: Boolean = false
)

class Style(
    val font: Font?,
    val size: Vec2,
    val padding: Int,
    val spacing: Int,
    val indent: Int,
    val title_height: Int,
    val scrollbarSize: Int,
    val thumbSize: Int,
    val colors: Array<Color>
)

class Context(
    /* callbacks */
    var textWidth: (font: Font?, text: String) -> Int = { _, _ -> 0 },
    var textHeight: ((font: Font?) -> Int) = { _ -> 0 },
    var drawFrame: ((ctx: Context, rect: Rect, color: Colors) -> Unit) = { _, _, _ -> },
    /* core state */
    val style: Style,
    var hover: Id = 0U,
    var focus: Id = 0U,
    var lastId: Id = 0U,
    var lastRect: Rect = UNCLIPPED_RECT,
    var lastZIndex: Int = 0,
    var updatedFocus: Int = 0,
    var frame: UInt = 0U,
    var hoverRoot: Container? = null,
    var nextHoverRoot: Container? = null,
    var scrollTarget: Container? = null,
    var numberEditBuf: Buffer = Buffer(""),
    var numberEdit: Id = 0U,
    /* stacks */
    val commandList: ArrayDeque<Command> = ArrayDeque(COMMAND_LIST_SIZE),
    val rootList: ArrayDeque<Container> = ArrayDeque(ROOT_LIST_SIZE),
    val containerStack: ArrayDeque<Container> = ArrayDeque(CONTAINER_STACK_SIZE),
    val clipStack: ArrayDeque<Rect> = ArrayDeque(CLIP_STACK_SIZE),
    val idStack: ArrayDeque<Id> = ArrayDeque(ID_STACK_SIZE),
    val layoutStack: ArrayDeque<Layout> = ArrayDeque(LAYOUT_STACK_SIZE),
    /* retained state pools */
    val containerPool: Array<PoolItem> = Array(CONTAINER_POOL_SIZE) { PoolItem(0U, 0U) },
    val containers: Array<Container> = Array(CONTAINER_POOL_SIZE) { Container() },
    val treeNodePool: Array<PoolItem> = Array(TREE_NODE_POOL_SIZE) { PoolItem(0U, 0U) },
    /* input state */
    var mousePos: Vec2 = Vec2(0, 0),
    var lastMousePos: Vec2 = Vec2(0, 0),
    val mouseDelta: Vec2 = Vec2(0, 0),
    var scrollDelta: Vec2 = Vec2(0, 0),
    var mouseDown: Mouse = Mouse.NONE,
    var mousePressed: Mouse = Mouse.NONE,
    var keyDown: Key = Key.NONE,
    var keyPressed: Key = Key.NONE,
    var inputText: String = ""
)