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
    private val getRoot: () -> Container,
    private val position: Position,
    private val visualizer: Visualizer
) {
    private val commands: ArrayDeque<DrawCommand> = ArrayDeque()
    private val oldCommands = ArrayDeque<DrawCommand>()

    fun pushRect(rect: Rect, color: Color) = commands.addLast(DrawCommand.DrawRect(rect, color))

    fun mainLoop(): Boolean {
        val root = getRoot()

        root.layoutChilds()
        root.draw(this, position)

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
                            walkTree(root) {
                                val (posX, posY) = cmd.event.position
                                if (it is MouseListener) {
                                    println("Mouse listener ${it.body}")
                                    val (x, y, w, h) = it.body
                                    val intersect = x <= posX && posX <= x + w && y <= posY && posY <= y + h
                                    if (intersect) it.onMouseMove(MouseEvent.Move(cmd.event.position))
                                }
                                false
                            }
                        }
                    }
                }
            }
        }

        root.layoutChilds()
        root.draw(this, position)

        visualizer.draw {
            for (command in commands) {
                when (command) {
                    is DrawCommand.DrawRect -> visualizer.drawRect(command.rect, command.color)
                }
            }
        }
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
