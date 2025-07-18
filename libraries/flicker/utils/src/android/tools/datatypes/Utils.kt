/*
 * Copyright (C) 2024 The Android Open Source Project
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

@file:JvmName("DataTypeUtils")

package android.tools.datatypes

import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Region
import androidx.core.graphics.toRect
import androidx.core.graphics.toRectF
import kotlin.math.abs

fun emptyColor(): Color = Color.valueOf(/*r */ -1f, /*g */ -1f, /*b */ -1f, /*a */ 0f)

fun defaultColor(): Color = Color.valueOf(/*r */ 0f, /* g */ 0f, /* b */ 0f, /* a */ 1f)

fun Color.isEmpty(): Boolean =
    this.red() == -1f && this.green() == -1f && this.blue() == -1f && this.alpha() == 0f

fun Color.isNotEmpty(): Boolean = !isEmpty()

fun Rect.crop(crop: Rect): Rect = this.toRectF().crop(crop.toRectF()).toRect()

fun RectF.crop(crop: RectF): RectF {
    val newLeft = maxOf(left, crop.left)
    val newTop = maxOf(top, crop.top)
    val newRight = minOf(right, crop.right)
    val newBottom = minOf(bottom, crop.bottom)
    return RectF(newLeft, newTop, newRight, newBottom)
}

fun RectF.containsWithThreshold(r: RectF, threshold: Float = 0.01f): Boolean {
    // check for empty first
    return this.left < this.right &&
        this.top < this.bottom && // now check for containment
        (left <= r.left || abs(left - r.left) < threshold) &&
        (top <= r.top || abs(top - r.top) < threshold) &&
        (right >= r.right || abs(right - r.right) < threshold) &&
        (bottom >= r.bottom || abs(bottom - r.bottom) < threshold)
}

fun Rect.intersection(r: Rect): Rect = intersection(r.left, r.top, r.right, r.bottom)

/**
 * If the rectangle specified by left,top,right,bottom intersects this rectangle, return true and
 * set this rectangle to that intersection, otherwise return false and do not change this rectangle.
 * No check is performed to see if either rectangle is empty. Note: To just test for intersection,
 * use intersects()
 *
 * @param left The left side of the rectangle being intersected with this rectangle
 * @param top The top of the rectangle being intersected with this rectangle
 * @param right The right side of the rectangle being intersected with this rectangle.
 * @param bottom The bottom of the rectangle being intersected with this rectangle.
 * @return A rectangle with the intersection coordinates
 */
fun Rect.intersection(left: Int, top: Int, right: Int, bottom: Int): Rect {
    if (this.left < right && left < this.right && this.top <= bottom && top <= this.bottom) {
        var intersectionLeft = this.left
        var intersectionTop = this.top
        var intersectionRight = this.right
        var intersectionBottom = this.bottom

        if (this.left < left) {
            intersectionLeft = left
        }
        if (this.top < top) {
            intersectionTop = top
        }
        if (this.right > right) {
            intersectionRight = right
        }
        if (this.bottom > bottom) {
            intersectionBottom = bottom
        }
        return Rect(intersectionLeft, intersectionTop, intersectionRight, intersectionBottom)
    }
    return Rect()
}

fun Region.outOfBoundsRegion(testRegion: Region): Region {
    val testRect = testRegion.bounds
    val outOfBoundsRegion = Region(this)
    outOfBoundsRegion.op(testRect, Region.Op.INTERSECT) && outOfBoundsRegion.op(this, Region.Op.XOR)
    return outOfBoundsRegion
}

fun Region.uncoveredRegion(testRegion: Region): Region {
    val uncoveredRegion = Region(this)
    uncoveredRegion.op(testRegion, Region.Op.INTERSECT) &&
        uncoveredRegion.op(testRegion, Region.Op.XOR)
    return uncoveredRegion
}

fun Region.coversAtLeast(testRegion: Region, tolerance: Int = 2): Boolean {
    // Expand [this] by [tolerance] pixels in all directions
    val expandedRegion =
        Region(this).apply {
            op(
                Region(
                    bounds.left - tolerance,
                    bounds.top - tolerance,
                    bounds.right + tolerance,
                    bounds.bottom + tolerance,
                ),
                Region.Op.UNION,
            )
        }

    val intersection = Region(expandedRegion)
    return intersection.op(testRegion, Region.Op.INTERSECT) &&
        !intersection.op(testRegion, Region.Op.XOR)
}

fun Region.coversAtMost(testRegion: Region): Boolean {
    if (this.isEmpty) {
        return true
    }
    val testRect = testRegion.bounds
    val intersection = Region(this)
    return intersection.op(testRect, Region.Op.INTERSECT) && !intersection.op(this, Region.Op.XOR)
}

fun Region.coversMoreThan(testRegion: Region): Boolean {
    return coversAtLeast(testRegion) && !Region(this).minus(testRegion).isEmpty
}

fun Region.minus(other: Region): Region {
    val thisRegion = Region(this)
    thisRegion.op(other, Region.Op.XOR)
    return thisRegion
}
