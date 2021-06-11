/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.constraintlayout.compose

import androidx.compose.ui.unit.Dp
import androidx.constraintlayout.core.state.ConstraintReference
import androidx.constraintlayout.core.state.Dimension
import androidx.constraintlayout.core.state.Dimension.SPREAD_DIMENSION
import androidx.constraintlayout.core.state.State.Chain.*
import androidx.constraintlayout.core.state.helpers.GuidelineReference
import androidx.constraintlayout.core.widgets.ConstraintWidget
import org.json.JSONArray
import org.json.JSONObject

internal val PARSER_DEBUG = false

class LayoutVariables {
    val margins = HashMap<String, Int>()
    val generators = HashMap<String, GeneratedValue>()
    val arrayIds = HashMap<String, ArrayList<String>>()

    fun put(elementName: String, element: Int) {
        margins[elementName] = element
    }

    fun put(elementName: String, start: Float, incrementBy: Float) {
        if (generators.containsKey(elementName)) {
            if (generators[elementName] is OverrideValue) {
                return
            }
        }
        var generator = Generator(start, incrementBy)
        generators[elementName] = generator
    }

    fun putOverride(elementName: String, value: Float) {
        var generator = OverrideValue(value)
        generators[elementName] = generator
    }

    fun get(elementName: Any): Float {
        if (elementName is String) {
            if (generators.containsKey(elementName)) {
                val value = generators[elementName]!!.value()
                return value
            }
            if (margins.containsKey(elementName)) {
                return margins[elementName]!!.toFloat()
            }
        } else if (elementName is Int) {
            return elementName.toFloat()
        } else if (elementName is Float) {
            return elementName
        }
        return 0f
    }

    fun getList(elementName: String) : ArrayList<String>? {
        if (arrayIds.containsKey(elementName)) {
            return arrayIds[elementName]
        }
        return null
    }

    fun put(elementName: String, elements: ArrayList<String>) {
        arrayIds[elementName] = elements
    }

}
interface GeneratedValue {
    fun value() : Float
}

class Generator(start: Float, incrementBy: Float) : GeneratedValue {
    var start : Float = start
    var incrementBy: Float = incrementBy
    var current : Float = start
    var stop = false

    override fun value() : Float {
        if (!stop) {
            current += incrementBy
        }
        return current
    }
}

class OverrideValue(value: Float) : GeneratedValue {
    var value : Float = value
    override fun value() : Float {
        return value
    }
}

internal fun parseJSON(content: String, state: State, layoutVariables: LayoutVariables) {
    val json = JSONObject(content)
    val elements = json.names() ?: return
    (0 until elements.length()).forEach { i ->
        val elementName = elements[i].toString()
        val element = json[elementName]
        if (PARSER_DEBUG) {
            System.out.println("element <$elementName = $element> " + element.javaClass)
        }
        when (elementName) {
            "Variables" -> parseVariables(state, layoutVariables, element)
            "Helpers" -> parseHelpers(state, layoutVariables, element)
            "Generate" -> parseGenerate(state, layoutVariables, element)
            else -> {
                if (element is JSONObject) {
                    var type = lookForType(element)
                    if (type != null) {
                        when (type) {
                            "hGuideline" -> parseGuidelineParams(ConstraintWidget.HORIZONTAL, state, elementName, element)
                            "vGuideline" -> parseGuidelineParams(ConstraintWidget.VERTICAL, state, elementName, element)
                            "barrier" -> parseBarrier(state, elementName, element)
                        }
                    } else if (type == null) {
                        parseWidget(state, layoutVariables, elementName, element)
                    }
                }
            }
        }
    }
}

fun parseVariables(state: State, layoutVariables: LayoutVariables, json: Any) {
    if (!(json is JSONObject)) {
        return
    }
    val elements = json.names() ?: return
    (0 until elements.length()).forEach { i ->
        val elementName = elements[i].toString()
        val element = json[elementName]
        if (element is Int) {
            layoutVariables.put(elementName, element)
        } else if (element is JSONObject) {
            if (element.has("start") && element.has("increment")) {
                var start = layoutVariables.get(element["start"])
                var increment = layoutVariables.get(element["increment"])
                layoutVariables.put(elementName, start, increment)
            } else if (element.has("ids")) {
                var ids = element.getJSONArray("ids");
                var arrayIds = arrayListOf<String>()
                for (i in 0..ids.length()-1) {
                    arrayIds.add(ids.getString(i))
                }
                layoutVariables.put(elementName, arrayIds)
            } else if (element.has("tag")) {
                var arrayIds = state.getIdsForTag(element.getString("tag"))
                layoutVariables.put(elementName, arrayIds)
            }
        }
    }
}

