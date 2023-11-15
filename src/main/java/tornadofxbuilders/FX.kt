@file:Suppress("unused")
package tornadofxbuilders

import javafx.beans.property.ListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.collections.ObservableSet
import javafx.event.EventTarget
import javafx.scene.*
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import java.lang.ref.WeakReference
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

fun <T> weak(referent: T, deinit: () -> Unit = {}): WeakDelegate<T> = WeakDelegate(referent, deinit)

class WeakDelegate<T>(referent: T, deinit: () -> Unit = {}) : ReadOnlyProperty<Any, DeregisteringWeakReference<T>> {
    private val weakRef = DeregisteringWeakReference(referent, deinit)
    override fun getValue(thisRef: Any, property: KProperty<*>) = weakRef
}

class DeregisteringWeakReference<T>(referent: T, val deinit: () -> Unit = {}) : WeakReference<T>(referent) {
    fun ifActive(op: T.() -> Unit) {
        val ref = get()
        if (ref != null) op(ref) else deinit()
    }
}

/**
 * Add the given node to the pane, invoke the node operation and return the node. The `opcr` name
 * is an acronym for "op connect & return".
 */
inline fun <T : Node> opcr(parent: EventTarget, node: T, op: T.() -> Unit = {}) = node.apply {
    parent.addChildIfPossible(this)
    op(this)
}

/**
 * Attaches the node to the pane and invokes the node operation.
 */
inline fun <T : Node> T.attachTo(parent: EventTarget, op: T.() -> Unit = {}): T = opcr(parent, this, op)

/**
 * Attaches the node to the pane and invokes the node operation.
 * Because the framework sometimes needs to setup the node, another lambda can be provided
 */
inline fun <T : Node> T.attachTo(
    parent: EventTarget,
    after: T.() -> Unit,
    before: (T) -> Unit
) = this.also(before).attachTo(parent, after)

@Suppress("UNNECESSARY_SAFE_CALL")
fun EventTarget.addChildIfPossible(node: Node, index: Int? = null) {
    if (FX.ignoreParentBuilder != FX.IgnoreParentBuilder.No) return
    if (this is Node) {
        val target = builderTarget
        if (target != null) {
            // Trick to get around the disallowed use of invoke on out projected types
            @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
            target!!(this).value = node
            return
        }
    }
    when (this) {
        is SubScene -> {
            root = node as Parent
        }
        is ScrollPane -> content = node
        is ButtonBase -> {
            graphic = node
        }
        is BorderPane -> {
        } // Either pos = builder { or caught by builderTarget above
        is TitledPane -> {
            if (content is Pane) {
                content.addChildIfPossible(node, index)
            } else if (content is Node) {
                val container = VBox()
                container.children.addAll(content, node)
                content = container
            } else {
                content = node
            }
        }
        is CustomMenuItem -> {
            content = node
        }
        is MenuItem -> {
            graphic = node
        }
        else -> getChildList()?.apply {
            if (!contains(node)) {
                if (index != null && index < size)
                    add(index, node)
                else
                    add(node)
            }
        }
    }
}


/**
 * Bind the children of this Layout node to the given observable list of items by converting
 * them into nodes via the given converter function. Changes to the source list will be reflected
 * in the children list of this layout node.
 */
fun <T> EventTarget.bindChildren(sourceList: ObservableList<T>, converter: (T) -> Node): ListConversionListener<T, Node> = requireNotNull(getChildList()?.bind(sourceList, converter)) { "Unable to extract child nodes from $this" }

/**
 * Bind the children of this Layout node to the items of the given ListPropery by converting
 * them into nodes via the given converter function. Changes to the source list and changing the list inside the ListProperty
 * will be reflected in the children list of this layout node.
 */
fun <T> EventTarget.bindChildren(sourceList: ListProperty<T>, converter: (T) -> Node): ListConversionListener<T, Node> = requireNotNull(getChildList()?.bind(sourceList, converter)) { "Unable to extract child nodes from $this" }

/**
 * Bind the children of this Layout node to the given observable set of items
 * by converting them into nodes via the given converter function.
 * Changes to the source set will be reflected in the children list of this layout node.
 */
inline fun <reified T> EventTarget.bindChildren(
    sourceSet: ObservableSet<T>,
    noinline converter: (T) -> Node
): SetConversionListener<T, Node> = requireNotNull(
    getChildList()?.bind(sourceSet, converter)
) { "Unable to extract child nodes from $this" }

inline fun <reified K, reified V> EventTarget.bindChildren(
    sourceMap: ObservableMap<K, V>,
    noinline converter: (K, V) -> Node
): MapConversionListener<K, V, Node> = requireNotNull(
    getChildList()?.bind(sourceMap, converter)
) { "Unable to extract child nodes from $this" }

/**
 * Find the list of children from a Parent node. Gleaned code from ControlsFX for this.
 */
fun EventTarget.getChildList(): MutableList<Node>? = when (this) {
    is SplitPane -> items
    is ToolBar -> items
    is Pane -> children
    is Group -> children
    is HBox -> children
    is VBox -> children
    is Control -> (skin as? SkinBase<*>)?.children ?: getChildrenReflectively()
    is Parent -> getChildrenReflectively()
    else -> null
}

@Suppress("UNCHECKED_CAST", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
private fun Parent.getChildrenReflectively(): MutableList<Node>? = this.javaClass
    .findMethodByName("getChildren")
    ?.takeIf { java.util.List::class.java.isAssignableFrom(it.returnType) }
    ?.takeIf { getter -> getter.canAccess(this) || getter.trySetAccessible() }
    ?.let { getter -> getter.invoke(this) as MutableList<Node> }

class FX {
    enum class IgnoreParentBuilder { No, Once }

    companion object {
        internal val inheritParamHolder = ThreadLocal<Map<String, Any?>>()
        internal var ignoreParentBuilder: IgnoreParentBuilder = IgnoreParentBuilder.No
            get() {
                if (field == IgnoreParentBuilder.Once) {
                    field = IgnoreParentBuilder.No
                    return IgnoreParentBuilder.Once
                }
                return field
            }
    }
}