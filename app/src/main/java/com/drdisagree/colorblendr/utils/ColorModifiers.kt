package com.drdisagree.colorblendr.utils

import com.drdisagree.colorblendr.data.config.Prefs
import com.drdisagree.colorblendr.data.enums.MONET
import com.drdisagree.colorblendr.utils.ColorUtil.getAdjustedChroma
import com.drdisagree.colorblendr.utils.ColorUtil.getAdjustedLightness
import com.drdisagree.colorblendr.utils.ColorUtil.systemPaletteNames
import com.drdisagree.colorblendr.utils.cam.HctSolver
import com.drdisagree.colorblendr.utils.monet.palettes.TonalPalette
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

object ColorModifiers {
    private val tones: DoubleArray = doubleArrayOf(99.0, 95.0, 90.0, 80.0, 70.0, 60.0, 49.6, 40.0, 30.0, 20.0, 10.0, 0.0)

    fun generateShades(hue: Float, chroma: Float): ArrayList<Int> {
        val shadeList = ArrayList(List(12) { 0 })

        shadeList[0] = ColorUtil.CAMToColor(
            hue,
            min(40.0, chroma.toDouble()).toFloat(), 99.0f
        )
        shadeList[1] = ColorUtil.CAMToColor(
            hue,
            min(40.0, chroma.toDouble()).toFloat(), 95.0f
        )

        for (i in 2..11) {
            val lstar = if (i == 6) {
                49.6f
            } else {
                (100 - ((i - 1) * 10)).toFloat()
            }
            shadeList[i] = ColorUtil.CAMToColor(hue, chroma, lstar)
        }

        return shadeList
    }

    fun modifyColors(
        palette: TonalPalette,
        counter: AtomicInteger,
        style: MONET,
        monetAccentSaturation: Int,
        monetBackgroundSaturation: Int,
        monetBackgroundLightness: Int,
        pitchBlackTheme: Boolean,
        accurateShades: Boolean,
        modifyPitchBlack: Boolean,
        overrideColors: Boolean = true
    ): ArrayList<Int> {
        counter.getAndIncrement()

        val accentPalette = counter.get() <= 3

        val accentSaturation = monetAccentSaturation != 100
        val backgroundSaturation = monetBackgroundSaturation != 100
        val backgroundLightness = monetBackgroundLightness != 100

        val isMonochrome = style == MONET.MONOCHROMATIC
        val isRainbow = style == MONET.RAINBOW

        val outChromas = ArrayList(List(tones.size) { palette.chroma })
        val outTones = ArrayList(tones.asList())

        if (accentPalette) {
            if (accentSaturation && !isMonochrome) {
                // Set accent saturation
                for (j in outChromas.indices) {
                    outChromas[j] = getAdjustedChroma(palette, outTones[j], monetAccentSaturation)
                }
            }
        } else {
            if (backgroundLightness && !isMonochrome) {
                // Set background lightness
                for (j in outTones.indices) {
                    outTones[j] = getAdjustedLightness(monetBackgroundLightness, j + 1)
                }
            }

            if (backgroundSaturation && !isMonochrome && !isRainbow) {
                // Set background saturation
                for (j in outChromas.indices) {
                    outChromas[j] = getAdjustedChroma(palette, outTones[j], monetBackgroundSaturation)
                }
            }
        }

        if (isMonochrome) {
            // Set monochrome lightness
            for (j in outTones.indices) {
                outTones[j] = getAdjustedLightness(monetBackgroundLightness, j + 1)
            }
        }

        if (!accentPalette) {
            if (pitchBlackTheme && modifyPitchBlack) {
                // Set pitch black theme
                outChromas[10] = 0.0
                outTones[10] = 0.0
            }
        }

        // Limiting chroma for lightness 99 and 95
        outChromas[0] = outChromas[0].coerceAtMost(40.0)
        outChromas[1] = outChromas[1].coerceAtMost(40.0)

        val colors = ArrayList<Int>()

        for (j in tones.indices) {
            colors.add(HctSolver.solveToInt(palette.hue, outChromas[j], outTones[j]))
        }

        if (overrideColors) {
            for (j in 0 until colors.size - 1) {
                val i = counter.get() - 1

                val overriddenColor = Prefs.getInt(systemPaletteNames[i][j + 1], Int.MIN_VALUE)

                if (overriddenColor != Int.MIN_VALUE) {
                    colors[j] = overriddenColor
                } else if (!accurateShades && (i in 0..2) && j == 2) {
                    colors[j] = colors[j + 2]
                }
            }
        }

        if (counter.get() >= 5) {
            counter.set(0)
        }

        return colors
    }

    fun <K, V> zipToMap(keys: List<K>, values: List<V>): Map<K, V> {
        val result: MutableMap<K, V> = HashMap()

        require(keys.size == values.size) { "Lists must have the same size. Provided keys size: " + keys.size + " Provided values size: " + values.size + "." }

        for (i in keys.indices) {
            result[keys[i]] = values[i]
        }

        return result
    }
}
