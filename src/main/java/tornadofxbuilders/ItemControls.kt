@file:Suppress("UNCHECKED_CAST")
package tornadofxbuilders

import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.*
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.scene.control.*
import javafx.scene.control.cell.*
import javafx.scene.input.MouseEvent
import javafx.scene.text.Text
import javafx.util.Callback
import javafx.util.StringConverter
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

/**
 * Create a spinner for an arbitrary type. This spinner requires you to configure a value factory, or it will throw an exception.
 */
fun <T> EventTarget.spinner(
    editable: Boolean = false,
    property: Property<T>? = null,
    enableScroll: Boolean = false,
    op: Spinner<T>.() -> Unit = {}
) = Spinner<T>().also{
    it.isEditable = editable
    it.attachTo(this, op)

    if (property != null) requireNotNull(it.valueFactory) {
        "You must configure the value factory or use the Number based spinner builder " +
                "which configures a default value factory along with min, max and initialValue!"
    }.valueProperty().apply {
        bindBidirectional(property)
    }

    if (enableScroll) it.setOnScroll { event ->
        if (event.deltaY > 0) it.increment()
        if (event.deltaY < 0) it.decrement()
    }

    if (editable) it.focusedProperty().addListener { _, _, newValue ->
        if (!newValue) it.increment(0)
    }
}

inline fun <reified T : Number> EventTarget.spinner(min: T? = null, max: T? = null, initialValue: T? = null, amountToStepBy: T? = null, editable: Boolean = false, property: Property<T>? = null, enableScroll: Boolean = false, noinline op: Spinner<T>.() -> Unit = {}): Spinner<T> {
    val spinner: Spinner<T>
    val isInt = (property is IntegerProperty && property !is DoubleProperty && property !is FloatProperty) || min is Int || max is Int || initialValue is Int ||
            T::class == Int::class || T::class == Integer::class || T::class.javaPrimitiveType == Integer::class.java
    if (isInt) {
        spinner = Spinner(min?.toInt() ?: 0, max?.toInt() ?: 100, initialValue?.toInt() ?: 0, amountToStepBy?.toInt()
            ?: 1)
    } else {
        spinner = Spinner(min?.toDouble() ?: 0.0, max?.toDouble() ?: 100.0, initialValue?.toDouble()
            ?: 0.0, amountToStepBy?.toDouble() ?: 1.0)
    }
    if (property != null) {
        spinner.valueFactory.valueProperty().bindBidirectional(property)
    }
    spinner.isEditable = editable

    if (enableScroll) {
        spinner.setOnScroll { event ->
            if (event.deltaY > 0) spinner.increment()
            if (event.deltaY < 0) spinner.decrement()
        }
    }

    if (editable) {
        spinner.focusedProperty().addListener { _, _, newValue ->
            if (!newValue) {
                spinner.increment(0)
            }
        }
    }

    return spinner.attachTo(this, op)
}

fun <T> EventTarget.spinner(
    items: ObservableList<T>,
    editable: Boolean = false,
    property: Property<T>? = null,
    enableScroll: Boolean = false,
    op: Spinner<T>.() -> Unit = {}
) = Spinner<T>(items).attachTo(this,op){
    if (property != null) it.valueFactory.valueProperty().apply {
        bindBidirectional(property)
    }

    it.isEditable = editable

    if (enableScroll) it.setOnScroll { event ->
        if (event.deltaY > 0) it.increment()
        if (event.deltaY < 0) it.decrement()
    }

    if (editable) it.focusedProperty().addListener { _, _, newValue ->
        if (!newValue) it.increment(0)
    }
}

fun <T> EventTarget.spinner(
    valueFactory: SpinnerValueFactory<T>,
    editable: Boolean = false,
    property: Property<T>? = null,
    enableScroll: Boolean = false,
    op: Spinner<T>.() -> Unit = {}
) = Spinner<T>(valueFactory).attachTo(this, op){
    if (property != null) it.valueFactory.valueProperty().apply {
        bindBidirectional(property)
    }

    it.isEditable = editable

    if (enableScroll) it.setOnScroll { event ->
        if (event.deltaY > 0) it.increment()
        if (event.deltaY < 0) it.decrement()
    }

    if (editable) it.focusedProperty().addListener { _, _, newValue ->
        if (!newValue) it.increment(0)
    }
}

