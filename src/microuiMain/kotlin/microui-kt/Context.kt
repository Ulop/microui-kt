package `microui-kt`

data class Rect(var x: Int, var y: Int, var w: Int, var h: Int)
data class Color(val r: UByte, val g: UByte, val b: UByte, val a: UByte = 255U)

sealed class DrawCommand {
    data class DrawRect(val rect: Rect, val color: Color) : DrawCommand()
}

sealed class UserCommand {
    object Exit : UserCommand()
    class Mouse(val event: MouseEvent) : UserCommand()
}

interface Visualizer {
    fun draw(bloc: Visualizer.() -> Unit)
    fun drawRect(rect: Rect, color: Color)
    fun readCommands(): List<UserCommand>
}

class Context(
    private val getRoot: Context.() -> Container,
    private val position: Position,
    private val visualizer: Visualizer
) {
    private val commands: ArrayDeque<DrawCommand> = ArrayDeque()

    private var _id = 0
    val id
        get() = _id++

    private var hoveredId = -1
    private var lastHoveredId = -1

    fun pushDrawCommand(command: DrawCommand) = commands.addLast(command)

    fun mainLoop(): Boolean {
        val root = getRoot()
        root.setAbsolutePosition(position)
        root.layoutChilds()

        val userCommands = visualizer.readCommands()
        for (cmd in userCommands) {
            when (cmd) {
                UserCommand.Exit -> return false
                is UserCommand.Mouse -> {
                    when (cmd.event) {
                        is MouseEvent.Down -> {
                        }
                        is MouseEvent.Up -> {
                        }
                        is MouseEvent.Move -> {
                            //println(cmd.event.position)
                            val (posX, posY) = cmd.event.position
                            var intersectCount = 0
                            walkTree(root) {
                                if (it is MouseListener) {
                                    val (x, y, w, h) = it.body
                                    val intersect = x <= posX && posX <= x + w && y <= posY && posY <= y + h
                                    if (intersect) {
                                        intersectCount++
                                        it.onMouseMove(MouseEvent.Move(cmd.event.position))
                                        if (hoveredId != it.id) {
                                            lastHoveredId = hoveredId
                                            hoveredId = it.id
                                        }
                                    }
                                }
                                false
                            }
                            if (intersectCount == 0) {
                                lastHoveredId = -1
                                walkTree(root) {
                                    if (it is MouseListener && hoveredId == it.id) {
                                        it.onMouseLeave()
                                        println("Leave #1 $it $hoveredId")
                                        hoveredId = -1
                                        return@walkTree true
                                    }
                                    false
                                }
                            } else {
                                walkTree(root) {
                                    if (it is MouseListener && lastHoveredId == it.id) {
                                        it.onMouseLeave()
                                        println("Leave #2 $it $hoveredId")
                                        return@walkTree true
                                    }
                                    false
                                }
                            }
                        }
                    }
                }
            }
        }

        walkTree(root) {
            if (it is MouseListener && hoveredId == it.id) {
                it.onMouseEnter()
                return@walkTree true
            }
            false
        }
        root.draw(this)

        visualizer.draw {
            for (command in commands) {
                when (command) {
                    is DrawCommand.DrawRect -> visualizer.drawRect(command.rect, command.color)
                }
            }
        }

        _id = 0
        return true
    }
}

fun <Node : Widget> walkTree(root: Node, func: (node: Node) -> Boolean) {
    val nodeStack: ArrayDeque<Node> = ArrayDeque()
    nodeStack.addLast(root)
    while (nodeStack.isNotEmpty()) {
        val node = nodeStack.removeLastOrNull() ?: return
        if (func(node)) {
            continue
        }
        if (node is Container) {
            node.childs.forEach { nodeStack.addLast(it as Node) }
        }
    }
}