fun parseHelpers(state: State, layoutVariables: LayoutVariables, element: Any) {
    if (!(element is JSONArray)) {
        return
    }
    (0 until element.length()).forEach { i ->
        val helper = element[i]
        if (helper is JSONArray && helper.length() > 1) {
            when (helper[0]) {
                "hChain" -> parseChain(ConstraintWidget.HORIZONTAL, state, layoutVariables, helper)
                "vChain" -> parseChain(ConstraintWidget.VERTICAL, state, layoutVariables, helper)
                "hGuideline" -> parseGuideline(ConstraintWidget.HORIZONTAL, state, layoutVariables, helper)
                "vGuideline" -> parseGuideline(ConstraintWidget.VERTICAL, state, layoutVariables, helper)
            }
        }
    }
}

fun parseGenerate(state: State, layoutVariables: LayoutVariables, json: Any) {
    if (!(json is JSONObject)) {
        return
    }
    val elements = json.names() ?: return
    (0 until elements.length()).forEach { i ->
        val elementName = elements[i].toString()
        val element = json[elementName]
        var arrayIds = layoutVariables.getList(elementName)
        if (arrayIds != null && element is JSONObject) {
            for (id in arrayIds) {
                parseWidget(state, layoutVariables, id, element)
            }
        }
    }
}

fun parseChain(orientation: Int, state: State, margins: LayoutVariables, helper: JSONArray) {
    var chain = if (orientation == ConstraintWidget.HORIZONTAL) state.horizontalChain() else state.verticalChain()
    var refs = helper[1]
    if (!(refs is JSONArray) || refs.length() < 1) {
        return
    }
    (0 until refs.length()).forEach { i ->
        chain.add(refs[i])
    }
    if (helper.length() > 2) { // we have additional parameters
        var params = helper[2]
        if (!(params is JSONObject)) {
            return
        }
        val constraints = params.names() ?: return
        (0 until constraints.length()).forEach{ i ->
            val constraintName = constraints[i].toString()
            when (constraintName) {
                "style" -> {
                    val styleObject = params[constraintName]
                    val styleValue : String
                    if (styleObject is JSONArray && styleObject.length() > 1) {
                        styleValue = styleObject[0].toString()
                        var biasValue = styleObject[1].toString().toFloat()
                        chain.bias(biasValue)
                    } else {
                        styleValue = styleObject.toString()
                    }
                    when (styleValue) {
                        "packed" -> chain.style(PACKED)
                        "spread_inside" -> chain.style(SPREAD_INSIDE)
                        else -> chain.style(SPREAD)
                    }
                }
                else -> {
                    parseConstraint(state, margins, params, chain as ConstraintReference, constraintName)
                }
            }
        }
    }
}

fun parseGuideline(orientation: Int, state: State, margins: LayoutVariables, helper: JSONArray) {
    var params = helper[1]
    if (!(params is JSONObject)) {
        return
    }
    val guidelineId = params.opt("id")
    if (guidelineId == null)  {
        return
    }
    parseGuidelineParams(orientation, state, guidelineId as String, params)
}

private fun parseGuidelineParams(
    orientation: Int,
    state: State,
    guidelineId: String,
    params: JSONObject
) {
    val constraints = params.names() ?: return
    var reference = state.constraints(guidelineId)
    if (orientation == ConstraintWidget.HORIZONTAL) {
        state.horizontalGuideline(guidelineId)
    } else {
        state.verticalGuideline(guidelineId)
    }
    var guidelineReference = reference.facade as GuidelineReference
    (0 until constraints.length()).forEach { i ->
        val constraintName = constraints[i].toString()
        when (constraintName) {
            "start" -> {
                val margin = state.convertDimension(
                    Dp(
                        params.getInt(constraintName).toFloat()
                    )
                )
                guidelineReference.start(margin)
            }
            "end" -> {
                val margin = state.convertDimension(
                    Dp(
                        params.getInt(constraintName).toFloat()
                    )
                )
                guidelineReference.end(margin)
            }
            "percent" -> {
                guidelineReference.percent(
                    params.getDouble(
                        constraintName
                    ).toFloat()
                )
            }
        }
    }
}