fun <T> EventTarget.combobox(property: Property<T>? = null, values: List<T>? = null, op: ComboBox<T>.() -> Unit = {}) = ComboBox<T>().attachTo(this, op) {
    if (values != null) it.items = values as? ObservableList<T> ?: values.asObservable()
    if (property != null) it.bind(property)
}

fun <T> EventTarget.choicebox(property: Property<T>? = null, values: List<T>? = null, op: ChoiceBox<T>.() -> Unit = {}) = ChoiceBox<T>().attachTo(this, op) {
    if (values != null) it.items = (values as? ObservableList<T>) ?: values.asObservable()
    if (property != null) it.bind(property)
}

fun <T> EventTarget.listview(values: ObservableList<T>? = null, op: ListView<T>.() -> Unit = {}) = ListView<T>().attachTo(this, op) {
    if (values != null) {
        if (values is SortedFilteredList<T>) values.bindTo(it)
        else it.items = values
    }
}

fun <T> EventTarget.listview(values: ReadOnlyListProperty<T>, op: ListView<T>.() -> Unit = {}) = listview(values as ObservableValue<ObservableList<T>>, op)

fun <T> EventTarget.listview(values: ObservableValue<ObservableList<T>>, op: ListView<T>.() -> Unit = {}) = ListView<T>().attachTo(this, op) {
    fun rebinder() {
        (it.items as? SortedFilteredList<T>)?.bindTo(it)
    }
    it.itemsProperty().bind(values)
    rebinder()
    it.itemsProperty().onChange {
        rebinder()
    }
}

fun <T> EventTarget.tableview(items: ObservableList<T>? = null, op: TableView<T>.() -> Unit = {}) = TableView<T>().attachTo(this, op) {
    if (items != null) {
        if (items is SortedFilteredList<T>) items.bindTo(it)
        else it.items = items
    }
}

fun <T> EventTarget.tableview(items: ReadOnlyListProperty<T>, op: TableView<T>.() -> Unit = {}) = tableview(items as ObservableValue<ObservableList<T>>, op)

fun <T> EventTarget.tableview(items: ObservableValue<out ObservableList<T>>, op: TableView<T>.() -> Unit = {}) = TableView<T>().attachTo(this, op) {
    fun rebinder() {
        (it.items as? SortedFilteredList<T>)?.bindTo(it)
    }
    it.itemsProperty().bind(items)
    rebinder()
    it.itemsProperty().onChange {
        rebinder()
    }
    items.onChange {
        rebinder()
    }
}

fun <T> EventTarget.treeview(root: TreeItem<T>? = null, op: TreeView<T>.() -> Unit = {}) = TreeView<T>().attachTo(this, op) {
    if (root != null) it.root = root
}

fun <T> EventTarget.treetableview(root: TreeItem<T>? = null, op: TreeTableView<T>.() -> Unit = {}) = TreeTableView<T>().attachTo(this, op) {
    if (root != null) it.root = root
}

fun <T> TreeItem<T>.treeitem(value: T? = null, op: TreeItem<T>.() -> Unit = {}): TreeItem<T> {
    val treeItem = value?.let { TreeItem<T>(it) } ?: TreeItem<T>()
    treeItem.op()
    this += treeItem
    return treeItem
}

operator fun <T> TreeItem<T>.plusAssign(treeItem: TreeItem<T>) {
    this.children.add(treeItem)
}

fun <S> TableView<S>.makeIndexColumn(name: String = "#", startNumber: Int = 1): TableColumn<S, Number> {
    return TableColumn<S, Number>(name).apply {
        isSortable = false
        prefWidth = width
        this@makeIndexColumn.columns += this
        setCellValueFactory { ReadOnlyObjectWrapper(items.indexOf(it.value) + startNumber) }
    }
}

