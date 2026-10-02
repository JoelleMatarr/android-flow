package com.example.flow22

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import com.checkout.components.interfaces.uicustomisation.BorderRadius
import com.checkout.components.interfaces.uicustomisation.designtoken.ColorTokens
import com.checkout.components.interfaces.uicustomisation.designtoken.DesignTokens
import com.checkout.components.interfaces.uicustomisation.font.Font
import com.checkout.components.interfaces.uicustomisation.font.FontFamily
import com.checkout.components.interfaces.uicustomisation.font.FontName
import com.checkout.components.interfaces.uicustomisation.font.FontStyle
import com.checkout.components.interfaces.uicustomisation.font.FontWeight

/**
 * The SDK has no dark-mode helper of its own, so this resolves the current system setting.
 */
object ThemeUtils {
    fun isCurrentDark(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
}

/**
 * Merchant-supplied Flow design tokens, passed to the SDK as
 * `CheckoutComponentConfiguration(appearance = ...)`.
 *
 * Colors are plain ARGB ints widened to Long. `Int.toLong()` sign-extends for any opaque
 * color, but the SDK hands the value to Compose's `Color(Long)`, which does `(color shl 32)`
 * and discards the high 32 bits — so the sign extension is harmless and no masking is needed.
 */
class CheckoutAppearance(private val context: Context) {

    fun createDesignTokens(): DesignTokens {
        val isDark = ThemeUtils.isCurrentDark(context)
        return if (isDark) {
            createDarkTheme()
        } else {
            createLightTheme()
        }
    }

    private fun createDarkTheme(): DesignTokens {
        val colorTokens = ColorTokens(
            colorAction = context.getColor(R.color.color_0ba5ec).toLong(),
            colorBackground = context.getColor(R.color.color_E2E4E9).toLong(),
            colorBorder = context.getColor(R.color.color_9c9da1).toLong(),
            colorDisabled = Color.WHITE.toLong(),
            colorError = context.getColor(R.color.color_DF1C41).toLong(),
            colorFormBackground = context.getColor(R.color.color_041036).toLong(),
            colorFormBorder = Color.WHITE.toLong(),
            colorInverse = Color.WHITE.toLong(),
            colorOutline = Color.RED.toLong(),
            colorPrimary = Color.WHITE.toLong(),
            colorSecondary = context.getColor(R.color.color_868C98).toLong()
        )

        return DesignTokens(
            colorTokens = colorTokens,
            fonts = createFonts(),
            borderFormRadius = FORM_RADIUS,
            borderButtonRadius = BUTTON_RADIUS
        )
    }

    private fun createLightTheme(): DesignTokens {
        val colorTokens = ColorTokens(
            colorAction = context.getColor(R.color.color_fc4f00).toLong(),
            colorBackground = context.getColor(R.color.color_E2E4E9).toLong(),
            colorBorder = context.getColor(R.color.color_9c9da1).toLong(),
            colorDisabled = context.getColor(R.color.color_0A0D14).toLong(),
            colorError = context.getColor(R.color.color_DF1C41).toLong(),
            colorFormBackground = Color.WHITE.toLong(),
            colorFormBorder = context.getColor(R.color.color_9c9da1).toLong(),
            colorInverse = Color.WHITE.toLong(),
            colorOutline = Color.RED.toLong(),
            colorPrimary = context.getColor(R.color.color_0A0D14).toLong(),
            colorSecondary = context.getColor(R.color.color_868C98).toLong()
        )

        return DesignTokens(
            colorTokens = colorTokens,
            fonts = createFonts(),
            borderFormRadius = FORM_RADIUS,
            borderButtonRadius = BUTTON_RADIUS
        )
    }

    /** Identical in both themes in the merchant's spec, so shared. */
    private fun createFonts(): Map<FontName, Font> = mapOf(
        FontName.Button to Font(
            fontFamily = FontFamily.Default,
            fontStyle = FontStyle.Normal,
            fontWeight = FontWeight.Bold,
            fontSize = 16,
            lineHeight = 20,
            letterSpacing = 1
        ),
        FontName.Input to Font(
            fontFamily = FontFamily.Default,
            fontStyle = FontStyle.Normal,
            fontWeight = FontWeight.Bold,
            fontSize = 16,
            lineHeight = 20,
            letterSpacing = 1
        ),
        FontName.Label to Font(
            fontFamily = FontFamily.Default,
            fontStyle = FontStyle.Normal,
            fontWeight = FontWeight.Bold,
            fontSize = 16,
            lineHeight = 20,
            letterSpacing = 1
        ),
        FontName.Subheading to Font(
            fontFamily = FontFamily.Default,
            fontStyle = FontStyle.Normal,
            fontWeight = FontWeight.Light,
            fontSize = 16,
            lineHeight = 20,
            letterSpacing = 1
        )
    )

    private companion object {
        val FORM_RADIUS =
            BorderRadius(topStart = 20, topEnd = 20, bottomStart = 20, bottomEnd = 20)
        val BUTTON_RADIUS =
            BorderRadius(topStart = 40, topEnd = 40, bottomStart = 40, bottomEnd = 40)
    }
}