fun parseBarrier(
    state: State,
    elementName: String, element: JSONObject) {
    val reference = state.barrier(elementName, androidx.constraintlayout.core.state.State.Direction.END)
    val constraints = element.names() ?: return
    var barrierReference = reference
    (0 until constraints.length()).forEach { i ->
        val constraintName = constraints[i].toString()
        when (constraintName) {
            "direction" -> {
                var direction = element.getString(constraintName)
                when (direction) {
                    "start" -> barrierReference.setBarrierDirection(androidx.constraintlayout.core.state.State.Direction.START)
                    "end" -> barrierReference.setBarrierDirection(androidx.constraintlayout.core.state.State.Direction.END)
                    "left" -> barrierReference.setBarrierDirection(androidx.constraintlayout.core.state.State.Direction.LEFT)
                    "right" -> barrierReference.setBarrierDirection(androidx.constraintlayout.core.state.State.Direction.RIGHT)
                    "top" -> barrierReference.setBarrierDirection(androidx.constraintlayout.core.state.State.Direction.TOP)
                    "bottom" -> barrierReference.setBarrierDirection(androidx.constraintlayout.core.state.State.Direction.BOTTOM)
                }
            }
            "contains" -> {
                val list = element.optJSONArray(constraintName)
                if (list != null) {
                    for (i in 0..list.length() - 1) {
                        var elementName = list.get(i)
                        val reference = state.constraints(elementName)
                        System.out.println("Add REFERENCE ($elementName = $reference) TO BARRIER ")
                        barrierReference.add(reference)
                    }
                }
            }
        }
    }
}

fun parseWidget(
    state: State,
    layoutVariables: LayoutVariables,
    elementName: String,
    element: JSONObject
) {
    val reference = state.constraints(elementName)
    val constraints = element.names() ?: return
    reference.width = Dimension.Wrap()
    reference.height = Dimension.Wrap()
    (0 until constraints.length()).forEach { i ->
        val constraintName = constraints[i].toString()
        when (constraintName) {
            "width" -> {
                reference.width = parseDimension(element, constraintName, state)
            }
            "height" -> {
                reference.height = parseDimension(element, constraintName, state)
            }
            "center" -> {
                val target = element.getString(constraintName)
                val targetReference = if (target.toString().equals("parent")) {
                    state.constraints(SolverState.PARENT)
                } else {
                    state.constraints(target)
                }
                reference.startToStart(targetReference)
                reference.endToEnd(targetReference)
                reference.topToTop(targetReference)
                reference.bottomToBottom(targetReference)
            }
            "centerHorizontally" -> {
                val target = element.getString(constraintName)
                val targetReference = if (target.toString().equals("parent")) {
                    state.constraints(SolverState.PARENT)
                } else {
                    state.constraints(target)
                }
                reference.startToStart(targetReference)
                reference.endToEnd(targetReference)
            }
            "centerVertically" -> {
                val target = element.getString(constraintName)
                val targetReference = if (target.toString().equals("parent")) {
                    state.constraints(SolverState.PARENT)
                } else {
                    state.constraints(target)
                }
                reference.topToTop(targetReference)
                reference.bottomToBottom(targetReference)
            }
            "alpha" -> {
                var value = layoutVariables.get(element[constraintName])
                reference.alpha(value)
//                reference.alpha(element.getDouble(constraintName).toFloat())
            }
            "scaleX" -> {
                var value = layoutVariables.get(element[constraintName])
                reference.scaleX(value) //element.getDouble(constraintName).toFloat())
            }
            "scaleY" -> {
                var value = layoutVariables.get(element[constraintName])
                reference.scaleY(value) //element.getDouble(constraintName).toFloat())
            }
            "translationX" -> {
                var value = layoutVariables.get(element[constraintName])
                reference.translationX(value)  //element.getDouble(constraintName).toFloat())
            }
            "translationY" -> {
                var value = layoutVariables.get(element[constraintName])
                reference.translationY(value) //element.getDouble(constraintName).toFloat())
            }
            "rotationX" -> {
                var value = layoutVariables.get(element[constraintName])
                reference.rotationX(value) //element.getDouble(constraintName).toFloat())
            }
            "rotationY" -> {
                var value = layoutVariables.get(element[constraintName])
                reference.rotationY(value) //element.getDouble(constraintName).toFloat())
            }
            "rotationZ" -> {
                var value = layoutVariables.get(element[constraintName])
                reference.rotationZ(value) // element.getDouble(constraintName).toFloat())
            }
            else -> {
                parseConstraint(state, layoutVariables, element, reference, constraintName)
            }
        }
    }
}