fun <S, T> TableColumn<S, T>.enableTextWrap() = apply {
    setCellFactory {
        TableCell<S, T>().apply {
            val text = Text()
            graphic = text
            prefHeight = Control.USE_COMPUTED_SIZE
            text.wrappingWidthProperty().bind(this@enableTextWrap.widthProperty().subtract(Bindings.multiply(2.0, graphicTextGapProperty())))
            text.textProperty().bind(itemProperty().stringBinding { item -> item?.toString() ?: "" })
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun <S> TableView<S>.addColumnInternal(column: TableColumn<S, *>, index: Int? = null) {
    val columnTarget = properties["tornadofx.columnTarget"] as? ObservableList<TableColumn<S, *>> ?: columns
    if (index == null) columnTarget.add(column) else columnTarget.add(index, column)
}

@Suppress("UNCHECKED_CAST")
fun <S> TreeTableView<S>.addColumnInternal(column: TreeTableColumn<S, *>, index: Int? = null) {
    val columnTarget = properties["tornadofx.columnTarget"] as? ObservableList<TreeTableColumn<S, *>> ?: columns
    if (index == null) columnTarget.add(column) else columnTarget.add(index, column)
}

/**
 * Create a column holding children columns
 */
@Suppress("UNCHECKED_CAST")
fun <S> TableView<S>.nestedColumn(title: String, op: TableView<S>.(TableColumn<S, Any?>) -> Unit = {}): TableColumn<S, Any?> {
    val column = TableColumn<S, Any?>(title)
    addColumnInternal(column)
    val previousColumnTarget = properties["tornadofx.columnTarget"] as? ObservableList<TableColumn<S, *>>
    properties["tornadofx.columnTarget"] = column.columns
    op(this, column)
    properties["tornadofx.columnTarget"] = previousColumnTarget
    return column
}

/**
 * Create a column holding children columns
 */
@Suppress("UNCHECKED_CAST")
fun <S> TreeTableView<S>.nestedColumn(title: String, op: TreeTableView<S>.() -> Unit = {}): TreeTableColumn<S, Any?> {
    val column = TreeTableColumn<S, Any?>(title)
    addColumnInternal(column)
    val previousColumnTarget = properties["tornadofx.columnTarget"] as? ObservableList<TableColumn<S, *>>
    properties["tornadofx.columnTarget"] = column.columns
    op(this)
    properties["tornadofx.columnTarget"] = previousColumnTarget
    return column
}

/**
 * Create a column using the propertyName of the attribute you want shown.
 */
fun <S, T> TableView<S>.column(title: String, propertyName: String, op: TableColumn<S, T>.() -> Unit = {}): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = PropertyValueFactory<S, T>(propertyName)
    addColumnInternal(column)
    return column.also(op)
}

/**
 * Create a column using the getter of the attribute you want shown.
 */
@JvmName("pojoColumn")
fun <S, T> TableView<S>.column(title: String, getter: KFunction<T>): TableColumn<S, T> {
    val startIndex = if (getter.name.startsWith("is") && getter.name[2].isUpperCase()) 2 else 3
    val propName = getter.name.substring(startIndex).decapitalize()
    return this.column(title, propName)
}

/**
 * Create a column using the propertyName of the attribute you want shown.
 */
fun <S, T> TreeTableView<S>.column(title: String, propertyName: String, op: TreeTableColumn<S, T>.() -> Unit = {}): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = TreeItemPropertyValueFactory<S, T>(propertyName)
    addColumnInternal(column)
    return column.also(op)
}

/**
 * Create a column using the getter of the attribute you want shown.
 */
@JvmName("pojoColumn")
fun <S, T> TreeTableView<S>.column(title: String, getter: KFunction<T>): TreeTableColumn<S, T> {
    val startIndex = if (getter.name.startsWith("is") && getter.name[2].isUpperCase()) 2 else 3
    val propName = getter.name.substring(startIndex).decapitalize()
    return this.column(title, propName)
}

fun <S, T> TableColumn<S, T?>.useComboBox(items: ObservableList<T>, afterCommit: (TableColumn.CellEditEvent<S, T?>) -> Unit = {}) = apply {
    cellFactory = ComboBoxTableCell.forTableColumn(items)
    setOnEditCommit {
        val property = it.tableColumn.getCellObservableValue(it.rowValue) as Property<T?>
        property.value = it.newValue
        afterCommit(it)
    }
}

inline fun <S, reified T> TableColumn<S, T?>.useTextField(
    converter: StringConverter<T>? = null,
    noinline afterCommit: (TableColumn.CellEditEvent<S, T?>) -> Unit = {}
) = apply {
    when (T::class) {
        String::class -> {
            @Suppress("UNCHECKED_CAST")
            val stringColumn = this as TableColumn<S, String?>
            stringColumn.cellFactory = TextFieldTableCell.forTableColumn()
        }
        else -> {
            requireNotNull(converter) { "You must supply a converter for non String columns" }
            cellFactory = TextFieldTableCell.forTableColumn(converter)
        }
    }

    setOnEditCommit {
        val property = it.tableColumn.getCellObservableValue(it.rowValue) as Property<T?>
        property.value = it.newValue
        afterCommit(it)
    }
}

fun <S, T> TableColumn<S, T?>.useChoiceBox(items: ObservableList<T>, afterCommit: (TableColumn.CellEditEvent<S, T?>) -> Unit = {}) = apply {
    cellFactory = ChoiceBoxTableCell.forTableColumn(items)
    setOnEditCommit {
        val property = it.tableColumn.getCellObservableValue(it.rowValue) as Property<T?>
        property.value = it.newValue
        afterCommit(it)
    }
}

// This was used earlier, but was changed to using cellFormat with cache, see above
//class CheckBoxCell<S>(val makeEditable: Boolean) : TableCell<S, Boolean?>() {
//    val checkbox: CheckBox by lazy {
//        CheckBox().apply {
//            if (makeEditable) {
//                selectedProperty().bindBidirectional(itemProperty())
//                setOnAction {
//                    tableView.edit(index, tableColumn)
//                    commitEdit(!isSelected)
//                }
//            } else {
//                isDisable = true
//                selectedProperty().bind(itemProperty())
//            }
//        }
//    }
//
//    init {
//        if (makeEditable) {
//            isEditable = true
//            tableView?.isEditable = true
//        }
//    }
//
//    override fun updateItem(item: Boolean?, empty: Boolean) {
//        super.updateItem(item, empty)
//        style { alignment = Pos.CENTER }
//        graphic = if (empty || item == null) null else checkbox
//    }
//}


fun <S> ListView<S>.useCheckbox(converter: StringConverter<S>? = null, getter: (S) -> ObservableValue<Boolean>) {
    setCellFactory { CheckBoxListCell(getter, converter) }
}

fun <T> TableView<T>.bindSelected(property: Property<T>) {
    selectionModel.selectedItemProperty().onChange {
        property.value = it
    }
}

fun <T> ComboBox<T>.bindSelected(property: Property<T>) {
    selectionModel.selectedItemProperty().onChange {
        property.value = it
    }
}

val <T> TableView<T>.selectedCell: TablePosition<T, *>?
    get() = selectionModel.selectedCells.firstOrNull() as TablePosition<T, *>?

val <T> TableView<T>.selectedColumn: TableColumn<T, *>?
    get() = selectedCell?.tableColumn

val <T> TableView<T>.selectedValue: Any?
    get() = selectedColumn?.getCellObservableValue(selectedItem)?.value

val <T> TreeTableView<T>.selectedCell: TreeTablePosition<T, *>?
    get() = selectionModel.selectedCells.firstOrNull()

val <T> TreeTableView<T>.selectedColumn: TreeTableColumn<T, *>?
    get() = selectedCell?.tableColumn

val <T> TreeTableView<T>.selectedValue: Any?
    get() = selectedColumn?.getCellObservableValue(selectionModel.selectedItem)?.value

/**
 * Create a column with a value factory that extracts the value from the given mutable
 * property and converts the property to an observable value.
 */
inline fun <reified S, T> TableView<S>.column(title: String, prop: KMutableProperty1<S, T>, noinline op: TableColumn<S, T>.() -> Unit = {}): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { observable(it.value, prop) }
    addColumnInternal(column)
    return column.also(op)
}

