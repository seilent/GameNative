package app.gamenative.service

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable
data class SteamCollectionJson(
    val id: String = "",
    val name: String = "",
    val added: List<Int> = emptyList(),
    val removed: List<Int> = emptyList(),
    val filterSpec: JsonElement? = null,
)

data class ParsedCollection(
    val id: String,
    val name: String,
    val appIds: List<Int>,
)

data class SteamCollectionsResult(
    val collections: List<ParsedCollection>,
    val hiddenAppIds: Set<Int>,
)

object SteamCollectionsParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(entries: List<Pair<String, String>>): SteamCollectionsResult {
        val collections = mutableListOf<ParsedCollection>()
        var hiddenAppIds = emptySet<Int>()

        for ((key, value) in entries) {
            if (!key.startsWith("user-collections.")) continue

            val parsed = try {
                json.decodeFromString<SteamCollectionJson>(value)
            } catch (_: Exception) {
                continue
            }

            if (key == "user-collections.hidden") {
                hiddenAppIds = parsed.added.toSet() - parsed.removed.toSet()
                continue
            }

            if (parsed.filterSpec != null) continue
            if (parsed.id.isBlank()) continue

            val removedSet = parsed.removed.toSet()
            collections.add(
                ParsedCollection(
                    id = parsed.id,
                    name = parsed.name,
                    appIds = parsed.added.filter { it !in removedSet },
                )
            )
        }

        return SteamCollectionsResult(collections, hiddenAppIds)
    }
}
