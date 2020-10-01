package microui

import platform.zlib.voidp

typealias Id = UInt
typealias Font = voidp

class Vec2(var x: Int, var y: Int)
class Rect(val x: Int, val y: Int, val w: Int, val h: Int)
class Color(val r: UByte, val g: UByte, val b: UByte, val a: UByte)
class PoolItem(val id: Id, val lastUpdate: Int)

class BaseCommand(val type: Command, val size: Int)
class JumpCommand(val base: BaseCommand, val dst: voidp)
class ClipCommand(val base: BaseCommand, val rect: Rect, )
class RectCommand(val base: BaseCommand, val rect: Rect, val color: Color)
class TextCommand(val base: BaseCommand, val font: voidp, val pos: Vec2, val color: Color, val char: Char)
class IconCommand(val base: BaseCommand, val rect: Rect, val id: Int, val color: Color)

class Layout(
    val body: Rect,
    val next: Rect,
    val position: Vec2,
    val size: Vec2,
    val max: Vec2,
    val widths: Array<Int>,
    val items: Int,
    val item_index: Int,
    val next_row: Int,
    val next_type: Int,
    val indent: Int
)

class Container(
    val head: Command,
    val tail: Command,
    val rect: Rect,
    val body: Rect,
    val contentSize: Vec2,
    val scroll: Vec2,
    val zIndex: Int,
    val open: Int
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

class Context (
    /* callbacks */
    val textWidth: ((font: Font, text: String) -> Int)? = null,
    val textHeight: ((font: Font) -> Int)? = null,
    val drawFrame: ((ctx: Context, rect: Rect, color: Colors) -> Unit)? = null,
    /* core state */
    val style: Style,
    val hover: Id = 0U,
    val focus: Id = 0U,
    val lastId: Id = 0U,
    val lastRect: Rect = UNCLIPPED_RECT,
    val lastZIndex: Int = 0,
    val updatedFocus: Int = 0,
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
    val mousePos: Vec2 = Vec2(0, 0),
    val lastMousePos: Vec2 = Vec2(0, 0),
    val mouseDelta: Vec2 = Vec2(0, 0),
    val scrollDelta: Vec2 = Vec2(0, 0),
    val mouseDown: Int = 0,
    val mousePressed: Int = 0,
    val keyDown: Int = 0,
    val keyPressed: Int = 0,
    val inputText: String = ""
)