inline fun <reified S, T> TreeTableView<S>.column(title: String, prop: KMutableProperty1<S, T>, noinline op: TreeTableColumn<S, T>.() -> Unit = {}): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = Callback { observable(it.value.value, prop) }
    addColumnInternal(column)
    return column.also(op)
}

/**
 * Create a column with a value factory that extracts the value from the given property and
 * converts the property to an observable value.
 *
 * ATTENTION: This function was renamed to `readonlyColumn` to avoid shadowing the version for
 * observable properties.
 */
inline fun <reified S, T> TableView<S>.readonlyColumn(title: String, prop: KProperty1<S, T>, noinline op: TableColumn<S, T>.() -> Unit = {}): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { observable(it.value, prop) }
    addColumnInternal(column)
    return column.also(op)
}

inline fun <reified S, T> TreeTableView<S>.column(title: String, prop: KProperty1<S, T>, noinline op: TreeTableColumn<S, T>.() -> Unit = {}): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = Callback { observable(it.value.value, prop) }
    addColumnInternal(column)
    return column.also(op)
}

/**
 * Create a column with a value factory that extracts the value from the given ObservableValue property.
 */
inline fun <reified S, T> TableView<S>.column(title: String, prop: KProperty1<S, ObservableValue<T>>, noinline op: TableColumn<S, T>.() -> Unit = {}): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { prop.call(it.value) }
    addColumnInternal(column)
    return column.also(op)
}

