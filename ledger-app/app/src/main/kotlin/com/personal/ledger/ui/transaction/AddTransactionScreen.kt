package com.personal.ledger.ui.transaction

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.personal.ledger.data.AppContainer
import com.personal.ledger.domain.model.Account
import com.personal.ledger.domain.model.Category
import com.personal.ledger.domain.model.TransactionType
import com.personal.ledger.ui.common.CategoryBadge
import com.personal.ledger.ui.common.AccountVisuals
import com.personal.ledger.ui.common.CategoryVisuals
import com.personal.ledger.ui.common.accountTypeLabel
import com.personal.ledger.ui.common.categoryContainerColor
import com.personal.ledger.ui.common.cardColor
import com.personal.ledger.ui.common.amountColor
import com.personal.ledger.ui.common.expenseContainerColor
import com.personal.ledger.ui.common.incomeContainerColor
import com.personal.ledger.ui.common.transferContainerColor
import com.personal.ledger.ui.common.rememberHapticSave
import com.personal.ledger.navigation.Routes
import com.personal.ledger.util.Money
import com.personal.ledger.viewmodel.AddTransactionViewModel
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
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
    var showAccountPicker by remember { mutableStateOf(false) }
    var pickTarget by remember { mutableStateOf(false) }
    var showMerchantPicker by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showNoteEditor by remember { mutableStateOf(false) }
    var expandedRootId by remember(state.type) { mutableStateOf<String?>(null) }
    val pagerScope = rememberCoroutineScope()
    val hapticSave = rememberHapticSave()
    val fontScale = LocalDensity.current.fontScale
    val density = LocalDensity.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val keyboardKeyHeight = if (screenHeightDp < 720 || fontScale > 1.3f) 48.dp else 52.dp
    val anyEditor = showDatePicker || showAccountPicker ||
        showMerchantPicker || showTagPicker || showNoteEditor
    val systemImeVisible = WindowInsets.ime.getBottom(density) > 0
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
            AnimatedVisibility(
                visible = !anyEditor && !systemImeVisible,
                modifier = Modifier.fillMaxWidth(),
                enter = expandVertically(animationSpec = tween(200)) + fadeIn(tween(180)),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(tween(180)),
            ) {
                AmountKeyboard(
                    saving = state.isSaving || state.isResolvingMerchant,
                    keyHeight = keyboardKeyHeight,
                    onKey = { vm.setAmountText(AmountEntry.press(state.amountText, it)) },
                    onSave = {
                        if (!vm.uiState.value.isSaving && !vm.uiState.value.saved &&
                            !vm.uiState.value.isResolvingMerchant
                        ) {
                            hapticSave()
                            vm.save()
                        }
                    },
                )
            }
        },
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
          val categoryRows = if (maxHeight < 560.dp || fontScale > 1.2f) 2 else 3
          val categoryColumns = if (maxWidth < 360.dp || fontScale > 1.35f) 4 else 5
          val compactCategory = maxHeight < 390.dp || fontScale > 1.4f
          Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp).padding(top = 4.dp),
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
                val pageSize = categoryRows * categoryColumns
                val pageCount = maxOf(1, (roots.size + pageSize - 1) / pageSize)
                val pagerState = rememberPagerState(pageCount = { pageCount })
                val childGridState = rememberLazyGridState()
                val shownPage = pagerState.currentPage.coerceIn(0, pageCount - 1)
                LaunchedEffect(state.type) { pagerState.scrollToPage(0) }
                val categoryGridHeight = (if (compactCategory) 58.dp else 64.dp) * categoryRows +
                    4.dp * (categoryRows - 1)
                val selectedRoot = categories.firstOrNull { it.id == expandedRootId }
                LaunchedEffect(categories, state.categoryId, expandedRootId) {
                    if (expandedRootId == null && state.categoryId != null) {
                        val selected = categories.firstOrNull { it.id == state.categoryId }
                        if (selected?.parentId != null) expandedRootId = selected.parentId
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (expandedRootId != null && selectedRoot != null) {
                        SelectedRootChip(selectedRoot) {
                            expandedRootId = null
                            vm.setCategoryId(null)
                        }
                    } else {
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
                }
                AnimatedContent(
                    targetState = expandedRootId,
                    modifier = Modifier.fillMaxWidth().height(categoryGridHeight),
                    transitionSpec = {
                        (fadeIn(tween(190)) + slideInVertically(tween(190)) { it / 10 }) togetherWith
                            (fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it / 10 })
                    },
                    label = "category-level",
                ) { rootId ->
                    if (rootId == null) {
                        if (roots.isEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("暂无可用分类", style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { navController.navigate(Routes.CATEGORIES) }) {
                                    Text("前往分类管理")
                                }
                            }
                        } else {
                            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    roots.drop(pageIndex * pageSize).take(pageSize).chunked(categoryColumns).forEach { row ->
                                        Row(modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            row.forEach { category ->
                                                CategoryTile(category, selectedRootId == category.id, compactCategory,
                                                    Modifier.weight(1f)) {
                                                    val activeChildren = CategorySelection.children(categories, category.id)
                                                    val selectedArchivedChild = categories.any {
                                                        it.parentId == category.id && it.id == state.categoryId && it.isArchived
                                                    }
                                                    if (activeChildren.isNotEmpty() || selectedArchivedChild) {
                                                        vm.setCategoryId(category.id)
                                                        expandedRootId = category.id
                                                    } else {
                                                        vm.setCategoryId(if (state.categoryId == category.id) null else category.id)
                                                    }
                                                }
                                            }
                                            repeat(categoryColumns - row.size) { Spacer(Modifier.weight(1f)) }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val root = categories.firstOrNull { it.id == rootId }
                        if (root == null) {
                            Box(Modifier.fillMaxSize())
                        } else {
                            val archivedSelectedChild = categories.firstOrNull {
                                it.id == state.categoryId && it.parentId == root.id && it.isArchived
                            }
                            val childItems = (CategorySelection.children(categories, root.id) +
                                listOfNotNull(archivedSelectedChild)).distinctBy { it.id }
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(categoryColumns),
                                state = childGridState,
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                item(key = "direct-${root.id}", contentType = "category") {
                                    CategoryTile(root, state.categoryId == root.id, compactCategory,
                                        Modifier.fillMaxWidth()) { vm.setCategoryId(root.id) }
                                }
                                items(childItems, key = { it.id }, contentType = { "category" }) { child ->
                                    CategoryTile(child, state.categoryId == child.id, compactCategory,
                                        Modifier.fillMaxWidth()) { vm.setCategoryId(child.id) }
                                }
                            }
                        }
                    }
                }
            }

            if (state.type != TransactionType.TRANSFER) {
                val selectedTagText = remember(state.tags, state.selectedTagIds) {
                    state.tags.filter { it.id in state.selectedTagIds }.joinToString("、") { it.name }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EntryField("账户",
                        state.accounts.firstOrNull { it.id == state.accountId }?.name ?: "选择",
                        Modifier.weight(1f)) {
                        pickTarget = false; showAccountPicker = true
                    }
                    EntryField("日期", dateLabel(state.time), Modifier.weight(1f)) { showDatePicker = true }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EntryField("商家",
                        state.merchants.firstOrNull { it.id == state.merchantId }?.name ?: "选择",
                        Modifier.weight(1f)) { showMerchantPicker = true }
                    EntryField("备注", state.note.ifEmpty { "添加" },
                        Modifier.weight(1f)) { showNoteEditor = true }
                    EntryField("标签",
                        selectedTagText.ifEmpty { "选择" },
                        Modifier.weight(1f)) { showTagPicker = true }
                }
            } else {
                val selectedTagText = remember(state.tags, state.selectedTagIds) {
                    state.tags.filter { it.id in state.selectedTagIds }.joinToString("、") { it.name }
                }
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
                    EntryField("日期", dateLabel(state.time), Modifier.weight(1f)) { showDatePicker = true }
                    EntryField("商家",
                        state.merchants.firstOrNull { it.id == state.merchantId }?.name ?: "选择",
                        Modifier.weight(1f)) { showMerchantPicker = true }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EntryField("备注", state.note.ifEmpty { "添加" },
                        Modifier.weight(1f)) { showNoteEditor = true }
                    EntryField("标签",
                        selectedTagText.ifEmpty { "选择" },
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

    if (showDatePicker) {
        DateWheelSheet(
            initialTime = state.time,
            onDismiss = { showDatePicker = false },
            onConfirm = { time -> vm.setTime(time); showDatePicker = false },
        )
    }

    if (showAccountPicker) {
        val otherAccountId = if (pickTarget) state.accountId else state.targetAccountId
        EntryPickerSheet(
            title = if (state.type == TransactionType.TRANSFER)
                if (pickTarget) "选择转入账户" else "选择转出账户" else "选择账户",
            subtitle = if (state.type == TransactionType.TRANSFER) "转出与转入账户必须不同" else null,
            options = state.accounts.filter { state.type != TransactionType.TRANSFER || it.id != otherAccountId }
                .map { account ->
                    EntryPickerOption(
                        id = account.id,
                        title = account.name,
                        detail = "${accountTypeLabel(account.type)} · ${account.currency}",
                        emoji = AccountVisuals.emojiFor(account.type),
                        selected = account.id == if (pickTarget) state.targetAccountId else state.accountId,
                    )
                },
            onPick = { id ->
                if (pickTarget) vm.setTargetAccountId(id) else vm.setAccountId(id)
                showAccountPicker = false
            },
            onDismiss = { showAccountPicker = false },
        )
    }

    if (showMerchantPicker) {
        val categories = state.expenseCategories + state.incomeCategories
        val rootId = CategorySelection.merchantRootId(categories, state.categoryId)
        val root = categories.firstOrNull { it.id == rootId }
        val oldSelected = state.merchants.firstOrNull { it.id == state.merchantId }
        val options = if (state.type == TransactionType.TRANSFER)
            state.merchants.filter { !it.isArchived } else state.merchantOptions
        val merchantItems = (options + listOfNotNull(oldSelected)
            .filter { old -> options.none { it.id == old.id } }).distinctBy { it.id }
        EntryPickerSheet(
            title = if (root != null) "${root.name}的商家" else "选择商家",
            subtitle = when {
                state.type == TransactionType.TRANSFER -> "当前账本商家库"
                root != null && state.categoryId != root.id ->
                    "${CategorySelection.label(categories, state.categoryId)} · ${root.name}商家库"
                root != null -> "${root.name}商家库"
                else -> "按一级分类关联商家"
            },
            options = merchantItems.map { merchant ->
                EntryPickerOption(merchant.id, merchant.name, emoji = "🏪",
                    selected = merchant.id == state.merchantId)
            },
            hint = if (state.type != TransactionType.TRANSFER && state.categoryId == null)
                "请先选择分类，再选择或新建分类关联商家" else null,
            addLabel = if (state.type == TransactionType.TRANSFER || state.categoryId != null)
                "新建商家" else null,
            clearLabel = "不选择商家",
            onClear = {
                vm.setMerchantId(null)
                showMerchantPicker = false
            },
            onPick = { id ->
                vm.setMerchantId(id)
                showMerchantPicker = false
            },
            onAdd = { name, done -> vm.addMerchant(name, done) },
            onDismiss = { showMerchantPicker = false },
            closeOnAddSuccess = true,
        )
    }

    if (showTagPicker) {
        EntryPickerSheet(
            title = "选择标签",
            subtitle = "可多选，完成后保存到账单",
            options = state.tags.map { tag ->
                EntryPickerOption(tag.id, tag.name, emoji = "🏷️", selected = tag.id in state.selectedTagIds)
            },
            onPick = {},
            multiSelect = true,
            onDone = { ids -> vm.setSelectedTagIds(ids); showTagPicker = false },
            addLabel = "新建标签",
            onAdd = { name, done -> vm.addTag(name, done) },
            onDismiss = { showTagPicker = false },
        )
    }
    if (showNoteEditor) {
        NoteEditorSheet(
            initialNote = state.note,
            onDismiss = { showNoteEditor = false },
            onConfirm = { note -> vm.setNote(note); showNoteEditor = false },
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

private fun dateLabel(ms: Long): String {
    val date = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
    return "%04d-%02d-%02d".format(date.year, date.monthValue, date.dayOfMonth)
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
        modifier = modifier.heightIn(min = if (compact) 58.dp else 64.dp)
            .selectable(selected = selected, onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            CategoryBadge(category.name, category.type,
                sizeDp = if (compact) 36 else 42, visualKey = category.id)
            Text(category.name, style = MaterialTheme.typography.labelMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CategoryChoiceChip(
    label: String,
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.heightIn(min = 44.dp).selectable(selected = selected, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryBadge(category.name, category.type, sizeDp = 24, visualKey = category.id)
            Text(label, modifier = Modifier.padding(start = 6.dp).widthIn(max = 180.dp),
                style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SelectedRootChip(category: Category, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryBadge(category.name, category.type, sizeDp = 32, visualKey = category.id)
            Column(Modifier.weight(1f).padding(start = 9.dp)) {
                Text(category.name, style = MaterialTheme.typography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("点击一级返回", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回一级分类",
                modifier = Modifier.size(20.dp))
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
private fun AmountKeyboard(
    saving: Boolean,
    keyHeight: androidx.compose.ui.unit.Dp,
    onKey: (String) -> Unit,
    onSave: () -> Unit,
) {
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
                                modifier = Modifier.weight(1f).height(keyHeight).clickable { onKey(key) },
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
                    modifier = Modifier.fillMaxWidth().height(keyHeight)) { Text("清空") }
                Button(onClick = onSave, enabled = !saving,
                    modifier = Modifier.fillMaxWidth().height(keyHeight * 3f + 6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp)) {
                    Text(if (saving) "保存中…" else "保存",
                        style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
