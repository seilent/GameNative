package app.gamenative.ui.enums

sealed interface LibraryTabItem {
    data class Store(val tab: LibraryTab) : LibraryTabItem
    data class Collection(val id: String, val name: String) : LibraryTabItem
}
