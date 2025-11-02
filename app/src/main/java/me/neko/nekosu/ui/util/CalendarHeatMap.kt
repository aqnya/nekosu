package me.neko.Nekosu.ui.util

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import me.neko.Nekosu.data.Session
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class DayStat(val date: LocalDate, val count: Int)

// 根据首次记录和当前日期动态生成范围
fun buildDayStats(
    sessions: List<Session>,
    daysBefore: Long = 10,
    daysAfter: Long = 30
): List<DayStat> {
    val today = LocalDate.now()
    // 首次记录日期
    val firstDate = sessions.minOfOrNull { it.timestamp.toLocalDate() } ?: today
    // 范围起止
    val start = firstDate.minusDays(daysBefore)
    val end = today.plusDays(daysAfter)
    val totalDays = ChronoUnit.DAYS.between(start, end).toInt()
    // 汇总
    val map = sessions.groupingBy { it.timestamp.toLocalDate() }.eachCount()
    return (0..totalDays).map { offset ->
        val date = start.plusDays(offset.toLong())
        DayStat(date, map[date] ?: 0)
    }
}

@Composable
fun CalendarHeatMapStyled(
    dayStats: List<DayStat>,
    modifier: Modifier = Modifier,
    cellSize: Dp = 20.dp,
    cellSpacing: Dp = 4.dp,
    // 高对比度色板：0次灰白，1-4次不同警示色，>=5次深警示红
    colors: List<Color> = listOf(
        MaterialTheme.colorScheme.surfaceVariant, // 0 次
        Color(0xFFFFF59D), // 1 次
        Color(0xFFFFC107), // 2 次
        Color(0xFFFF5722), // 3 次
        Color(0xFFF44336), // 4 次
        MaterialTheme.colorScheme.error // >=5 次
    ),
    showCountInCell: Boolean = false,
    weekDayLabels: List<String> = listOf("一", "二", "三", "四", "五", "六", "日"),
    monthLabels: List<String> = listOf(
        "一月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "十一月", "十二月"
    )
) {
    val today = LocalDate.now()
    val firstRecordDate = dayStats.minOfOrNull { it.date } ?: today
    // 显示范围：从首条记录前10天 到 今天后30天
    val startDate = firstRecordDate.minusDays(10)
    val endDate = today.plusDays(30)

    // 对齐到周一开始
    val daysToSubtract = (startDate.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong()
    val alignedStart = if (daysToSubtract > 0) startDate.minusDays(daysToSubtract) else startDate
    // 不再填充到周日，末尾使用精确 endDate
    val alignedEnd = endDate

    val totalDays = ChronoUnit.DAYS.between(alignedStart, alignedEnd).toInt()
    // 构建完整日期列表
    val dateList = remember(alignedStart, alignedEnd) {
        (0..totalDays).map { alignedStart.plusDays(it.toLong()) }
    }
    // 统计映射
    val statsMap = remember(dayStats) { dayStats.associateBy { it.date } }
    val fullStats = remember(dateList, statsMap) {
        dateList.map { date -> DayStat(date, statsMap[date]?.count ?: 0) }
    }
    // 按周分组，每周最多7天，最后一周可能不足7天
    val weeks = remember(fullStats) { fullStats.chunked(7) }
    var tooltipData by remember { mutableStateOf<DayStat?>(null) }
    val scrollState = rememberScrollState()
    LaunchedEffect(Unit) {
        // 滚动到最新的列（即末尾）
        scrollState.scrollTo(scrollState.maxValue)
    }

    Column(modifier) {
        // 月份标签，与热力图一起滚动
        Row(
            Modifier
                .horizontalScroll(scrollState)
                .padding(start = cellSize + cellSpacing)
        ) {
            var lastMonth = 0
            weeks.forEach { week ->
                val month = week.first().date.monthValue
                if (month != lastMonth) {
                    Text(
                        text = monthLabels[month - 1],
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(cellSize + cellSpacing),
                        textAlign = TextAlign.Center
                    )
                    lastMonth = month
                } else {
                    Spacer(Modifier.width(cellSize + cellSpacing))
                }
            }
        }
        Row {
            // 星期标签
            Column(
                verticalArrangement = Arrangement.spacedBy(cellSpacing),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                weekDayLabels.forEach { wd ->
                    Text(wd, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(cellSpacing - 2.dp))
                }
            }
            // 热力图网格
            Row(
                Modifier
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(cellSpacing)
            ) {
                weeks.forEach { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(cellSpacing)) {
                        week.forEach { ds ->
                            val level = ds.count.coerceAtMost(5)
                            val bgColor by animateColorAsState(colors[level])
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(cellSize)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(bgColor)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .pointerInput(ds) {
                                        detectTapGestures(onLongPress = { tooltipData = ds })
                                    }
                            ) {
                                if (showCountInCell && ds.count > 0) {
                                    Text(
                                        text = ds.count.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        // Tooltip
        tooltipData?.let { ds ->
            Popup(
                alignment = Alignment.Center,
                onDismissRequest = { tooltipData = null }
            ) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            ds.date.format(DateTimeFormatter.ISO_DATE),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("${ds.count} 次", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // 图例
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("少", style = MaterialTheme.typography.bodySmall)
            colors.forEach {
                Box(Modifier
                    .size(cellSize)
                    .background(it, RoundedCornerShape(4.dp)))
            }
            Text("多", style = MaterialTheme.typography.bodySmall)
        }
    }
}
