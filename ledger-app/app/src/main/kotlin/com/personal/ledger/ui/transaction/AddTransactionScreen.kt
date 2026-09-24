package com.personal.ledger.ui.transaction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.domain.model.Category
import com.personal.ledger.domain.model.TransactionType
import com.personal.ledger.ui.common.CategoryBadge
import com.personal.ledger.ui.common.cardColor
import com.personal.ledger.ui.common.amountColor
import com.personal.ledger.ui.common.expenseContainerColor
import com.personal.ledger.ui.common.incomeContainerColor
import com.personal.ledger.ui.common.transferContainerColor
import com.personal.ledger.util.Money
import com.personal.ledger.util.TimeUtils
import com.personal.ledger.viewmodel.AddTransactionViewModel
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(container: AppContainer, navController: NavHostController, txId: String?) {
    val vm: AddTransactionViewModel = viewModel {
        AddTransactionViewModel(
            container.ledgerRepository,
            container.accountRepository,
            container.categoryRepository,
            container.merchantRepository,
            container.tagRepository,
            container.transactionRepository,
            container.settings,
        )
    }
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showDateTimeEditor by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var pickTarget by remember { mutableStateOf(false) }
    var showMerchantPicker by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showNoteEditor by remember { mutableStateOf(false) }
    val pagerScope = rememberCoroutineScope()
    val anyEditor = showDatePicker || showDateTimeEditor || showTimePicker || showAccountPicker ||
        showMerchantPicker || showTagPicker || showNoteEditor
    LaunchedEffect(txId) { vm.load(txId) }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            navController.popBackStack()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(if (state.editing != null) "编辑账单" else "记一笔") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        bottomBar = {
            if (!anyEditor) AmountKeyboard(
                saving = state.isSaving || state.isResolvingMerchant,
                onKey = { vm.setAmountText(AmountEntry.press(state.amountText, it)) },
                onSave = { vm.save() },
            )
        },
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
          val categoryRows = if (maxHeight < 560.dp || LocalDensity.current.fontScale > 1.2f) 2 else 3
          val compactCategory = maxHeight < 390.dp || LocalDensity.current.fontScale > 1.4f
          Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
          ) {
            // 类型分段选择（全宽）
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val types = TransactionType.entries
                types.forEachIndexed { index, t ->
                    SegmentedButton(
                        selected = state.type == t,
                        onClick = {
                            vm.setType(t)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                    ) {
                        Text(typeLabel(t))
                    }
                }
            }

            val amountBackground = when (state.type) {
                TransactionType.EXPENSE -> expenseContainerColor()
                TransactionType.INCOME -> incomeContainerColor()
                TransactionType.TRANSFER -> transferContainerColor()
            }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = amountBackground,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(Money.symbolFor(state.baseCurrency),
                        style = MaterialTheme.typography.titleLarge,
                        color = amountColor(state.type))
                    Spacer(Modifier.width(8.dp))
                    val amount = AmountEntry.fromStored(state.amountText).ifEmpty { "0.00" }
                    Text(
                        amount,
                        modifier = Modifier.weight(1f),
                        color = amountColor(state.type),
                        fontSize = if (amount.length > 15) 21.sp else 30.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        softWrap = true,
                    )
                }
            }
            if (state.type != TransactionType.TRANSFER) {
                val categories = if (state.type == TransactionType.EXPENSE)
                    state.expenseCategories else state.incomeCategories
                val roots = remember(categories) { CategorySelection.roots(categories) }
                val selectedRootId = CategorySelection.selectedRootId(categories, state.categoryId)
                val pageSize = categoryRows * 4
                val pageCount = maxOf(1, (roots.size + pageSize - 1) / pageSize)
                val pagerState = rememberPagerState(pageCount = { pageCount })
                val shownPage = pagerState.currentPage.coerceIn(0, pageCount - 1)
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("分类", style = MaterialTheme.typography.titleSmall)
                        Text(CategorySelection.label(categories, state.categoryId),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(
                        onClick = { if (shownPage > 0) pagerScope.launch {
                            pagerState.animateScrollToPage(shownPage - 1)
                        } },
                        enabled = shownPage > 0,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一页分类")
                    }
                    Text((shownPage + 1).toString() + "/" + pageCount,
                        style = MaterialTheme.typography.labelMedium)
                    IconButton(
                        onClick = { if (shownPage + 1 < pageCount) pagerScope.launch {
                            pagerState.animateScrollToPage(shownPage + 1)
                        } },
                        enabled = shownPage + 1 < pageCount,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下一页分类")
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().height(
                        (if (compactCategory) 60.dp else 72.dp) * categoryRows +
                            6.dp * (categoryRows - 1)),
                ) { pageIndex ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        roots.drop(pageIndex * pageSize).take(pageSize).chunked(4).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { category ->
                                    CategoryTile(category, selectedRootId == category.id, compactCategory,
                                        Modifier.weight(1f)) { vm.setCategoryId(category.id) }
                                }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                if (roots.isEmpty()) Text("暂无可用一级分类")
            }

            if (state.type != TransactionType.TRANSFER) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EntryField("账户",
                        state.accounts.firstOrNull { it.id == state.accountId }?.name ?: "选择",
                        Modifier.weight(1f)) {
                        pickTarget = false; showAccountPicker = true
                    }
                    EntryField("日期时间",
                        TimeUtils.formatMonthDay(state.time) + " " + timeOfDay(state.time),
                        Modifier.weight(1f)) { showDateTimeEditor = true }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EntryField("商家",
                        state.merchants.firstOrNull { it.id == state.merchantId }?.name ?: "选择",
                        Modifier.weight(1f)) { showMerchantPicker = true }
                    EntryField("备注", state.note.ifEmpty { "添加" },
                        Modifier.weight(1f)) { showNoteEditor = true }
                    EntryField("标签",
                        if (state.selectedTagIds.isEmpty()) "选择"
                        else state.tags.filter { it.id in state.selectedTagIds }
                            .joinToString("、") { it.name },
                        Modifier.weight(1f)) { showTagPicker = true }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EntryField("转出账户",
                        state.accounts.firstOrNull { it.id == state.accountId }?.name ?: "选择",
                        Modifier.weight(1f)) {
                        pickTarget = false; showAccountPicker = true
                    }
                    EntryField("转入账户",
                        state.accounts.firstOrNull { it.id == state.targetAccountId }?.name ?: "选择",
                        Modifier.weight(1f)) {
                        pickTarget = true; showAccountPicker = true
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EntryField("日期时间",
                        TimeUtils.formatMonthDay(state.time) + " " + timeOfDay(state.time),
                        Modifier.weight(1f)) { showDateTimeEditor = true }
                    EntryField("商家",
                        state.merchants.firstOrNull { it.id == state.merchantId }?.name ?: "选择",
                        Modifier.weight(1f)) { showMerchantPicker = true }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EntryField("备注", state.note.ifEmpty { "添加" },
                        Modifier.weight(1f)) { showNoteEditor = true }
                    EntryField("标签",
                        if (state.selectedTagIds.isEmpty()) "选择"
                        else state.tags.filter { it.id in state.selectedTagIds }
                            .joinToString("、") { it.name },
                        Modifier.weight(1f)) { showTagPicker = true }
                }
            }
            state.merchantNotice?.let {
                Text(it, color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium)
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

          }
        }
    }

    if (showDateTimeEditor) {
        AlertDialog(
            onDismissRequest = { showDateTimeEditor = false },
            title = { Text("日期与时间") },
            text = {
                Column {
                    TextButton(onClick = { showDateTimeEditor = false; showDatePicker = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("日期  " + TimeUtils.formatMonthDay(state.time))
                    }
                    TextButton(onClick = { showDateTimeEditor = false; showTimePicker = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("时间  " + timeOfDay(state.time))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDateTimeEditor = false }) { Text("完成") } },
        )
    }
    if (showDatePicker) {
        val zone = ZoneId.systemDefault()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DatePickerSelection.initialUtcMillis(state.time, zone))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val picked = datePickerState.selectedDateMillis
                    if (picked != null) {
                        vm.setTime(DatePickerSelection.withPickedDate(state.time, picked, zone))
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val time = Instant.ofEpochMilli(state.time).atZone(ZoneId.systemDefault()).toLocalTime()
        var hour by remember { mutableStateOf("%02d".format(time.hour)) }
        var minute by remember { mutableStateOf("%02d".format(time.minute)) }
        val valid = (hour.toIntOrNull()?.let { it in 0..23 } == true) &&
            (minute.toIntOrNull()?.let { it in 0..59 } == true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("选择时间") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(hour, { hour = it.filter(Char::isDigit).take(2) },
                        label = { Text("时") }, modifier = Modifier.weight(1f), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Text(":")
                    OutlinedTextField(minute, { minute = it.filter(Char::isDigit).take(2) },
                        label = { Text("分") }, modifier = Modifier.weight(1f), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            },
            confirmButton = {
                TextButton(enabled = valid, onClick = {
                    val date = Instant.ofEpochMilli(state.time).atZone(ZoneId.systemDefault()).toLocalDate()
                    vm.setTime(date.atTime(hour.toInt(), minute.toInt())
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("取消") } },
        )
    }

    if (showAccountPicker) {
        SimplePickerDialog(
            title = "选择账户",
            items = state.accounts
                .filter { !pickTarget || it.id != state.accountId }
                .map { it.id to it.name },
            onPick = { id ->
                if (pickTarget) vm.setTargetAccountId(id) else vm.setAccountId(id)
                showAccountPicker = false
            },
            onDismiss = { showAccountPicker = false },
        )
    }

    if (showMerchantPicker) {
        val options = state.merchantOptions
        val oldSelected = state.merchants.firstOrNull { it.id == state.merchantId }
        SimplePickerDialog(
            title = "选择商家",
            items = (options + listOfNotNull(oldSelected).filter { old -> options.none { it.id == old.id } })
                .distinctBy { it.id }.map { it.id to it.name },
            hint = if (state.type != TransactionType.TRANSFER && state.categoryId == null)
                "请先选择分类" else null,
            addLabel = if (state.type == TransactionType.TRANSFER || state.categoryId != null)
                "+ 新建商家" else null,
            clearLabel = "不选择商家",
            onClear = {
                vm.setMerchantId(null)
                showMerchantPicker = false
            },
            onPick = { id ->
                vm.setMerchantId(id)
                showMerchantPicker = false
            },
            closeOnAddSuccess = true,
            onAdd = { name, done -> vm.addMerchant(name, done) },
            onDismiss = { showMerchantPicker = false },
        )
    }

    if (showTagPicker) {
        SimplePickerDialog(
            title = "选择标签",
            items = state.tags.map { it.id to ((if (it.id in state.selectedTagIds) "✓ " else "") + it.name) },
            addLabel = "+ 新建标签",
            onPick = { vm.toggleTag(it) },
            onAdd = { name, done -> vm.addTag(name, done) },
            onDismiss = { showTagPicker = false },
        )
    }
    if (showNoteEditor) {
        var note by remember { mutableStateOf(state.note) }
        AlertDialog(
            onDismissRequest = { showNoteEditor = false },
            title = { Text("备注") },
            text = { OutlinedTextField(note, { note = it }, modifier = Modifier.fillMaxWidth(),
                minLines = 3, maxLines = 6, label = { Text("备注内容") }) },
            confirmButton = { TextButton(onClick = { vm.setNote(note); showNoteEditor = false }) {
                Text("完成")
            } },
            dismissButton = { TextButton(onClick = { showNoteEditor = false }) { Text("取消") } },
        )
    }
}

private fun typeLabel(t: TransactionType): String = when (t) {
    TransactionType.EXPENSE -> "支出"
    TransactionType.INCOME -> "收入"
    TransactionType.TRANSFER -> "转账"
}

private fun timeOfDay(ms: Long): String {
    val t = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalTime()
    return "%02d:%02d".format(t.hour, t.minute)
}

@Composable
private fun CategoryTile(
    category: Category,
    selected: Boolean,
    compact: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else cardColor(),
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier.heightIn(min = if (compact) 60.dp else 72.dp)
            .selectable(selected = selected, onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            CategoryBadge(category.name, category.type,
                sizeDp = if (compact) 36 else 48, visualKey = category.id)
            Text(category.name, style = MaterialTheme.typography.labelMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun EntryField(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = cardColor(),
        modifier = modifier.heightIn(min = 48.dp).clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Text(value, style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun AmountKeyboard(saving: Boolean, onKey: (String) -> Unit, onSave: () -> Unit) {
    Surface(shadowElevation = 6.dp, modifier = Modifier.navigationBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Column(modifier = Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf(
                    listOf("1", "2", "3"), listOf("4", "5", "6"),
                    listOf("7", "8", "9"), listOf(".", "0", AmountEntry.BACKSPACE),
                ).forEach { keys ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        keys.forEach { key ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.weight(1f).height(48.dp).clickable { onKey(key) },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(key, style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                TextButton(onClick = { onKey(AmountEntry.CLEAR) },
                    modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("清空") }
                Button(onClick = onSave, enabled = !saving,
                    modifier = Modifier.fillMaxWidth().height(153.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp)) {
                    Text(if (saving) "保存中…" else "保存",
                        style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
/** 简单列表选择对话框；addLabel 非空时提供新建输入框。 */
@Composable
private fun SimplePickerDialog(
    title: String,
    items: List<Pair<String, String>>,
    onPick: (String) -> Unit = {},
    onAdd: ((String, (Result<Unit>) -> Unit) -> Unit)? = null,
    addLabel: String? = "新建",
    closeOnAddSuccess: Boolean = false,
    hint: String? = null,
    clearLabel: String? = null,
    onClear: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    var newName by remember { mutableStateOf("") }
    var addError by remember { mutableStateOf<String?>(null) }
    var isAdding by remember { mutableStateOf(false) }
    val dismissWithoutHaptic = onDismiss
    AlertDialog(
        onDismissRequest = dismissWithoutHaptic,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                hint?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                if (clearLabel != null && onClear != null) {
                    TextButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) {
                        Text(clearLabel)
                    }
                }
                items.forEach { (id, name) ->
                    Text(
                        name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(id) }
                            .heightIn(min = 48.dp)
                            .padding(vertical = 10.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (onAdd != null && addLabel != null) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it; addError = null },
                        label = { Text("名称") },
                        singleLine = true,
                    )
                    addError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(enabled = !isAdding && newName.isNotBlank(), onClick = {
                        isAdding = true
                        onAdd(newName.trim()) { result ->
                            isAdding = false
                            result.onSuccess {
                                newName = ""
                                if (closeOnAddSuccess) onDismiss()
                            }.onFailure { addError = it.message ?: "新建失败，请重试" }
                        }
                    }) { Text(if (isAdding) "创建中…" else addLabel) }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = dismissWithoutHaptic) { Text("取消") }
        },
    )
}
