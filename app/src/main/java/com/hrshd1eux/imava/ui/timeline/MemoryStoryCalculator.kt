package com.hrshd1eux.imava.ui.timeline

import android.content.Context
import androidx.core.os.ConfigurationCompat
import com.hrshd1eux.imava.R
import com.hrshd1eux.imava.data.model.MediaItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object MemoryStoryCalculator {

    fun generateStories(items: List<MediaItem>, context: Context? = null): List<MemoryStory> {
        if (items.isEmpty()) return emptyList()

        val locale = if (context != null) {
            ConfigurationCompat.getLocales(context.resources.configuration)[0] ?: Locale.getDefault()
        } else {
            Locale.getDefault()
        }
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val todayMonth = today.monthValue
        val todayDay = today.dayOfMonth
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)
        val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", locale)

        val stories = mutableListOf<MemoryStory>()

        val exactDayItems = items.filter { item ->
            if (item.dateTaken <= 0) return@filter false
            val itemDate = Instant.ofEpochMilli(item.dateTaken).atZone(zoneId).toLocalDate()
            itemDate.monthValue == todayMonth && itemDate.dayOfMonth == todayDay && itemDate.year < today.year
        }
        if (exactDayItems.isNotEmpty()) {
            val groupedByYear = exactDayItems.groupBy { item ->
                Instant.ofEpochMilli(item.dateTaken).atZone(zoneId).toLocalDate().year
            }
            groupedByYear.forEach { (year, yearItems) ->
                val yearsAgo = today.year - year
                val title = when {
                    context != null && yearsAgo == 1 -> context.getString(R.string.memory_years_ago_one)
                    context != null -> context.getString(R.string.memory_years_ago_plural, yearsAgo)
                    yearsAgo == 1 -> "1 Year Ago Today"
                    else -> "$yearsAgo Years Ago"
                }
                val sampleDate = Instant.ofEpochMilli(yearItems.first().dateTaken).atZone(zoneId).toLocalDate().format(formatter)
                val cover = yearItems.firstOrNull()
                if (cover != null) {
                    stories.add(
                        MemoryStory(
                            id = "memory_day_$year",
                            title = title,
                            dateSubtitle = sampleDate,
                            coverItem = cover,
                            items = yearItems
                        )
                    )
                }
            }
        }

        val sameMonthPastYears = items.filter { item ->
            if (item.dateTaken <= 0) return@filter false
            val itemDate = Instant.ofEpochMilli(item.dateTaken).atZone(zoneId).toLocalDate()
            itemDate.monthValue == todayMonth && itemDate.year < today.year && !exactDayItems.contains(item)
        }
        if (sameMonthPastYears.isNotEmpty()) {
            val groupedByYear = sameMonthPastYears.groupBy { item ->
                Instant.ofEpochMilli(item.dateTaken).atZone(zoneId).toLocalDate().year
            }
            groupedByYear.forEach { (year, yearItems) ->
                val sampleDate = Instant.ofEpochMilli(yearItems.first().dateTaken).atZone(zoneId).toLocalDate().format(monthFormatter)
                val cover = yearItems.firstOrNull()
                if (cover != null && yearItems.size >= 2) {
                    stories.add(
                        MemoryStory(
                            id = "memory_month_$year",
                            title = context?.getString(R.string.memory_month_memories, sampleDate) ?: "$sampleDate Memories",
                            dateSubtitle = context?.getString(R.string.memory_moments_count, yearItems.size) ?: "${yearItems.size} moments",
                            coverItem = cover,
                            items = yearItems
                        )
                    )
                }
            }
        }

        if (stories.isEmpty()) {
            val validItems = items.filter { it.dateTaken > 0 }
            val groupedByMonth = validItems.groupBy { item ->
                val date = Instant.ofEpochMilli(item.dateTaken).atZone(zoneId).toLocalDate()
                "${date.year}-${date.monthValue}"
            }
            groupedByMonth.entries.take(4).forEach { (_, monthItems) ->
                val sampleDate = Instant.ofEpochMilli(monthItems.first().dateTaken).atZone(zoneId).toLocalDate().format(monthFormatter)
                val cover = monthItems.firstOrNull()
                if (cover != null) {
                    stories.add(
                        MemoryStory(
                            id = "memory_fallback_${cover.id}",
                            title = context?.getString(R.string.memory_month_highlights, sampleDate) ?: "$sampleDate Highlights",
                            dateSubtitle = context?.getString(R.string.memory_moments_count, monthItems.size) ?: "${monthItems.size} moments",
                            coverItem = cover,
                            items = monthItems
                        )
                    )
                }
            }
        }

        return stories.sortedByDescending { it.items.firstOrNull()?.dateTaken ?: 0L }
    }
}
