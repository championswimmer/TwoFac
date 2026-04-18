package tech.arnav.twofac.lib.storage

import kotlinx.serialization.Serializable
import tech.arnav.twofac.lib.PublicApi

/**
 * Fixed palette of 10 accessible colors available for tag assignment.
 * Serialized by name for forward/backward compatibility.
 */
@PublicApi
@Serializable
enum class TagColor(val argb: Long) {
    RED(0xFFEF4444L),
    ORANGE(0xFFF97316L),
    AMBER(0xFFF59E0BL),
    GREEN(0xFF22C55EL),
    TEAL(0xFF14B8A6L),
    BLUE(0xFF3B82F6L),
    INDIGO(0xFF6366F1L),
    PURPLE(0xFFA855F7L),
    PINK(0xFFEC4899L),
    GREY(0xFF6B7280L);

    companion object {
        val palette: List<TagColor> = entries
    }
}

/**
 * A tag that can be assigned to one or more accounts for organization.
 *
 * @param tagId   Unique identifier (UUID as string).
 * @param name    Human-readable tag label (e.g. "Work", "Finance").
 * @param color   Color chosen from the fixed [TagColor] palette.
 */
@PublicApi
@Serializable
data class StoredTag(
    val tagId: String,
    val name: String,
    val color: TagColor,
)
