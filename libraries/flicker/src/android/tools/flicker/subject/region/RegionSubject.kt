/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.tools.flicker.subject.region

import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Region
import android.tools.Timestamp
import android.tools.datatypes.coversAtLeast
import android.tools.datatypes.coversAtMost
import android.tools.datatypes.outOfBoundsRegion
import android.tools.datatypes.uncoveredRegion
import android.tools.flicker.subject.FlickerSubject
import android.tools.flicker.subject.exceptions.ExceptionMessageBuilder
import android.tools.flicker.subject.exceptions.IncorrectRegionException
import android.tools.function.AssertionPredicate
import android.tools.io.Reader
import android.tools.traces.region.RegionEntry
import androidx.core.graphics.toRect
import kotlin.math.abs

/**
 * Subject for [Region] objects, used to make assertions over behaviors that occur on a rectangle.
 */
class RegionSubject
@JvmOverloads
constructor(
    val regionEntry: RegionEntry,
    override val timestamp: Timestamp,
    override val reader: Reader? = null,
) : FlickerSubject(), IRegionSubject {

    /** Custom constructor for existing android regions */
    @JvmOverloads
    constructor(
        region: Region?,
        timestamp: Timestamp,
        reader: Reader? = null,
    ) : this(RegionEntry(region ?: Region(), timestamp), timestamp, reader)

    /** Custom constructor for existing rects */
    @JvmOverloads
    constructor(
        rect: Rect?,
        timestamp: Timestamp,
        reader: Reader? = null,
    ) : this(Region(rect ?: Rect()), timestamp, reader)

    /** Custom constructor for existing rects */
    @JvmOverloads
    constructor(
        rect: RectF?,
        timestamp: Timestamp,
        reader: Reader? = null,
    ) : this(rect?.toRect(), timestamp, reader)

    /** Custom constructor for existing regions */
    @JvmOverloads
    constructor(
        regions: Collection<Region>,
        timestamp: Timestamp,
        reader: Reader? = null,
    ) : this(mergeRegions(regions), timestamp, reader)

    val region = regionEntry.region

    private val Rect.area
        get() = this.width() * this.height()

    /**
     * Asserts that the current [Region] doesn't contain layers
     *
     * @throws AssertionError
     */
    fun isEmpty(): RegionSubject = apply {
        if (!regionEntry.region.isEmpty) {
            val errorMsgBuilder =
                ExceptionMessageBuilder()
                    .forSubject(this)
                    .forIncorrectRegion("region")
                    .setExpected(Region())
                    .setActual(regionEntry.region)
            throw IncorrectRegionException(errorMsgBuilder)
        }
    }

    /**
     * Asserts that the current [Region] doesn't contain layers
     *
     * @throws AssertionError
     */
    fun isNotEmpty(): RegionSubject = apply {
        if (regionEntry.region.isEmpty) {
            val errorMsgBuilder =
                ExceptionMessageBuilder()
                    .forSubject(this)
                    .forIncorrectRegion("region")
                    .setExpected("Not empty")
                    .setActual(regionEntry.region)
            throw IncorrectRegionException(errorMsgBuilder)
        }
    }

    operator fun invoke(assertion: AssertionPredicate<Region>): RegionSubject = apply {
        assertion.verify(regionEntry.region)
    }

    /** Subtracts [other] from this subject [region] */
    fun minus(other: Region): RegionSubject {
        val remainingRegion = Region(this.region)
        remainingRegion.op(other, Region.Op.XOR)
        return RegionSubject(remainingRegion, timestamp, reader)
    }

    /** Adds [other] to this subject [region] */
    fun plus(other: Region): RegionSubject {
        val remainingRegion = Region(this.region)
        remainingRegion.op(other, Region.Op.UNION)
        return RegionSubject(remainingRegion, timestamp, reader)
    }

    /** See [isHigherOrEqual] */
    fun isHigherOrEqual(subject: RegionSubject): RegionSubject = isHigherOrEqual(subject.region)

    /** {@inheritDoc} */
    override fun isHigherOrEqual(other: Rect): RegionSubject = isHigherOrEqual(Region(other))

    /** {@inheritDoc} */
    override fun isHigherOrEqual(other: Region): RegionSubject = apply {
        assertLeftRightAndAreaEquals(other)
        assertCompare(
            name = "top position. Expected to be higher or equal",
            other,
            { it.top },
            { thisV, otherV -> thisV <= otherV },
        )
        assertCompare(
            name = "bottom position. Expected to be higher or equal",
            other,
            { it.bottom },
            { thisV, otherV -> thisV <= otherV },
        )
    }

    /** See [isLowerOrEqual] */
    fun isLowerOrEqual(subject: RegionSubject): RegionSubject = isLowerOrEqual(subject.region)

    /** {@inheritDoc} */
    override fun isLowerOrEqual(other: Rect): RegionSubject = isLowerOrEqual(Region(other))

    /** {@inheritDoc} */
    override fun isLowerOrEqual(other: Region): RegionSubject = apply {
        assertLeftRightAndAreaEquals(other)
        assertCompare(
            name = "top position. Expected to be lower or equal",
            other,
            { it.top },
            { thisV, otherV -> thisV >= otherV },
        )
        assertCompare(
            name = "bottom position. Expected to be lower or equal",
            other,
            { it.bottom },
            { thisV, otherV -> thisV >= otherV },
        )
    }

    /** {@inheritDoc} */
    override fun isToTheRight(other: Region): RegionSubject = apply {
        assertTopBottomAndAreaEquals(other)
        assertCompare(
            name = "left position. Expected to be lower or equal",
            other,
            { it.left },
            { thisV, otherV -> thisV >= otherV },
        )
        assertCompare(
            name = "right position. Expected to be lower or equal",
            other,
            { it.right },
            { thisV, otherV -> thisV >= otherV },
        )
    }

    /** See [isHigher] */
    fun isHigher(subject: RegionSubject): RegionSubject = isHigher(subject.region)

    /** {@inheritDoc} */
    override fun isHigher(other: Rect): RegionSubject = isHigher(Region(other))

    /** {@inheritDoc} */
    override fun isHigher(other: Region): RegionSubject = apply {
        assertLeftRightAndAreaEquals(other)
        assertCompare(
            name = "top position. Expected to be higher",
            other,
            { it.top },
            { thisV, otherV -> thisV < otherV },
        )
        assertCompare(
            name = "bottom position. Expected to be higher",
            other,
            { it.bottom },
            { thisV, otherV -> thisV < otherV },
        )
    }

    /** See [isLower] */
    fun isLower(subject: RegionSubject): RegionSubject = isLower(subject.region)

    /** {@inheritDoc} */
    override fun isLower(other: Rect): RegionSubject = isLower(Region(other))

    /**
     * Asserts that the top and bottom coordinates of [other] are greater than those of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     *
     * @throws IncorrectRegionException
     */
    override fun isLower(other: Region): RegionSubject = apply {
        assertLeftRightAndAreaEquals(other)
        assertCompare(
            name = "top position. Expected to be lower",
            other,
            { it.top },
            { thisV, otherV -> thisV > otherV },
        )
        assertCompare(
            name = "bottom position. Expected to be lower",
            other,
            { it.bottom },
            { thisV, otherV -> thisV > otherV },
        )
    }

    /** {@inheritDoc} */
    override fun coversAtMost(other: Region): RegionSubject = apply {
        if (!region.coversAtMost(other)) {
            val errorMsgBuilder =
                ExceptionMessageBuilder()
                    .forSubject(this)
                    .forIncorrectRegion("region. $region should cover at most $other")
                    .setExpected(other)
                    .setActual(regionEntry.region)
                    .addExtraDescription("Out-of-bounds region", region.outOfBoundsRegion(other))
            throw IncorrectRegionException(errorMsgBuilder)
        }
    }

    /** {@inheritDoc} */
    override fun coversAtMost(other: Rect): RegionSubject = coversAtMost(Region(other))

    /** {@inheritDoc} */
    override fun notBiggerThan(other: Region): RegionSubject = apply {
        val testArea = other.bounds.area
        val area = region.bounds.area

        if (area > testArea) {
            val errorMsgBuilder =
                ExceptionMessageBuilder()
                    .forSubject(this)
                    .forIncorrectRegion("region. $region area should not be bigger than $testArea")
                    .setExpected(testArea)
                    .setActual(area)
                    .addExtraDescription("Expected region", other)
                    .addExtraDescription("Actual region", regionEntry.region)
            throw IncorrectRegionException(errorMsgBuilder)
        }
    }

    /** {@inheritDoc} */
    override fun notSmallerThan(other: Region): RegionSubject = apply {
        val testArea = other.bounds.area
        val area = region.bounds.area

        if (area < testArea) {
            val errorMsgBuilder =
                ExceptionMessageBuilder()
                    .forSubject(this)
                    .forIncorrectRegion("region. $region area should not be smaller than $testArea")
                    .setExpected(testArea)
                    .setActual(area)
                    .addExtraDescription("Expected region", other)
                    .addExtraDescription("Actual region", regionEntry.region)
            throw IncorrectRegionException(errorMsgBuilder)
        }
    }

    /** {@inheritDoc} */
    override fun isToTheRightBottom(other: Region, threshold: Int): RegionSubject = apply {
        val horizontallyPositionedToTheRight = other.bounds.left - threshold <= region.bounds.left
        val verticallyPositionedToTheBottom = other.bounds.top - threshold <= region.bounds.top

        if (!horizontallyPositionedToTheRight || !verticallyPositionedToTheBottom) {
            val errorMsgBuilder =
                ExceptionMessageBuilder()
                    .forSubject(this)
                    .forIncorrectRegion(
                        "region. $region area should be to the right bottom of $other"
                    )
                    .setExpected(other)
                    .setActual(regionEntry.region)
                    .addExtraDescription("Threshold", threshold)
                    .addExtraDescription(
                        "Horizontally positioned to the right",
                        horizontallyPositionedToTheRight,
                    )
                    .addExtraDescription(
                        "Vertically positioned to the bottom",
                        verticallyPositionedToTheBottom,
                    )
            throw IncorrectRegionException(errorMsgBuilder)
        }
    }

    /** {@inheritDoc} */
    override fun isLeftEdgeToTheRight(other: Region): RegionSubject = apply {
        val horizontallyPositionedToTheRight = other.bounds.left <= region.bounds.left
        if (!horizontallyPositionedToTheRight) {
            val errorMsgBuilder =
                ExceptionMessageBuilder()
                    .forSubject(this)
                    .forIncorrectRegion(
                        "region. left edge of $region area should be to the right of $other"
                    )
                    .setExpected(other)
                    .setActual(regionEntry.region)
                    .addExtraDescription(
                        "Horizontally positioned to the right",
                        horizontallyPositionedToTheRight,
                    )
            throw IncorrectRegionException(errorMsgBuilder)
        }
    }

    /** {@inheritDoc} */
    override fun regionsCenterPointInside(other: Rect): RegionSubject = apply {
        if (!other.contains(region.bounds.centerX(), region.bounds.centerY())) {
            val center = Point(region.bounds.centerX(), region.bounds.centerY())
            val errorMsgBuilder =
                ExceptionMessageBuilder()
                    .forSubject(this)
                    .forIncorrectRegion("region. $region center point should be inside $other")
                    .setExpected(other)
                    .setActual(regionEntry.region)
                    .addExtraDescription("Center point", center)
            throw IncorrectRegionException(errorMsgBuilder)
        }
    }

    /** {@inheritDoc} */
    override fun coversAtLeast(other: Region): RegionSubject = apply {
        if (!region.coversAtLeast(other)) {
            val errorMsgBuilder =
                ExceptionMessageBuilder()
                    .forSubject(this)
                    .forIncorrectRegion("region. $region should cover at least $other")
                    .setExpected(other)
                    .setActual(regionEntry.region)
                    .addExtraDescription("Uncovered region", region.uncoveredRegion(other))
            throw IncorrectRegionException(errorMsgBuilder)
        }
    }

    /** {@inheritDoc} */
    override fun coversAtLeast(other: Rect): RegionSubject = coversAtLeast(Region(other))

    /** {@inheritDoc} */
    override fun coversExactly(other: Region): RegionSubject = apply {
        val intersection = Region(region)
        val isNotEmpty = intersection.op(other, Region.Op.XOR)

        if (isNotEmpty) {
            val errorMsgBuilder =
                ExceptionMessageBuilder()
                    .forSubject(this)
                    .forIncorrectRegion("region. $region should cover exactly $other")
                    .setExpected(other)
                    .setActual(regionEntry.region)
                    .addExtraDescription("Difference", intersection)
            throw IncorrectRegionException(errorMsgBuilder)
        }
    }

    /** {@inheritDoc} */
    override fun coversExactly(other: Rect): RegionSubject = coversExactly(Region(other))

    /** {@inheritDoc} */
    override fun overlaps(other: Region): RegionSubject = apply {
        val intersection = Region(region)
        val isEmpty = !intersection.op(other, Region.Op.INTERSECT)

        if (isEmpty) {
            val errorMsgBuilder =
                ExceptionMessageBuilder()
                    .forSubject(this)
                    .forIncorrectRegion("region. $region should overlap with $other")
                    .setExpected(other)
                    .setActual(regionEntry.region)
            throw IncorrectRegionException(errorMsgBuilder)
        }
    }

    /** {@inheritDoc} */
    override fun overlaps(other: Rect): RegionSubject = overlaps(Region(other))

    /** {@inheritDoc} */
    override fun notOverlaps(other: Region): RegionSubject = apply {
        val intersection = Region(region)
        val isEmpty = !intersection.op(other, Region.Op.INTERSECT)

        if (!isEmpty) {
            val errorMsgBuilder =
                ExceptionMessageBuilder()
                    .forSubject(this)
                    .forIncorrectRegion("region. $region should not overlap with $other")
                    .setExpected(other)
                    .setActual(regionEntry.region)
                    .addExtraDescription("Overlap region", intersection)
            throw IncorrectRegionException(errorMsgBuilder)
        }
    }

    /** {@inheritDoc} */
    override fun notOverlaps(other: Rect): RegionSubject = apply { notOverlaps(Region(other)) }

    /** {@inheritDoc} */
    override fun isSameAspectRatio(other: Region, threshold: Double): RegionSubject = apply {
        val thisBounds = this.region.bounds
        val otherBounds = other.bounds
        val aspectRatio = thisBounds.width().toFloat() / thisBounds.height()
        val otherAspectRatio = otherBounds.width().toFloat() / otherBounds.height()
        if (abs(aspectRatio - otherAspectRatio) > threshold) {
            val errorMsgBuilder =
                ExceptionMessageBuilder()
                    .forSubject(this)
                    .forIncorrectRegion(
                        "region. $region should have the same aspect ratio as $other"
                    )
                    .setExpected(other)
                    .setActual(regionEntry.region)
                    .addExtraDescription("Threshold", threshold)
                    .addExtraDescription("Region aspect ratio", aspectRatio)
                    .addExtraDescription("Other aspect ratio", otherAspectRatio)
            throw IncorrectRegionException(errorMsgBuilder)
        }
    }

    /** {@inheritDoc} */
    override fun hasSameBottomPosition(displayRect: Rect): RegionSubject = apply {
        assertEquals("bottom", Region(displayRect)) { it.bottom }
    }

    /** {@inheritDoc} */
    override fun hasSameTopPosition(displayRect: Rect): RegionSubject = apply {
        assertEquals("top", Region(displayRect)) { it.top }
    }

    override fun hasSameLeftPosition(displayRect: Rect): RegionSubject = apply {
        assertEquals("left", Region(displayRect)) { it.left }
    }

    override fun hasSameRightPosition(displayRect: Rect): RegionSubject = apply {
        assertEquals("right", Region(displayRect)) { it.right }
    }

    fun isSameAspectRatio(other: RegionSubject, threshold: Double = 0.1): RegionSubject =
        isSameAspectRatio(other.region, threshold)

    fun isSameAspectRatio(
        numerator: Int,
        denominator: Int,
        threshold: Double = 0.1,
    ): RegionSubject {
        val region = Region()
        region.set(Rect(0, 0, numerator, denominator))
        return isSameAspectRatio(region, threshold)
    }

    private fun <T : Comparable<T>> assertCompare(
        name: String,
        other: Region,
        valueProvider: (Rect) -> T,
        boundsCheck: (T, T) -> Boolean,
    ) {
        val thisValue = valueProvider(region.bounds)
        val otherValue = valueProvider(other.bounds)
        if (!boundsCheck(thisValue, otherValue)) {
            val errorMsgBuilder =
                ExceptionMessageBuilder()
                    .forSubject(this)
                    .forIncorrectRegion(name)
                    .setExpected(otherValue.toString())
                    .setActual(thisValue.toString())
                    .addExtraDescription("Actual region", region)
                    .addExtraDescription("Expected region", other)
            throw IncorrectRegionException(errorMsgBuilder)
        }
    }

    private fun <T : Comparable<T>> assertEquals(
        name: String,
        other: Region,
        valueProvider: (Rect) -> T,
    ) = assertCompare(name, other, valueProvider) { thisV, otherV -> thisV == otherV }

    private fun assertLeftRightAndAreaEquals(other: Region) {
        assertEquals("left", other) { it.left }
        assertEquals("right", other) { it.right }
        assertEquals("area", other) { it.area }
    }

    private fun assertTopBottomAndAreaEquals(other: Region) {
        assertEquals("top", other) { it.top }
        assertEquals("bottom", other) { it.bottom }
        assertEquals("area", other) { it.area }
    }

    companion object {
        private fun mergeRegions(regions: Collection<Region>): Region {
            val result = Region()
            regions.forEach { region -> result.op(region, Region.Op.UNION) }
            return result
        }
    }
}
