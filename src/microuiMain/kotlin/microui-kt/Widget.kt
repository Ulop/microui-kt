package `microui-kt`

data class Position(val x: Int, val y: Int)

private operator fun Position.plus(parentPosition: Position): Position {
    return copy(x = x + parentPosition.x, y = y + parentPosition.y)
}

enum class MouseButton {
    Left, Middle, Right
}

sealed class MouseEvent(val position: Position) {
    class Down(val button: MouseButton, position: Position) : MouseEvent(position)
    class Up(val button: MouseButton, position: Position) : MouseEvent(position)
    class Move(position: Position) : MouseEvent(position)
}

abstract class Widget(val id: Int = 0) : WidgetDrawer {
    override var absolutePosition = Position(0, 0)
    val body: Rect
        get() = Rect(absolutePosition.x, absolutePosition.y, getWidth(), getHeight())

    abstract fun getWidth(): Int
    abstract fun getHeight(): Int
    abstract fun draw(context: Context)

    internal open fun setAbsolutePosition(position: Position) {
        this.absolutePosition = position
    }
}

interface WidgetDrawer {
    var absolutePosition: Position

    fun Context.pushRect(rect: Rect, color: Color) = pushDrawCommand(DrawCommand.DrawRect(rect, color))

    fun Context.drawRect(rect: Rect, color: Color) {
        this.pushRect(
            rect.copy(
                x = rect.x + absolutePosition.x,
                y = rect.y + absolutePosition.y
            ),
            color
        )
    }
}

abstract class Container(val childs: MutableList<Widget>, id: Int) : Widget(id) {
    // positions relative to parent
    private var positions: List<Position> = listOf()

    abstract fun calcChildPositions(): List<Position>

    fun layoutChilds() {
        positions = calcChildPositions()
        childs.forEachIndexed { index, widget ->
            if (widget is Container) {
                widget.layoutChilds()
            }
            widget.setAbsolutePosition(positions[index] + absolutePosition)
        }
    }

    override fun setAbsolutePosition(position: Position) {
        super.setAbsolutePosition(position)
        if (childs.size != positions.size) return

        childs.forEachIndexed { index, widget ->
            widget.setAbsolutePosition(positions[index] + absolutePosition)
        }
    }

    override fun draw(context: Context) {
        childs.forEach { it.draw(context) }
    }
}

class Column(childs: MutableList<Widget>, id: Int) : Container(childs, id) {
    override fun getWidth(): Int {
        return childs.maxOf { it.getWidth() }
    }

    override fun getHeight(): Int {
        return childs.sumBy { it.getHeight() }
    }

    override fun calcChildPositions(): List<Position> {
        var prevHeight = 0
        return childs.map { widget ->
            val result = Position(0, prevHeight)
            prevHeight += widget.getHeight()
            result
        }
    }

    override fun toString(): String {
        return "Column"
    }
}

class Row(childs: MutableList<Widget>, id: Int) : Container(childs, id) {
    override fun getWidth(): Int {
        return childs.sumBy { it.getWidth() }
    }

    override fun getHeight(): Int {
        return childs.maxOf { it.getHeight() }
    }

    override fun calcChildPositions(): List<Position> {
        var prevWidth = 0
        return childs.map { widget ->
            val result = Position(prevWidth, 0)
            prevWidth += widget.getWidth()
            result
        }
    }

    override fun toString(): String {
        return "Row"
    }
}

class Box(id: Int) : Widget(id) {
    override fun getWidth() = 200

    override fun getHeight() = 80

    override fun draw(context: Context) {
        context.drawRect(Rect(0, 0, getWidth(), getHeight()), Color(19U, 19U, 19U))
        context.drawRect(Rect(2, 2, getWidth() - 2, getHeight() - 2), Color(32U, 32U, 32U))
    }
}

abstract class StatefulWidget<State>(id: Int) : Widget(id) {
    abstract var state: State
}

data class WidgetState(val hovered: Boolean, val pressed: Boolean)

interface MouseListener {
    fun onMouseEnter() {}
    fun onMouseLeave() {}
    fun onMouseDown(event: MouseEvent.Down) {}
    fun onMouseUp(event: MouseEvent.Up) {}
    fun onMouseMove(event: MouseEvent.Move) {}
}


class Button(id: Int) : StatefulWidget<WidgetState>(id), MouseListener {
    override var state = WidgetState(hovered = false, pressed = false)

    override fun getWidth() = 180

    override fun getHeight() = 60

    override fun onMouseEnter() {
        super.onMouseEnter()
        state = state.copy(hovered = true)
    }

    override fun onMouseLeave() {
        super.onMouseLeave()
        state = state.copy(hovered = false)
    }

    override fun onMouseDown(event: MouseEvent.Down) {
        super.onMouseDown(event)
        state = state.copy(pressed = true)
        println("Down: $this")
    }

    override fun onMouseUp(event: MouseEvent.Up) {
        super.onMouseUp(event)
        state = state.copy(pressed = true)
        println("Up: $this")
    }

    override fun draw(context: Context) {
        context.drawRect(Rect(0, 0, getWidth(), getHeight()), Color(19U, 19U, 19U))

        val colorValue: UByte = if (state.hovered) 76U else 32U
        val borderWidth = if (state.pressed) 10 else 4
        context.drawRect(
            Rect(
                borderWidth,
                borderWidth,
                getWidth() - borderWidth * 2,
                getHeight() - borderWidth * 2
            ), Color(colorValue, colorValue, colorValue)
        )
    }

    override fun toString(): String {
        return "Button(${this.body}) with state $state"
    }
}