/**
 * Add a global edit commit handler to the TableView. You avoid assuming the responsibility
 * for writing back the data into your domain object and can consentrate on the actual
 * response you want to happen when a column commits and edit.
 */
fun <S> TableView<S>.onEditCommit(onCommit: TableColumn.CellEditEvent<S, Any>.(S) -> Unit) {
    fun addEventHandlerForColumn(column: TableColumn<S, *>) {
        column.addEventHandler(TableColumn.editCommitEvent<S, Any>()) { event ->
            // Make sure the domain object gets the new value before we notify our handler
            Platform.runLater {
                onCommit(event, event.rowValue)
            }
        }
        column.columns.forEach(::addEventHandlerForColumn)
    }

    columns.forEach(::addEventHandlerForColumn)

    columns.addListener({ change: ListChangeListener.Change<out TableColumn<S, *>> ->
        while (change.next()) {
            if (change.wasAdded())
                change.addedSubList.forEach(::addEventHandlerForColumn)
        }
    })
}

/**
 * Add a global edit start handler to the TableView. You can use this callback
 * to cancel the edit request by calling cancel()
 */
fun <S> TableView<S>.onEditStart(onEditStart: TableColumn.CellEditEvent<S, Any?>.(S) -> Unit) {
    fun addEventHandlerForColumn(column: TableColumn<S, *>) {
        column.addEventHandler(TableColumn.editStartEvent<S, Any?>()) { event ->
            onEditStart(event, event.rowValue)
        }
        column.columns.forEach(::addEventHandlerForColumn)
    }

    columns.forEach(::addEventHandlerForColumn)

    columns.addListener({ change: ListChangeListener.Change<out TableColumn<S, *>> ->
        while (change.next()) {
            if (change.wasAdded())
                change.addedSubList.forEach(::addEventHandlerForColumn)
        }
    })
}

/**
 * Used to cancel an edit event, typically from `onEditStart`
 */
fun <S, T> TableColumn.CellEditEvent<S, T>.cancel() {
    tableView.edit(-1, tableColumn);
}

/**
 * Create a column with a title specified cell type and operate on it. Inside the code block you can call
 * `value { it.value.someProperty }` to set up a cellValueFactory that must return T or ObservableValue<T>
 */
@Suppress("UNUSED_PARAMETER")
fun <S, T : Any> TableView<S>.column(title: String, cellType: KClass<T>, op: TableColumn<S, T>.() -> Unit = {}): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    addColumnInternal(column)
    return column.also(op)
}

/**
 * Create a column with a value factory that extracts the value from the given callback.
 */
