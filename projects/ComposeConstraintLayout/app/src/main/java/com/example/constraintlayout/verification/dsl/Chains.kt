/*
 * Copyright (C) 2021 The Android Open Source Project
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

@file:JvmName("DslVerificationKt")
@file:JvmMultifileClass

package com.example.constraintlayout.verification.dsl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility

@Preview
@Composable
fun Test10() {
    val constraintSet = ConstraintSet {
        val box1 = createRefFor("box1")
        val box2 = createRefFor("box2")

        createHorizontalChain(box1, box2, chainStyle = ChainStyle.Spread)
        constrain(box1) {
            width = Dimension.fillToConstraints
            height = Dimension.value(20.dp)
            centerVerticallyTo(parent)

            horizontalChainWeight = 1.5f
        }
        constrain(box2) {
            width = Dimension.fillToConstraints
            height = Dimension.fillToConstraints
            centerVerticallyTo(box1)

            horizontalChainWeight = 0.5f
        }
    }
    TwoBoxChainLayout(constraintSet = constraintSet)
}

@Preview
@Composable
fun Test11() {
    val constraintSet = ConstraintSet {
        val box1 = createRefFor("box1")
        val box2 = createRefFor("box2")

        createVerticalChain(box1, box2, chainStyle = ChainStyle.Spread)
        constrain(box1) {
            width = Dimension.value(20.dp)
            height = Dimension.fillToConstraints
            centerHorizontallyTo(parent)

            verticalChainWeight = 1.5f
        }
        constrain(box2) {
            width = Dimension.fillToConstraints
            height = Dimension.fillToConstraints
            centerHorizontallyTo(box1)

            verticalChainWeight = 0.5f
        }
    }
    TwoBoxChainLayout(constraintSet = constraintSet)
}

@Preview
@Composable
fun Test12() { // Constraint an horizontal chain
    val constraintSet = ConstraintSet {
        val box1 = createRefFor("box1")
        val box2 = createRefFor("box2")
        val box3 = createRefFor("box3")
        val chain1 = createHorizontalChain(box1, box2, chainStyle = ChainStyle.Spread)

        constrain(box1) {
            width = Dimension.value(20.dp)
            height = Dimension.value(20.dp)
            centerVerticallyTo(parent)
        }
        constrain(box2) {
            width = Dimension.value(20.dp)
            height = Dimension.value(20.dp)
            centerVerticallyTo(box1)
        }
        constrain(box3) {
            width = Dimension.value(200.dp)
            height = Dimension.value(20.dp)
            top.linkTo(parent.top, 12.dp)
            start.linkTo(parent.start, 12.dp)
        }
        constrain(chain1) {
            start.linkTo(box3.end, 10.dp)
        }
    }
    ThreeBoxChainLayout(constraintSet = constraintSet)
}

@Preview
@Composable
fun Test18() { // Constraint an horizontal chain with gone margins
    val constraintSet = ConstraintSet {
        val box1 = createRefFor("box1")
        val box2 = createRefFor("box2")
        val box3 = createRefFor("box3")
        val chain1 = createHorizontalChain(box1, box2, chainStyle = ChainStyle.Spread)

        constrain(box1) {
            width = Dimension.value(20.dp)
            height = Dimension.value(20.dp)
            centerVerticallyTo(parent)
        }
        constrain(box2) {
            width = Dimension.value(20.dp)
            height = Dimension.value(20.dp)
            centerVerticallyTo(box1)
        }
        constrain(box3) {
            width = Dimension.value(200.dp)
            height = Dimension.value(20.dp)
            top.linkTo(parent.top, 12.dp)
            start.linkTo(parent.start, 12.dp)
            visibility = Visibility.Gone
        }
        constrain(chain1) {
            start.linkTo(box3.end, 10.dp, 100.dp)
            // gone margin with parent should not matter
            end.linkTo(parent.end, 10.dp, 200.dp)
        }
    }
    ThreeBoxChainLayout(constraintSet = constraintSet)
}

@Preview
@Composable
fun Test13() { // Constrain a vertical chain
    val constraintSet = ConstraintSet {
        val box1 = createRefFor("box1")
        val box2 = createRefFor("box2")
        val box3 = createRefFor("box3")
        val chain1 = createVerticalChain(box1, box2, chainStyle = ChainStyle.Spread)

        constrain(box1) {
            width = Dimension.value(20.dp)
            height = Dimension.value(20.dp)
            centerHorizontallyTo(parent)
        }
        constrain(box2) {
            width = Dimension.value(20.dp)
            height = Dimension.value(20.dp)
            centerHorizontallyTo(box1)
        }
        constrain(box3) {
            width = Dimension.value(20.dp)
            height = Dimension.value(200.dp)
            top.linkTo(parent.top, 12.dp)
            start.linkTo(parent.start, 12.dp)
        }
        constrain(chain1) {
            top.linkTo(box3.bottom, 10.dp)
        }
    }
    ThreeBoxChainLayout(constraintSet = constraintSet)
}

@Preview
@Composable
fun Test17() { // Constrain a vertical chain with gone margins
    val constraintSet = ConstraintSet {
        val box1 = createRefFor("box1")
        val box2 = createRefFor("box2")
        val box3 = createRefFor("box3")
        val chain1 = createVerticalChain(box1, box2, chainStyle = ChainStyle.Spread)

        constrain(box1) {
            width = Dimension.value(20.dp)
            height = Dimension.value(20.dp)
            centerHorizontallyTo(parent)
        }
        constrain(box2) {
            width = Dimension.value(20.dp)
            height = Dimension.value(20.dp)
            centerHorizontallyTo(box1)
        }
        constrain(box3) {
            width = Dimension.value(20.dp)
            height = Dimension.value(200.dp)
            top.linkTo(parent.top, 12.dp)
            start.linkTo(parent.start, 12.dp)
            visibility = Visibility.Gone
        }
        constrain(chain1) {
            // With the gone margin it should basically layout the same as if box3 was visible
            top.linkTo(box3.bottom, 10.dp, 100.dp)
            // gone margin with parent should not matter
            bottom.linkTo(parent.bottom, 10.dp, 200.dp)
        }
    }
    ThreeBoxChainLayout(constraintSet = constraintSet)
}

@Preview
@Composable
fun Test14() { // Constrain a chain with the Modifier DSL
    Column {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
        ) {
            val (box1, box2, box3) = createRefs()
            val chain1 = createHorizontalChain(box1, box2)
            constrain(chain1) {
                start.linkTo(box3.end)
            }
            Box(
                modifier = Modifier
                    .background(Color.Red)
                    .constrainAs(box1) {
                        width = Dimension.value(20.dp)
                        height = Dimension.value(20.dp)
                        centerVerticallyTo(parent)
                    }
            )
            Box(
                modifier = Modifier
                    .background(Color.Blue)
                    .constrainAs(box2) {
                        width = Dimension.value(20.dp)
                        height = Dimension.value(20.dp)
                        centerVerticallyTo(box1)
                    }
            )
            Box(
                modifier = Modifier
                    .background(Color.Green)
                    .constrainAs(box3) {
                        width = Dimension.value(200.dp)
                        height = Dimension.value(20.dp)
                        top.linkTo(parent.top, 12.dp)
                        start.linkTo(parent.start, 12.dp)
                    }
            )

        }
        Button(onClick = { }) {
            Text(text = "Run")
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
@Composable
private inline fun ThreeBoxChainLayout(constraintSet: ConstraintSet) {
    Column {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true), constraintSet = constraintSet
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Red)
                    .layoutId("box1")
            )
            Box(
                modifier = Modifier
                    .background(Color.Blue)
                    .layoutId("box2")
            )
            Box(
                modifier = Modifier
                    .background(Color.Green)
                    .layoutId("box3")
            )
        }
        Button(onClick = { }) {
            Text(text = "Run")
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
@Composable
private inline fun TwoBoxChainLayout(constraintSet: ConstraintSet) {
    Column {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true), constraintSet = constraintSet
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Red)
                    .layoutId("box1")
            )
            Box(
                modifier = Modifier
                    .background(Color.Blue)
                    .layoutId("box2")
            )
        }
        Button(onClick = { }) {
            Text(text = "Run")
        }
    }
}