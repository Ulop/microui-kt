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
    class Up(position: Position) : MouseEvent(position)
    class Move(position: Position) : MouseEvent(position)
}


abstract class Widget(val id: Int = 0) {
    protected var absolutePosition = Position(0, 0)
    val body: Rect
        get() = Rect(absolutePosition.x, absolutePosition.y, getWidth(), getHeight())

    abstract fun getWidth(): Int
    abstract fun getHeight(): Int
    abstract fun draw(context: Context, position: Position)

    internal open fun setAbsolutePosition(position: Position) {
        this.absolutePosition = position
    }
}

abstract class Container(val childs: MutableList<Widget>, id: Int) : Widget(id) {
    // positions relative to parent
    private var positions: List<Position> = listOf()

    abstract fun calcChildPositions(): List<Position>

    fun layoutChilds() {
        positions = calcChildPositions()
        println(positions)
        childs.forEachIndexed { index, widget ->
            if (widget is Container) {
                widget.layoutChilds()
            }
            widget.setAbsolutePosition(positions[index] + absolutePosition)
            println("$widget")
        }
    }

    override fun setAbsolutePosition(position: Position) {
        super.setAbsolutePosition(position)
        if (childs.size != positions.size) return

        childs.forEachIndexed { index, widget ->
            widget.setAbsolutePosition(positions[index] + absolutePosition)
        }
    }

    override fun draw(context: Context, position: Position) {
        val (x, y) = position
        positions.forEachIndexed { i, pos ->
            childs[i].draw(
                context,
                pos.copy(x = pos.x + x, y = pos.y + y)
            )
        }
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

class Box : Widget(666) {
    override fun getWidth() = 200

    override fun getHeight() = 80

    override fun draw(context: Context, position: Position) {
        val (x, y) = position
        context.pushRect(Rect(x, y, getWidth(), getHeight()), Color(19U, 19U, 19U))
        context.pushRect(Rect(x + 2, y + 2, getWidth() - 2, getHeight() - 2), Color(32U, 32U, 32U))
    }
}

abstract class StatefulWidget<State> : Widget(555) {
    abstract var state: State
}

data class WidgetState(val hovered: Boolean, val pressed: Boolean)

interface MouseListener {
    fun onMouseEnter() {}
    fun onMouseLeave() {}
    fun onMousePress(event: MouseEvent.Down) {}
    fun onMouseUp(event: MouseEvent.Up) {}
    fun onMouseMove(event: MouseEvent.Move) {}
}


class Button : StatefulWidget<WidgetState>(), MouseListener {
    override var state = WidgetState(hovered = false, pressed = false)

    override fun getWidth() = 180

    override fun getHeight() = 60

    override fun onMouseMove(event: MouseEvent.Move) {
        super.onMouseMove(event)
        if (!state.hovered) {
            state = state.copy(hovered = true)
            //println("$body $state")
        }
    }

    override fun draw(context: Context, position: Position) {
        val (x, y) = position
        context.pushRect(Rect(x, y, getWidth(), getHeight()), Color(19U, 19U, 19U))

        val colorValue: UByte = if (state.hovered) 76U else 32U
        context.pushRect(Rect(x + 2, y + 2, getWidth() - 2, getHeight() - 2), Color(colorValue, colorValue, colorValue))
    }

    override fun toString(): String {
        return "Button(${this.body})"
    }
}