fun <S, T> TableView<S>.column(title: String, valueProvider: (TableColumn.CellDataFeatures<S, T>) -> ObservableValue<T>): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { valueProvider(it) }
    addColumnInternal(column)
    return column
}

/**
 * Configure a cellValueFactory for the column. If the returned value is not observable, it is automatically
 * wrapped in a SimpleObjectProperty for convenience.
 */
@Suppress("UNCHECKED_CAST")
infix fun <S> TableColumn<S, *>.value(cellValueFactory: (TableColumn.CellDataFeatures<S, Any>) -> Any?) = apply {
    this.cellValueFactory = Callback {
        val createdValue = cellValueFactory(it as TableColumn.CellDataFeatures<S, Any>)
        (createdValue as? ObservableValue<Any>) ?: SimpleObjectProperty(createdValue)
    }
}

@JvmName(name = "columnForObservableProperty")
inline fun <reified S, T> TreeTableView<S>.column(title: String, prop: KProperty1<S, ObservableValue<T>>): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = Callback { prop.call(it.value.value) }
    addColumnInternal(column)
    return column
}

/**
 * Create a column with a value factory that extracts the observable value from the given function reference.
 * This method requires that you have kotlin-reflect on your classpath.
 */
inline fun <S, reified T> TableView<S>.column(title: String, observableFn: KFunction<ObservableValue<T>>): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { observableFn.call(it.value) }
    addColumnInternal(column)
    return column
}

inline fun <S, reified T> TreeTableView<S>.column(title: String, observableFn: KFunction<ObservableValue<T>>): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = Callback { observableFn.call(it.value) }
    addColumnInternal(column)
    return column
}

/**
 * Create a column with a value factory that extracts the value from the given callback.
 */
inline fun <reified S, T> TreeTableView<S>.column(title: String, noinline valueProvider: (TreeTableColumn.CellDataFeatures<S, T>) -> ObservableValue<T>): TreeTableColumn<S, T> {
    val column = TreeTableColumn<S, T>(title)
    column.cellValueFactory = Callback { valueProvider(it) }
    addColumnInternal(column)
    return column
}

fun <T> TableView<T>.enableCellEditing() {
    selectionModel.isCellSelectionEnabled = true
    isEditable = true
}

fun <T> TableView<T>.selectOnDrag() {
    var startRow = 0
    var startColumn = columns.first()

    // Record start position and clear selection unless Control is down
    addEventFilter(MouseEvent.MOUSE_PRESSED) {
        startRow = 0

        (it.pickResult.intersectedNode as? TableCell<*, *>)?.apply {
            startRow = index
            startColumn = tableColumn as TableColumn<T, *>?

            if (selectionModel.isCellSelectionEnabled) {
                selectionModel.clearAndSelect(startRow, startColumn)
            } else {
                selectionModel.clearAndSelect(startRow)
            }
        }
    }

    // Select items while dragging
    addEventFilter(MouseEvent.MOUSE_DRAGGED) {
        (it.pickResult.intersectedNode as? TableCell<*, *>)?.apply {
            if (items.size > index) {
                if (selectionModel.isCellSelectionEnabled) {
                    selectionModel.selectRange(startRow, startColumn, index, tableColumn as TableColumn<T, *>?)
                } else {
                    selectionModel.selectRange(startRow, index)
                }
            }
        }
    }
}

/**
 * Write a value into the property representing this TableColumn, provided
 * the property is writable.
 */
@Suppress("UNCHECKED_CAST")
fun <S, T> TableColumn<S, T>.setValue(item: S, value: T?) {
    val property = getTableColumnProperty(item)
    (property as? WritableValue<T>)?.value = value
}

/**
 * Get the value from the property representing this TableColumn.
 */
fun <S, T> TableColumn<S, T>.getValue(item: S) = getTableColumnProperty(item).value

/**
 * Get the property representing this TableColumn for the given item.
 */
fun <S, T> TableColumn<S, T>.getTableColumnProperty(item: S): ObservableValue<T?> {
    val param = TableColumn.CellDataFeatures<S, T>(tableView, this, item)
    val property = cellValueFactory.call(param)
    return property
}
