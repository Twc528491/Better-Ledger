package io.github.twc528491.betterledger.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiSelectionTest {

    @Test fun `long press enters selection mode with that item selected`() {
        val s = MultiSelection.None.longPress("a")
        assertTrue(s.active)
        assertEquals(setOf("a"), s.selected)
        assertEquals(1, s.count)
    }

    @Test fun `long press while selecting toggles like a tap`() {
        val s = MultiSelection.None.longPress("a").longPress("b").longPress("a")
        assertTrue(s.active)
        assertEquals(setOf("b"), s.selected)
    }

    @Test fun `toggling off the last item keeps selection mode`() {
        val s = MultiSelection.None.longPress("a").toggle("a")
        assertTrue(s.active)
        assertEquals(0, s.count)
    }

    @Test fun `toggle all selects every visible id then clears them`() {
        val visible = listOf("a", "b", "c")
        val partial = MultiSelection.None.longPress("b")
        assertFalse(partial.allSelected(visible))

        val all = partial.toggleAll(visible)
        assertTrue(all.allSelected(visible))
        assertEquals(setOf("a", "b", "c"), all.selected)

        val none = all.toggleAll(visible)
        assertTrue(none.active)
        assertTrue(none.selected.isEmpty())
    }

    @Test fun `group toggle only touches that group`() {
        val s = MultiSelection.None.longPress("x").toggleAll(listOf("a", "b"))
        assertEquals(setOf("x", "a", "b"), s.selected)
        assertEquals(setOf("x"), s.toggleAll(listOf("a", "b")).selected)
    }

    @Test fun `empty id list is never all selected`() {
        assertFalse(MultiSelection(active = true).allSelected(emptyList()))
    }

    @Test fun `retain drops ids that left the list and exits when list is empty`() {
        val s = MultiSelection(active = true, selected = setOf("a", "b", "c"))
        assertEquals(setOf("a", "c"), s.retain(listOf("a", "c", "d")).selected)
        assertSame(s, s.retain(listOf("a", "b", "c", "d")))
        assertEquals(MultiSelection.None, s.retain(emptyList()))
        // 非选择模式下不做任何事。
        assertSame(MultiSelection.None, MultiSelection.None.retain(emptyList()))
    }

    @Test fun `selected ids follow list order`() {
        val s = MultiSelection(active = true, selected = setOf("c", "a"))
        assertEquals(listOf("a", "c"), s.selectedIn(listOf("a", "b", "c")))
    }
}
