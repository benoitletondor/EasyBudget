/*
 *   Copyright 2024 Benoit LETONDOR
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.benoitletondor.easybudgetapp.view.main

import android.content.Context
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout

import kotlin.math.min

/**
 * Behavior of FAB for the floating action menu reacting to Snackbar
 *
 * @author Benoit LETONDOR
 */
@Suppress("UNUSED_PARAMETER")
class FloatingActionButtonBehavior(context: Context, attrs: AttributeSet) : CoordinatorLayout.Behavior<LinearLayout>() {

    override fun layoutDependsOn(parent: CoordinatorLayout, child: LinearLayout, dependency: View): Boolean {
        return dependency is Snackbar.SnackbarLayout
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: LinearLayout, dependency: View): Boolean {
        val translationY = min(0f, dependency.translationY - dependency.height)
        child.translationY = translationY
        return true
    }

    override fun onDependentViewRemoved(
        parent: CoordinatorLayout,
        child: LinearLayout,
        dependency: View
    ) {
        super.onDependentViewRemoved(parent, child, dependency)
        child.translationY = 0f
    }
}
