package microui

enum class Clip { PART, ALL, NONE }

enum class Colors {
    TEXT,
    BORDER,
    WINDOW_BG,
    TITLE_BG,
    TITLE_TEXT,
    PANEL_BG,
    BUTTON,
    BUTTON_HOVER,
    BUTTON_FOCUS,
    BASE,
    BASE_HOVER,
    BASE_FOCUS,
    SCROLL_BASE,
    SCROLL_THUMB,
    MAX
}

enum class Icon{
    CLOSE,
    CHECK,
    COLLAPSED,
    EXPANDED,
    MAX
}

enum class Res(val value: Int){
    ACTIVE(1 shl 0),
    SUBMIT(1 shl 1),
    CHANGE(1 shl 2)
}

enum class Opt(val value: Int){
    NONE          (0),
    ALIGN_CENTER  (1 shl 0),
    ALIGN_RIGHT   (1 shl 1),
    NO_INTERACT   (1 shl 2),
    NO_FRAME      (1 shl 3),
    NO_RESIZE     (1 shl 4),
    NO_SCROLL     (1 shl 5),
    NO_CLOSE      (1 shl 6),
    NO_TITLE      (1 shl 7),
    HOLD_FOCUS    (1 shl 8),
    AUTO_SIZE     (1 shl 9),
    POPUP         (1 shl 10),
    CLOSED        (1 shl 11),
    EXPANDED      (1 shl 12)
}

enum class Mouse(val value: Int) {
    LEFT       (1 shl 0),
    RIGHT      (1 shl 1),
    MIDDLE     (1 shl 2)
}

enum class Key(val value: Int){
    SHIFT        (1 shl 0),
    CTRL         (1 shl 1),
    ALT          (1 shl 2),
    BACKSPACE    (1 shl 3),
    RETURN       (1 shl 4)
}