private fun parseConstraint(
    state: State,
    layoutVariables: LayoutVariables,
    element: JSONObject,
    reference: ConstraintReference,
    constraintName: String
) {
    val constraint = element.optJSONArray(constraintName)
    if (constraint != null && constraint.length() > 1) {
        val target = constraint[0]
        val anchor = constraint[1]
        var margin: Int = 0
        if (constraint.length() > 2) {
            margin = layoutVariables.get(constraint[2]).toInt()
        }
        margin = state.convertDimension(Dp(margin.toFloat()))

        val targetReference = if (target.toString().equals("parent")) {
            state.constraints(SolverState.PARENT)
        } else {
            state.constraints(target)
        }
        when (constraintName) {
            "circular" -> {
                var angle = layoutVariables.get(constraint[1])
                reference.circularConstraint(targetReference, angle, 0f)
            }
            "start" -> {
                when (anchor) {
                    "start" -> {
                        reference.startToStart(targetReference)
                    }
                    "end" -> reference.startToEnd(targetReference)
                }
            }
            "end" -> {
                when (anchor) {
                    "start" -> reference.endToStart(targetReference)
                    "end" -> reference.endToEnd(targetReference)
                }
            }
            "top" -> {
                when (anchor) {
                    "top" -> reference.topToTop(targetReference)
                    "bottom" -> reference.topToBottom(targetReference)
                }
            }
            "bottom" -> {
                when (anchor) {
                    "top" -> {
                        reference.bottomToTop(targetReference)
                    }
                    "bottom" -> {
                        reference.bottomToBottom(targetReference)
                    }
                }
            }
        }
        reference.margin(margin)
    } else {
        var target = element.optString(constraintName)
        if (target != null) {
            val targetReference = if (target.toString().equals("parent")) {
                state.constraints(SolverState.PARENT)
            } else {
                state.constraints(target)
            }
            when (constraintName) {
                "start" -> reference.startToStart(targetReference)
                "end" -> reference.endToEnd(targetReference)
                "top" -> reference.topToTop(targetReference)
                "bottom" -> reference.bottomToBottom(targetReference)
            }
        }
    }
}

private fun parseDimension(
    element: JSONObject,
    constraintName: String,
    state: State
): Dimension {
    var dimensionString = element.getString(constraintName)
    var dimension: Dimension
    when (dimensionString) {
        "wrap" -> dimension = Dimension.Wrap()
        "spread" -> dimension = Dimension.Suggested(SPREAD_DIMENSION)
        "parent" -> dimension = Dimension.Parent()
        else -> {
            if (dimensionString.endsWith('%')) {
                // parent percent
                var percentString = dimensionString.substringBefore('%')
                var percentValue = percentString.toFloat() / 100f
                dimension = Dimension.Percent(0, percentValue).suggested(0)
            } else if (dimensionString.contains(':')) {
                dimension = Dimension.Ratio(dimensionString).suggested(0)
            } else {
                dimension = Dimension.Fixed(
                    state.convertDimension(
                        Dp(
                            element.getInt(constraintName).toFloat()
                        )
                    )
                )
            }
        }
    }
    return dimension
}

fun lookForType(element: JSONObject): String? {
    val constraints = element.names() ?: return null
    (0 until constraints.length()).forEach { i ->
        val constraintName = constraints[i].toString()
        if (constraintName.equals("type")) {
            return element.getString("type")
        }
    }
    return null
}