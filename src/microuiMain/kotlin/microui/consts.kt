package microui

import kotlin.math.max

const val COMMAND_LIST_SIZE = (256 * 1024)
const val ROOT_LIST_SIZE         = 32
const val CONTAINER_STACK_SIZE   = 32
const val CLIP_STACK_SIZE        = 32
const val ID_STACK_SIZE          = 32
const val LAYOUT_STACK_SIZE      = 16
const val CONTAINER_POOL_SIZE    = 48
const val TREE_NODE_POOL_SIZE     = 48
const val MAX_WIDTHS            = 16
const val REAL_FMT              = "%.3g"
const val SLIDER_FMT            = "%.2f"
const val MAX_FMT               = 127

fun clamp(x: Int, a: Int, b: Int) =  minOf(b, max(a, x))