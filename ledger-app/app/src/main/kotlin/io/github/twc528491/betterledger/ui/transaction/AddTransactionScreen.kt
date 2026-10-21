package io.github.twc528491.betterledger.ui.transaction

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import io.github.twc528491.betterledger.R
import io.github.twc528491.betterledger.data.AppContainer
import io.github.twc528491.betterledger.domain.model.Category
import io.github.twc528491.betterledger.domain.model.TransactionType
import io.github.twc528491.betterledger.navigation.Routes
import io.github.twc528491.betterledger.ui.common.AccountBadge
import io.github.twc528491.betterledger.ui.common.CategoryBadge
import io.github.twc528491.betterledger.ui.common.accountTypeLabel
import io.github.twc528491.betterledger.ui.common.amountColor
import io.github.twc528491.betterledger.ui.common.cardColor
import io.github.twc528491.betterledger.ui.common.expenseContainerColor
import io.github.twc528491.betterledger.ui.common.incomeContainerColor
import io.github.twc528491.betterledger.ui.common.rememberHapticSave
import io.github.twc528491.betterledger.ui.common.transferContainerColor
import io.github.twc528491.betterledger.ui.theme.LedgerNumerals
import io.github.twc528491.betterledger.util.Money
import io.github.twc528491.betterledger.util.asString
import io.github.twc528491.betterledger.util.userMessage
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.twc528491.betterledger.viewmodel.AddTransactionViewModel
import io.github.twc528491.betterledger.ui.common.rememberHapticConfirm
import io.github.twc528491.betterledger.ui.common.rememberHapticLongPress
import io.github.twc528491.betterledger.ui.common.rememberHapticReject
import io.github.twc528491.betterledger.ui.settings.CategoryIconSheet
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.key
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
    var showAccountPicker by remember { mutableStateOf(false) }
    var pickTarget by remember { mutableStateOf(false) }
    var showMerchantPicker by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showNoteEditor by remember { mutableStateOf(false) }
    var expandedRootId by remember(state.type) { mutableStateOf<String?>(null) }
    var initialExpandDone by remember(state.type) { mutableStateOf(false) }
    // 记一笔中新建二级分类：长按一级分类弹出的菜单、新建面板所属的一级分类、创建后需滚动到的新分类。
    var rootMenuId by remember(state.type) { mutableStateOf<String?>(null) }
    var addChildRootId by remember { mutableStateOf<String?>(null) }
    var addChildError by remember { mutableStateOf<String?>(null) }
    var addChildBusy by remember { mutableStateOf(false) }
    var scrollToChildId by remember { mutableStateOf<String?>(null) }
    val pagerScope = rememberCoroutineScope()
    val hapticSave = rememberHapticSave()
    val hapticLongPress = rememberHapticLongPress()
    val hapticConfirm = rememberHapticConfirm()
    val hapticReject = rememberHapticReject()
    val context = LocalContext.current
    val fontScale = LocalDensity.current.fontScale
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    // 键盘尺寸只取决于屏幕与字号，与弹层 / 输入法无关，整页布局因此保持稳定。
    val keyboardKeyHeight = if (screenHeightDp < 720 || fontScale > 1.3f) 48.dp else 52.dp
    LaunchedEffect(txId) { vm.load(txId) }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            navController.popBackStack()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    TypeSwitcher(selected = state.type, onSelect = vm::setType)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            // 金额键盘常驻：选择面板与系统输入法都以覆盖层出现，不再收起 / 展开键盘，
            // 避免正文可用高度瞬间变化导致分类网格行数跳变与闪烁。
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
        },
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
          val categoryRows = if (maxHeight < 560.dp || fontScale > 1.2f) 2 else 3
          val categoryColumns = if (maxWidth < 360.dp || fontScale > 1.35f) 4 else 5
          val compactCategory = maxHeight < 390.dp || fontScale > 1.4f
          val allCategories = state.expenseCategories + state.incomeCategories
          Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            AmountPanel(
                type = state.type,
                amountText = state.amountText,
                currency = state.baseCurrency,
                editing = state.editing != null,
                category = allCategories.firstOrNull { it.id == state.categoryId },
                categoryLabel = CategorySelection.label(allCategories, state.categoryId)
                    ?: stringResource(R.string.add_choose_root_category),
            )

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
                val tileBadge = if (compactCategory) 38 else 42
                val labelLineHeight = with(LocalDensity.current) {
                    MaterialTheme.typography.labelMedium.lineHeight.toDp()
                }
                // 圆标（含 6dp 选中描边）+ 名称行高 + 上下留白，避免最底行文字被裁切。
                val tileHeight = (tileBadge + 6).dp + labelLineHeight + 14.dp
                val categoryGridHeight = tileHeight * categoryRows + 4.dp * (categoryRows - 1)
                val selectedRoot = categories.firstOrNull { it.id == expandedRootId }
                // 仅在分类数据首次就绪时（如编辑已选子类的旧账单）自动展开一次；
                // 之后用户点“返回一级分类”不得被再次展开。
                LaunchedEffect(categories) {
                    if (!initialExpandDone && categories.isNotEmpty()) {
                        initialExpandDone = true
                        val selected = categories.firstOrNull { it.id == state.categoryId }
                        if (expandedRootId == null && selected?.parentId != null) expandedRootId = selected.parentId
                    }
                }
                Surface(shape = MaterialTheme.shapes.large, color = cardColor()) {
                  Column(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (expandedRootId != null && selectedRoot != null) {
                            IconButton(
                                onClick = { expandedRootId = null },
                                modifier = Modifier.size(44.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.add_back_to_root_categories))
                            }
                            Text(stringResource(R.string.add_root_categories), style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("/", modifier = Modifier.padding(horizontal = 6.dp),
                                color = MaterialTheme.colorScheme.outline)
                            Text(selectedRoot.name, modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                        } else {
                            Text(stringResource(R.string.add_category), style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(start = 8.dp).weight(1f))
                            if (pageCount > 1) {
                                IconButton(
                                    onClick = { if (shownPage > 0) pagerScope.launch {
                                        pagerState.animateScrollToPage(shownPage - 1)
                                    } },
                                    enabled = shownPage > 0,
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = stringResource(R.string.add_previous_category_page))
                                }
                                PageDots(count = pageCount, current = shownPage)
                                IconButton(
                                    onClick = { if (shownPage + 1 < pageCount) pagerScope.launch {
                                        pagerState.animateScrollToPage(shownPage + 1)
                                    } },
                                    enabled = shownPage + 1 < pageCount,
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = stringResource(R.string.add_next_category_page))
                                }
                            }
                        }
                    }
                    AnimatedContent(
                        targetState = expandedRootId,
                        modifier = Modifier.fillMaxWidth().height(categoryGridHeight),
                        transitionSpec = {
                            (fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 12 }) togetherWith
                                (fadeOut(tween(140)) + slideOutVertically(tween(140)) { -it / 12 })
                        },
                        label = "category-level",
                    ) { rootId ->
                        if (rootId == null) {
                            if (roots.isEmpty()) {
                                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(stringResource(R.string.add_no_categories), style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextButton(onClick = { navController.navigate(Routes.CATEGORIES) }) {
                                        Text(stringResource(R.string.add_manage_categories))
                                    }
                                }
                            } else {
                                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
                                    Column(Modifier.selectableGroup(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        roots.drop(pageIndex * pageSize).take(pageSize).chunked(categoryColumns).forEach { row ->
                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                row.forEach { category ->
                                                    CategoryTile(category, selectedRootId == category.id, compactCategory,
                                                        Modifier.weight(1f).height(tileHeight),
                                                        onLongClick = {
                                                            hapticLongPress()
                                                            rootMenuId = category.id
                                                        },
                                                        menu = {
                                                            DropdownMenu(
                                                                expanded = rootMenuId == category.id,
                                                                onDismissRequest = { rootMenuId = null },
                                                            ) {
                                                                DropdownMenuItem(
                                                                    text = { Text(stringResource(R.string.add_add_subcategory)) },
                                                                    leadingIcon = {
                                                                        Icon(Icons.Filled.Add, contentDescription = null)
                                                                    },
                                                                    onClick = {
                                                                        rootMenuId = null
                                                                        addChildError = null
                                                                        addChildRootId = category.id
                                                                    },
                                                                )
                                                            }
                                                        },
                                                    ) {
                                                        val activeChildren = CategorySelection.children(categories, category.id)
                                                        val selectedArchivedChild = categories.any {
                                                            it.parentId == category.id && it.id == state.categoryId && it.isArchived
                                                        }
                                                        if (activeChildren.isNotEmpty() || selectedArchivedChild) {
                                                            expandedRootId = category.id
                                                            // 默认“直接记入”一级分类；已选该一级下的子类时保留原选择。
                                                            if (selectedRootId != category.id) vm.setCategoryId(category.id)
                                                        } else {
                                                            vm.setCategoryId(category.id)
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
                                    modifier = Modifier.fillMaxSize().selectableGroup(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    item(key = "direct-${root.id}", contentType = "category") {
                                        CategoryTile(root, state.categoryId == root.id, compactCategory,
                                            Modifier.fillMaxWidth().height(tileHeight),
                                            labelOverride = stringResource(R.string.add_direct_root)) { vm.setCategoryId(root.id) }
                                    }
                                    items(childItems, key = { it.id }, contentType = { "category" }) { child ->
                                        CategoryTile(child, state.categoryId == child.id, compactCategory,
                                            Modifier.fillMaxWidth().height(tileHeight)) { vm.setCategoryId(child.id) }
                                    }
                                    if (!root.isArchived) {
                                        item(key = "add-${root.id}", contentType = "category-add") {
                                            AddCategoryTile(compactCategory, Modifier.fillMaxWidth().height(tileHeight)) {
                                                addChildError = null
                                                addChildRootId = root.id
                                            }
                                        }
                                    }
                                }
                                // 新建的二级分类排在末尾，可能落在可见行之外：创建后滚动到它。
                                LaunchedEffect(scrollToChildId, childItems) {
                                    val index = childItems.indexOfFirst { it.id == scrollToChildId }
                                    if (index >= 0) {
                                        childGridState.animateScrollToItem(index + 1)
                                        scrollToChildId = null
                                    }
                                }
                            }
                        }
                    }
                  }
                }
            }

            val tagSeparator = stringResource(R.string.add_tag_separator)
            val selectedTagText = remember(state.tags, state.selectedTagIds, tagSeparator) {
                state.tags.filter { it.id in state.selectedTagIds }.joinToString(tagSeparator) { it.name }
            }
            val accountName = state.accounts.firstOrNull { it.id == state.accountId }?.name
            val merchantName = state.merchants.firstOrNull { it.id == state.merchantId }?.name
            if (state.type != TransactionType.TRANSFER) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EntryField(stringResource(R.string.add_field_account), accountName, stringResource(R.string.add_placeholder_choose), Modifier.weight(1f)) {
                        pickTarget = false; showAccountPicker = true
                    }
                    EntryField(stringResource(R.string.add_field_date), dateLabel(state.time), stringResource(R.string.add_placeholder_choose), Modifier.weight(1f)) {
                        showDatePicker = true
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EntryField(stringResource(R.string.add_field_merchant), merchantName, stringResource(R.string.add_placeholder_optional), Modifier.weight(1f)) { showMerchantPicker = true }
                    EntryField(stringResource(R.string.add_field_note), state.note.ifEmpty { null }, stringResource(R.string.add_placeholder_optional), Modifier.weight(1f)) {
                        showNoteEditor = true
                    }
                    EntryField(stringResource(R.string.add_field_tags), selectedTagText.ifEmpty { null }, stringResource(R.string.add_placeholder_optional), Modifier.weight(1f)) {
                        showTagPicker = true
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EntryField(stringResource(R.string.add_field_from_account), accountName, stringResource(R.string.add_placeholder_choose), Modifier.weight(1f)) {
                        pickTarget = false; showAccountPicker = true
                    }
                    EntryField(stringResource(R.string.add_field_to_account),
                        state.accounts.firstOrNull { it.id == state.targetAccountId }?.name,
                        stringResource(R.string.add_placeholder_choose),
                        Modifier.weight(1f)) {
                        pickTarget = true; showAccountPicker = true
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EntryField(stringResource(R.string.add_field_date), dateLabel(state.time), stringResource(R.string.add_placeholder_choose), Modifier.weight(1f)) {
                        showDatePicker = true
                    }
                    EntryField(stringResource(R.string.add_field_note), state.note.ifEmpty { null }, stringResource(R.string.add_placeholder_optional), Modifier.weight(1f)) {
                        showNoteEditor = true
                    }
                    EntryField(stringResource(R.string.add_field_tags), selectedTagText.ifEmpty { null }, stringResource(R.string.add_placeholder_optional), Modifier.weight(1f)) {
                        showTagPicker = true
                    }
                }
            }
            state.merchantNotice?.let {
                Text(it.asString(), color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium)
            }
            state.error?.let {
                Text(it.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
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
                stringResource(if (pickTarget) R.string.add_choose_to_account else R.string.add_choose_from_account)
            else stringResource(R.string.add_choose_account),
            subtitle = if (state.type == TransactionType.TRANSFER)
                stringResource(R.string.add_accounts_must_differ) else null,
            options = state.accounts.filter { state.type != TransactionType.TRANSFER || it.id != otherAccountId }
                .map { account ->
                    EntryPickerOption(
                        id = account.id,
                        title = account.name,
                        detail = "${accountTypeLabel(account.type)} · ${account.currency}",
                        leading = { AccountBadge(account.type, sizeDp = 36) },
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

    if (showMerchantPicker && state.type != TransactionType.TRANSFER) {
        val rootId = CategorySelection.merchantRootId(allCategoriesOf(state), state.categoryId)
        val root = allCategoriesOf(state).firstOrNull { it.id == rootId }
        val oldSelected = state.merchants.firstOrNull { it.id == state.merchantId }
        val options = state.merchantOptions
        val merchantItems = remember(options, oldSelected, state.merchantId) {
            MerchantChoices.distinctByName(
                listOfNotNull(oldSelected).filter { old -> options.none { it.id == old.id } } + options,
                state.merchantId,
            )
        }
        EntryPickerSheet(
            title = stringResource(R.string.add_choose_merchant),
            subtitle = root?.let { stringResource(R.string.add_merchants_in_category, it.name) },
            options = merchantItems.map { merchant ->
                EntryPickerOption(merchant.id, merchant.name, emoji = merchant.name.trim().take(1),
                    selected = merchant.id == state.merchantId)
            },
            hint = if (state.categoryId == null) stringResource(R.string.add_merchant_hint_choose_category) else null,
            addLabel = if (state.categoryId != null) stringResource(R.string.add_new) else null,
            clearLabel = stringResource(R.string.add_no_merchant),
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
            title = stringResource(R.string.add_choose_tags),
            subtitle = stringResource(R.string.add_tags_hint),
            options = state.tags.map { tag ->
                EntryPickerOption(tag.id, tag.name, emoji = "#", selected = tag.id in state.selectedTagIds)
            },
            onPick = {},
            multiSelect = true,
            onDone = { ids -> vm.setSelectedTagIds(ids); showTagPicker = false },
            addLabel = stringResource(R.string.add_new),
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

    val addChildRoot = addChildRootId?.let { id -> allCategoriesOf(state).firstOrNull { it.id == id } }
    if (addChildRoot != null) {
        key(addChildRoot.id) {
            // 复用分类管理中的新建面板：名称 + 图标库（默认按名称自动匹配图标）。
            CategoryIconSheet(
                title = stringResource(R.string.add_new_subcategory),
                subtitle = stringResource(R.string.add_subcategory_parent, addChildRoot.name),
                type = addChildRoot.type,
                initialName = "",
                initialIconKey = null,
                nameEditable = true,
                confirmLabel = stringResource(R.string.add_create),
                error = addChildError,
                onDismiss = {
                    addChildRootId = null
                    addChildError = null
                },
                onConfirm = { name, iconKey ->
                    if (!addChildBusy) {
                        addChildBusy = true
                        vm.addSubcategory(addChildRoot.id, name, iconKey) { result ->
                            addChildBusy = false
                            result.onSuccess { created ->
                                hapticConfirm()
                                addChildRootId = null
                                addChildError = null
                                // 展开到该一级分类的二级网格，新分类已被选中。
                                expandedRootId = addChildRoot.id
                                scrollToChildId = created.id
                            }.onFailure {
                                hapticReject()
                                addChildError = it.userMessage(context, R.string.add_error_create_failed)
                            }
                        }
                    }
                },
            )
        }
    }
}

private fun allCategoriesOf(state: io.github.twc528491.betterledger.viewmodel.AddTransactionUiState): List<Category> =
    state.expenseCategories + state.incomeCategories

@Composable
private fun typeLabel(t: TransactionType): String = stringResource(
    when (t) {
        TransactionType.EXPENSE -> R.string.type_expense
        TransactionType.INCOME -> R.string.type_income
        TransactionType.TRANSFER -> R.string.type_transfer
    },
)

@Composable
private fun editingTypeLabel(t: TransactionType): String = stringResource(
    when (t) {
        TransactionType.EXPENSE -> R.string.add_editing_expense
        TransactionType.INCOME -> R.string.add_editing_income
        TransactionType.TRANSFER -> R.string.add_editing_transfer
    },
)

private fun dateLabel(ms: Long): String {
    val date = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
    return "%04d-%02d-%02d".format(date.year, date.monthValue, date.dayOfMonth)
}

/** 顶栏内的类型切换：胶囊分段，选中项为白底浮起块。 */
@Composable
private fun TypeSwitcher(selected: TransactionType, onSelect: (TransactionType) -> Unit) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
            .padding(3.dp)
            .selectableGroup(),
    ) {
        TransactionType.entries.forEach { type ->
            val isSelected = type == selected
            val container by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent,
                animationSpec = tween(160), label = "type-bg",
            )
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .widthIn(min = 64.dp)
                    .background(container, CircleShape)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(type) })
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    typeLabel(type),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) amountColor(type) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 金额面板：左侧显示当前分类（或转账），右侧大号金额；底色随收支类型变化。 */
@Composable
private fun AmountPanel(
    type: TransactionType,
    amountText: String,
    currency: String,
    editing: Boolean,
    category: Category?,
    categoryLabel: String,
) {
    val background by animateColorAsState(
        when (type) {
            TransactionType.EXPENSE -> expenseContainerColor()
            TransactionType.INCOME -> incomeContainerColor()
            TransactionType.TRANSFER -> transferContainerColor()
        },
        animationSpec = tween(200), label = "amount-bg",
    )
    val accent = amountColor(type)
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp)
            .background(background, MaterialTheme.shapes.large)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            type == TransactionType.TRANSFER -> Box(
                Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp)) }
            category != null -> CategoryBadge(category.name, category.type, sizeDp = 40,
                visualKey = category.id, iconKey = category.iconKey)
            else -> Box(
                Modifier.size(40.dp).border(1.5.dp, accent.copy(alpha = 0.35f), CircleShape),
            )
        }
        Column(Modifier.padding(start = 10.dp).weight(1f)) {
            Text(
                if (editing) editingTypeLabel(type) else typeLabel(type),
                style = MaterialTheme.typography.labelMedium,
                color = accent,
            )
            Text(
                if (type == TransactionType.TRANSFER) stringResource(R.string.add_between_accounts) else categoryLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        val amount = AmountEntry.fromStored(amountText).ifEmpty { "0.00" }
        Row(
            modifier = Modifier.padding(start = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(Money.symbolFor(currency),
                style = MaterialTheme.typography.titleMedium, color = accent,
                modifier = Modifier.padding(bottom = 4.dp, end = 3.dp))
            Text(
                amount,
                color = accent,
                style = LedgerNumerals.of(MaterialTheme.typography.headlineMedium),
                fontSize = if (amount.length > 12) 22.sp else 30.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { index ->
            Box(
                Modifier.size(width = if (index == current) 14.dp else 6.dp, height = 6.dp)
                    .background(
                        if (index == current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape,
                    ),
            )
        }
    }
}

/**
 * 分类格：无卡片底，只保留圆标与名称；选中时圆标加主色描边、名称变主色。
 * 传入 [onLongClick] 时支持长按（一级分类长按菜单），[menu] 为锚定在该格上的弹出菜单。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryTile(
    category: Category,
    selected: Boolean,
    compact: Boolean,
    modifier: Modifier,
    labelOverride: String? = null,
    onLongClick: (() -> Unit)? = null,
    menu: @Composable () -> Unit = {},
    onClick: () -> Unit,
) {
    val badge = if (compact) 38 else 42
    val interaction = if (onLongClick == null) {
        Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    } else {
        Modifier
            .combinedClickable(
                role = Role.RadioButton,
                onLongClickLabel = stringResource(R.string.add_more_actions),
                onLongClick = onLongClick,
                hapticFeedbackEnabled = false,
                onClick = onClick,
            )
            .semantics { this.selected = selected }
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(interaction)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        menu()
        Box(
            Modifier.size((badge + 6).dp)
                .border(
                    width = 2.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            CategoryBadge(category.name, category.type, sizeDp = badge, visualKey = category.id,
                iconKey = category.iconKey)
        }
        Text(
            labelOverride ?: category.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** 二级网格末尾的“新建”格：虚线圆 + 加号，尺寸与分类格一致，不打乱网格行高。 */
@Composable
private fun AddCategoryTile(compact: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val badge = if (compact) 38 else 42
    val outline = MaterialTheme.colorScheme.outline
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.add_new_subcategory), onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size((badge + 6).dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(badge.dp).drawBehind {
                    val stroke = 1.5.dp.toPx()
                    drawCircle(
                        color = outline,
                        radius = (size.minDimension - stroke) / 2f,
                        style = Stroke(
                            width = stroke,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                        ),
                    )
                },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size((badge * 0.5f).dp),
                )
            }
        }
        Text(
            stringResource(R.string.add_new),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** 字段格：小标签 + 值；未填写时以弱化占位显示。 */
@Composable
private fun EntryField(
    label: String,
    value: String?,
    placeholder: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(cardColor())
            .clickable(onClick = onClick, role = Role.Button, onClickLabel = stringResource(R.string.add_field_click_label, label))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            value ?: placeholder,
            style = LedgerNumerals.of(MaterialTheme.typography.bodyMedium),
            color = if (value == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (value == null) FontWeight.Normal else FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AmountKeyboard(
    saving: Boolean,
    keyHeight: Dp,
    onKey: (String) -> Unit,
    onSave: () -> Unit,
) {
    val gap = 6.dp
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            Column(modifier = Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(gap)) {
                listOf(
                    listOf("1", "2", "3"), listOf("4", "5", "6"),
                    listOf("7", "8", "9"), listOf(".", "0", AmountEntry.BACKSPACE),
                ).forEach { keys ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        keys.forEach { key ->
                            KeyCap(
                                modifier = Modifier.weight(1f).height(keyHeight),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                onClick = { onKey(key) },
                            ) {
                                if (key == AmountEntry.BACKSPACE) {
                                    Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = stringResource(R.string.add_key_backspace),
                                        modifier = Modifier.size(22.dp))
                                } else {
                                    Text(key, style = LedgerNumerals.of(MaterialTheme.typography.titleLarge),
                                        fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(gap)) {
                KeyCap(
                    modifier = Modifier.fillMaxWidth().height(keyHeight),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    onClick = { onKey(AmountEntry.CLEAR) },
                ) {
                    Text(stringResource(R.string.add_key_clear), style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                KeyCap(
                    modifier = Modifier.fillMaxWidth().height(keyHeight * 3 + gap * 2),
                    color = if (saving) MaterialTheme.colorScheme.surfaceContainerHighest
                        else MaterialTheme.colorScheme.primary,
                    enabled = !saving,
                    onClick = onSave,
                ) {
                    Text(stringResource(if (saving) R.string.add_saving else R.string.action_save),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = if (saving) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun KeyCap(
    modifier: Modifier,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = color,
        modifier = modifier,
    ) {
        Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) { content() }
    }
}
