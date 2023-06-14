package com.simplemobiletools.commons.extensions

import android.app.Activity
import android.graphics.Color
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.helpers.DARK_GREY

fun Activity.getThemeId(color: Int = baseConfig.primaryColor, showTransparentTop: Boolean = false) = when {
    baseConfig.isUsingSystemTheme -> if (isUsingSystemDarkTheme()) R.style.AppTheme_Base_System else R.style.AppTheme_Base_System_Light
    isBlackAndWhiteTheme() -> when {
        showTransparentTop -> R.style.AppTheme_BlackAndWhite_NoActionBar
        baseConfig.primaryColor.getContrastColor() == DARK_GREY -> R.style.AppTheme_BlackAndWhite_DarkTextColor
        else -> R.style.AppTheme_BlackAndWhite
    }
    isWhiteTheme() -> when {
        showTransparentTop -> R.style.AppTheme_White_NoActionBar
        baseConfig.primaryColor.getContrastColor() == Color.WHITE -> R.style.AppTheme_White_LightTextColor
        else -> R.style.AppTheme_White
    }
    showTransparentTop -> {
        when (color) {
            -12846 -> R.style.AppTheme_Red_100_core
            -1074534 -> R.style.AppTheme_Red_200_core
            -1739917 -> R.style.AppTheme_Red_300_core
            -1092784 -> R.style.AppTheme_Red_400_core
            -769226 -> R.style.AppTheme_Red_500_core
            -1754827 -> R.style.AppTheme_Red_600_core
            -2937041 -> R.style.AppTheme_Red_700_core
            -3790808 -> R.style.AppTheme_Red_800_core
            -4776932 -> R.style.AppTheme_Red_900_core

            -476208 -> R.style.AppTheme_Pink_100_core
            -749647 -> R.style.AppTheme_Pink_200_core
            -1023342 -> R.style.AppTheme_Pink_300_core
            -1294214 -> R.style.AppTheme_Pink_400_core
            -1499549 -> R.style.AppTheme_Pink_500_core
            -2614432 -> R.style.AppTheme_Pink_600_core
            -4056997 -> R.style.AppTheme_Pink_700_core
            -5434281 -> R.style.AppTheme_Pink_800_core
            -7860657 -> R.style.AppTheme_Pink_900_core

            -1982745 -> R.style.AppTheme_Purple_100_core
            -3238952 -> R.style.AppTheme_Purple_200_core
            -4560696 -> R.style.AppTheme_Purple_300_core
            -5552196 -> R.style.AppTheme_Purple_400_core
            -6543440 -> R.style.AppTheme_Purple_500_core
            -7461718 -> R.style.AppTheme_Purple_600_core
            -8708190 -> R.style.AppTheme_Purple_700_core
            -9823334 -> R.style.AppTheme_Purple_800_core
            -11922292 -> R.style.AppTheme_Purple_900_core

            -3029783 -> R.style.AppTheme_Deep_Purple_100_core
            -5005861 -> R.style.AppTheme_Deep_Purple_200_core
            -6982195 -> R.style.AppTheme_Deep_Purple_300_core
            -8497214 -> R.style.AppTheme_Deep_Purple_400_core
            -10011977 -> R.style.AppTheme_Deep_Purple_500_core
            -10603087 -> R.style.AppTheme_Deep_Purple_600_core
            -11457112 -> R.style.AppTheme_Deep_Purple_700_core
            -12245088 -> R.style.AppTheme_Deep_Purple_800_core
            -13558894 -> R.style.AppTheme_Deep_Purple_900_core

            -3814679 -> R.style.AppTheme_Indigo_100_core
            -6313766 -> R.style.AppTheme_Indigo_200_core
            -8812853 -> R.style.AppTheme_Indigo_300_core
            -10720320 -> R.style.AppTheme_Indigo_400_core
            -12627531 -> R.style.AppTheme_Indigo_500_core
            -13022805 -> R.style.AppTheme_Indigo_600_core
            -13615201 -> R.style.AppTheme_Indigo_700_core
            -14142061 -> R.style.AppTheme_Indigo_800_core
            -15064194 -> R.style.AppTheme_Indigo_900_core

            -4464901 -> R.style.AppTheme_Blue_100_core
            -7288071 -> R.style.AppTheme_Blue_200_core
            -10177034 -> R.style.AppTheme_Blue_300_core
            -12409355 -> R.style.AppTheme_Blue_400_core
            -14575885 -> R.style.AppTheme_Blue_500_core
            -14776091 -> R.style.AppTheme_Blue_600_core
            -15108398 -> R.style.AppTheme_Blue_700_core
            -15374912 -> R.style.AppTheme_Blue_800_core
            -15906911 -> R.style.AppTheme_Blue_900_core

            -4987396 -> R.style.AppTheme_Light_Blue_100_core
            -8268550 -> R.style.AppTheme_Light_Blue_200_core
            -11549705 -> R.style.AppTheme_Light_Blue_300_core
            -14043396 -> R.style.AppTheme_Light_Blue_400_core
            -16537100 -> R.style.AppTheme_Light_Blue_500_core
            -16540699 -> R.style.AppTheme_Light_Blue_600_core
            -16611119 -> R.style.AppTheme_Light_Blue_700_core
            -16615491 -> R.style.AppTheme_Light_Blue_800_core
            -16689253 -> R.style.AppTheme_Light_Blue_900_core

            -5051406 -> R.style.AppTheme_Cyan_100_core
            -8331542 -> R.style.AppTheme_Cyan_200_core
            -11677471 -> R.style.AppTheme_Cyan_300_core
            -14235942 -> R.style.AppTheme_Cyan_400_core
            -16728876 -> R.style.AppTheme_Cyan_500_core
            -16732991 -> R.style.AppTheme_Cyan_600_core
            -16738393 -> R.style.AppTheme_Cyan_700_core
            -16743537 -> R.style.AppTheme_Cyan_800_core
            -16752540 -> R.style.AppTheme_Cyan_900_core

            -5054501 -> R.style.AppTheme_Teal_100_core
            -8336444 -> R.style.AppTheme_Teal_200_core
            -11684180 -> R.style.AppTheme_Teal_300_core
            -14244198 -> R.style.AppTheme_Teal_400_core
            -16738680 -> R.style.AppTheme_Teal_500_core
            -16742021 -> R.style.AppTheme_Teal_600_core
            -16746133 -> R.style.AppTheme_Teal_700_core
            -16750244 -> R.style.AppTheme_Teal_800_core
            -16757440 -> R.style.AppTheme_Teal_900_core

            -3610935 -> R.style.AppTheme_Green_100_core
            -5908825 -> R.style.AppTheme_Green_200_core
            -8271996 -> R.style.AppTheme_Green_300_core
            -10044566 -> R.style.AppTheme_Green_400_core
            -11751600 -> R.style.AppTheme_Green_500_core
            -12345273 -> R.style.AppTheme_Green_600_core
            -13070788 -> R.style.AppTheme_Green_700_core
            -13730510 -> R.style.AppTheme_Green_800_core
            -14983648 -> R.style.AppTheme_Green_900_core

            -2298424 -> R.style.AppTheme_Light_Green_100_core
            -3808859 -> R.style.AppTheme_Light_Green_200_core
            -5319295 -> R.style.AppTheme_Light_Green_300_core
            -6501275 -> R.style.AppTheme_Light_Green_400_core
            -7617718 -> R.style.AppTheme_Light_Green_500_core
            -8604862 -> R.style.AppTheme_Light_Green_600_core
            -9920712 -> R.style.AppTheme_Light_Green_700_core
            -11171025 -> R.style.AppTheme_Light_Green_800_core
            -13407970 -> R.style.AppTheme_Light_Green_900_core

            -985917 -> R.style.AppTheme_Lime_100_core
            -1642852 -> R.style.AppTheme_Lime_200_core
            -2300043 -> R.style.AppTheme_Lime_300_core
            -2825897 -> R.style.AppTheme_Lime_400_core
            -3285959 -> R.style.AppTheme_Lime_500_core
            -4142541 -> R.style.AppTheme_Lime_600_core
            -5983189 -> R.style.AppTheme_Lime_700_core
            -6382300 -> R.style.AppTheme_Lime_800_core
            -8227049 -> R.style.AppTheme_Lime_900_core

            -1596 -> R.style.AppTheme_Yellow_100_core
            -2672 -> R.style.AppTheme_Yellow_200_core
            -3722 -> R.style.AppTheme_Yellow_300_core
            -4520 -> R.style.AppTheme_Yellow_400_core
            -5317 -> R.style.AppTheme_Yellow_500_core
            -141259 -> R.style.AppTheme_Yellow_600_core
            -278483 -> R.style.AppTheme_Yellow_700_core
            -415707 -> R.style.AppTheme_Yellow_800_core
            -688361 -> R.style.AppTheme_Yellow_900_core

            -4941 -> R.style.AppTheme_Amber_100_core
            -8062 -> R.style.AppTheme_Amber_200_core
            -10929 -> R.style.AppTheme_Amber_300_core
            -13784 -> R.style.AppTheme_Amber_400_core
            -16121 -> R.style.AppTheme_Amber_500_core
            -19712 -> R.style.AppTheme_Amber_600_core
            -24576 -> R.style.AppTheme_Amber_700_core
            -28928 -> R.style.AppTheme_Amber_800_core
            -37120 -> R.style.AppTheme_Amber_900_core

            -8014 -> R.style.AppTheme_Orange_100_core
            -13184 -> R.style.AppTheme_Orange_200_core
            -18611 -> R.style.AppTheme_Orange_300_core
            -22746 -> R.style.AppTheme_Orange_400_core
            -26624 -> R.style.AppTheme_Orange_500_core
            -291840 -> R.style.AppTheme_Orange_600_core
            -689152 -> R.style.AppTheme_Orange_700_core
            -1086464 -> R.style.AppTheme_Orange_800_core
            -1683200 -> R.style.AppTheme_Orange_900_core

            -13124 -> R.style.AppTheme_Deep_Orange_100_core
            -21615 -> R.style.AppTheme_Deep_Orange_200_core
            -30107 -> R.style.AppTheme_Deep_Orange_300_core
            -36797 -> R.style.AppTheme_Deep_Orange_400_core
            -43230 -> R.style.AppTheme_Deep_Orange_500_core
            -765666 -> R.style.AppTheme_Deep_Orange_600_core
            -1684967 -> R.style.AppTheme_Deep_Orange_700_core
            -2604267 -> R.style.AppTheme_Deep_Orange_800_core
            -4246004 -> R.style.AppTheme_Deep_Orange_900_core

            -2634552 -> R.style.AppTheme_Brown_100_core
            -4412764 -> R.style.AppTheme_Brown_200_core
            -6190977 -> R.style.AppTheme_Brown_300_core
            -7508381 -> R.style.AppTheme_Brown_400_core
            -8825528 -> R.style.AppTheme_Brown_500_core
            -9614271 -> R.style.AppTheme_Brown_600_core
            -10665929 -> R.style.AppTheme_Brown_700_core
            -11652050 -> R.style.AppTheme_Brown_800_core
            -12703965 -> R.style.AppTheme_Brown_900_core

            -3155748 -> R.style.AppTheme_Blue_Grey_100_core
            -5194811 -> R.style.AppTheme_Blue_Grey_200_core
            -7297874 -> R.style.AppTheme_Blue_Grey_300_core
            -8875876 -> R.style.AppTheme_Blue_Grey_400_core
            -10453621 -> R.style.AppTheme_Blue_Grey_500_core
            -11243910 -> R.style.AppTheme_Blue_Grey_600_core
            -12232092 -> R.style.AppTheme_Blue_Grey_700_core
            -13154481 -> R.style.AppTheme_Blue_Grey_800_core
            -14273992 -> R.style.AppTheme_Blue_Grey_900_core

            -1 -> R.style.AppTheme_Grey_100_core
            -1118482 -> R.style.AppTheme_Grey_200_core
            -2039584 -> R.style.AppTheme_Grey_300_core
            -4342339 -> R.style.AppTheme_Grey_400_core
            -6381922 -> R.style.AppTheme_Grey_500_core
            -9079435 -> R.style.AppTheme_Grey_600_core
            -10395295 -> R.style.AppTheme_Grey_700_core
            -12434878 -> R.style.AppTheme_Grey_800_core
            -16777216 -> R.style.AppTheme_Grey_900_core

            else -> R.style.AppTheme_Orange_700_core
        }
    }
    else -> {
        when (color) {
            -12846 -> R.style.AppTheme_Red_100
            -1074534 -> R.style.AppTheme_Red_200
            -1739917 -> R.style.AppTheme_Red_300
            -1092784 -> R.style.AppTheme_Red_400
            -769226 -> R.style.AppTheme_Red_500
            -1754827 -> R.style.AppTheme_Red_600
            -2937041 -> R.style.AppTheme_Red_700
            -3790808 -> R.style.AppTheme_Red_800
            -4776932 -> R.style.AppTheme_Red_900

            -476208 -> R.style.AppTheme_Pink_100
            -749647 -> R.style.AppTheme_Pink_200
            -1023342 -> R.style.AppTheme_Pink_300
            -1294214 -> R.style.AppTheme_Pink_400
            -1499549 -> R.style.AppTheme_Pink_500
            -2614432 -> R.style.AppTheme_Pink_600
            -4056997 -> R.style.AppTheme_Pink_700
            -5434281 -> R.style.AppTheme_Pink_800
            -7860657 -> R.style.AppTheme_Pink_900

            -1982745 -> R.style.AppTheme_Purple_100
            -3238952 -> R.style.AppTheme_Purple_200
            -4560696 -> R.style.AppTheme_Purple_300
            -5552196 -> R.style.AppTheme_Purple_400
            -6543440 -> R.style.AppTheme_Purple_500
            -7461718 -> R.style.AppTheme_Purple_600
            -8708190 -> R.style.AppTheme_Purple_700
            -9823334 -> R.style.AppTheme_Purple_800
            -11922292 -> R.style.AppTheme_Purple_900

            -3029783 -> R.style.AppTheme_Deep_Purple_100
            -5005861 -> R.style.AppTheme_Deep_Purple_200
            -6982195 -> R.style.AppTheme_Deep_Purple_300
            -8497214 -> R.style.AppTheme_Deep_Purple_400
            -10011977 -> R.style.AppTheme_Deep_Purple_500
            -10603087 -> R.style.AppTheme_Deep_Purple_600
            -11457112 -> R.style.AppTheme_Deep_Purple_700
            -12245088 -> R.style.AppTheme_Deep_Purple_800
            -13558894 -> R.style.AppTheme_Deep_Purple_900

            -3814679 -> R.style.AppTheme_Indigo_100
            -6313766 -> R.style.AppTheme_Indigo_200
            -8812853 -> R.style.AppTheme_Indigo_300
            -10720320 -> R.style.AppTheme_Indigo_400
            -12627531 -> R.style.AppTheme_Indigo_500
            -13022805 -> R.style.AppTheme_Indigo_600
            -13615201 -> R.style.AppTheme_Indigo_700
            -14142061 -> R.style.AppTheme_Indigo_800
            -15064194 -> R.style.AppTheme_Indigo_900

            -4464901 -> R.style.AppTheme_Blue_100
            -7288071 -> R.style.AppTheme_Blue_200
            -10177034 -> R.style.AppTheme_Blue_300
            -12409355 -> R.style.AppTheme_Blue_400
            -14575885 -> R.style.AppTheme_Blue_500
            -14776091 -> R.style.AppTheme_Blue_600
            -15108398 -> R.style.AppTheme_Blue_700
            -15374912 -> R.style.AppTheme_Blue_800
            -15906911 -> R.style.AppTheme_Blue_900

            -4987396 -> R.style.AppTheme_Light_Blue_100
            -8268550 -> R.style.AppTheme_Light_Blue_200
            -11549705 -> R.style.AppTheme_Light_Blue_300
            -14043396 -> R.style.AppTheme_Light_Blue_400
            -16537100 -> R.style.AppTheme_Light_Blue_500
            -16540699 -> R.style.AppTheme_Light_Blue_600
            -16611119 -> R.style.AppTheme_Light_Blue_700
            -16615491 -> R.style.AppTheme_Light_Blue_800
            -16689253 -> R.style.AppTheme_Light_Blue_900

            -5051406 -> R.style.AppTheme_Cyan_100
            -8331542 -> R.style.AppTheme_Cyan_200
            -11677471 -> R.style.AppTheme_Cyan_300
            -14235942 -> R.style.AppTheme_Cyan_400
            -16728876 -> R.style.AppTheme_Cyan_500
            -16732991 -> R.style.AppTheme_Cyan_600
            -16738393 -> R.style.AppTheme_Cyan_700
            -16743537 -> R.style.AppTheme_Cyan_800
            -16752540 -> R.style.AppTheme_Cyan_900

            -5054501 -> R.style.AppTheme_Teal_100
            -8336444 -> R.style.AppTheme_Teal_200
            -11684180 -> R.style.AppTheme_Teal_300
            -14244198 -> R.style.AppTheme_Teal_400
            -16738680 -> R.style.AppTheme_Teal_500
            -16742021 -> R.style.AppTheme_Teal_600
            -16746133 -> R.style.AppTheme_Teal_700
            -16750244 -> R.style.AppTheme_Teal_800
            -16757440 -> R.style.AppTheme_Teal_900

            -3610935 -> R.style.AppTheme_Green_100
            -5908825 -> R.style.AppTheme_Green_200
            -8271996 -> R.style.AppTheme_Green_300
            -10044566 -> R.style.AppTheme_Green_400
            -11751600 -> R.style.AppTheme_Green_500
            -12345273 -> R.style.AppTheme_Green_600
            -13070788 -> R.style.AppTheme_Green_700
            -13730510 -> R.style.AppTheme_Green_800
            -14983648 -> R.style.AppTheme_Green_900

            -2298424 -> R.style.AppTheme_Light_Green_100
            -3808859 -> R.style.AppTheme_Light_Green_200
            -5319295 -> R.style.AppTheme_Light_Green_300
            -6501275 -> R.style.AppTheme_Light_Green_400
            -7617718 -> R.style.AppTheme_Light_Green_500
            -8604862 -> R.style.AppTheme_Light_Green_600
            -9920712 -> R.style.AppTheme_Light_Green_700
            -11171025 -> R.style.AppTheme_Light_Green_800
            -13407970 -> R.style.AppTheme_Light_Green_900

            -985917 -> R.style.AppTheme_Lime_100
            -1642852 -> R.style.AppTheme_Lime_200
            -2300043 -> R.style.AppTheme_Lime_300
            -2825897 -> R.style.AppTheme_Lime_400
            -3285959 -> R.style.AppTheme_Lime_500
            -4142541 -> R.style.AppTheme_Lime_600
            -5983189 -> R.style.AppTheme_Lime_700
            -6382300 -> R.style.AppTheme_Lime_800
            -8227049 -> R.style.AppTheme_Lime_900

            -1596 -> R.style.AppTheme_Yellow_100
            -2672 -> R.style.AppTheme_Yellow_200
            -3722 -> R.style.AppTheme_Yellow_300
            -4520 -> R.style.AppTheme_Yellow_400
            -5317 -> R.style.AppTheme_Yellow_500
            -141259 -> R.style.AppTheme_Yellow_600
            -278483 -> R.style.AppTheme_Yellow_700
            -415707 -> R.style.AppTheme_Yellow_800
            -688361 -> R.style.AppTheme_Yellow_900

            -4941 -> R.style.AppTheme_Amber_100
            -8062 -> R.style.AppTheme_Amber_200
            -10929 -> R.style.AppTheme_Amber_300
            -13784 -> R.style.AppTheme_Amber_400
            -16121 -> R.style.AppTheme_Amber_500
            -19712 -> R.style.AppTheme_Amber_600
            -24576 -> R.style.AppTheme_Amber_700
            -28928 -> R.style.AppTheme_Amber_800
            -37120 -> R.style.AppTheme_Amber_900

            -8014 -> R.style.AppTheme_Orange_100
            -13184 -> R.style.AppTheme_Orange_200
            -18611 -> R.style.AppTheme_Orange_300
            -22746 -> R.style.AppTheme_Orange_400
            -26624 -> R.style.AppTheme_Orange_500
            -291840 -> R.style.AppTheme_Orange_600
            -689152 -> R.style.AppTheme_Orange_700
            -1086464 -> R.style.AppTheme_Orange_800
            -1683200 -> R.style.AppTheme_Orange_900

            -13124 -> R.style.AppTheme_Deep_Orange_100
            -21615 -> R.style.AppTheme_Deep_Orange_200
            -30107 -> R.style.AppTheme_Deep_Orange_300
            -36797 -> R.style.AppTheme_Deep_Orange_400
            -43230 -> R.style.AppTheme_Deep_Orange_500
            -765666 -> R.style.AppTheme_Deep_Orange_600
            -1684967 -> R.style.AppTheme_Deep_Orange_700
            -2604267 -> R.style.AppTheme_Deep_Orange_800
            -4246004 -> R.style.AppTheme_Deep_Orange_900

            -2634552 -> R.style.AppTheme_Brown_100
            -4412764 -> R.style.AppTheme_Brown_200
            -6190977 -> R.style.AppTheme_Brown_300
            -7508381 -> R.style.AppTheme_Brown_400
            -8825528 -> R.style.AppTheme_Brown_500
            -9614271 -> R.style.AppTheme_Brown_600
            -10665929 -> R.style.AppTheme_Brown_700
            -11652050 -> R.style.AppTheme_Brown_800
            -12703965 -> R.style.AppTheme_Brown_900

            -3155748 -> R.style.AppTheme_Blue_Grey_100
            -5194811 -> R.style.AppTheme_Blue_Grey_200
            -7297874 -> R.style.AppTheme_Blue_Grey_300
            -8875876 -> R.style.AppTheme_Blue_Grey_400
            -10453621 -> R.style.AppTheme_Blue_Grey_500
            -11243910 -> R.style.AppTheme_Blue_Grey_600
            -12232092 -> R.style.AppTheme_Blue_Grey_700
            -13154481 -> R.style.AppTheme_Blue_Grey_800
            -14273992 -> R.style.AppTheme_Blue_Grey_900

            -1 -> R.style.AppTheme_Grey_100
            -1118482 -> R.style.AppTheme_Grey_200
            -2039584 -> R.style.AppTheme_Grey_300
            -4342339 -> R.style.AppTheme_Grey_400
            -6381922 -> R.style.AppTheme_Grey_500
            -9079435 -> R.style.AppTheme_Grey_600
            -10395295 -> R.style.AppTheme_Grey_700
            -12434878 -> R.style.AppTheme_Grey_800
            -16777216 -> R.style.AppTheme_Grey_900

            else -> R.style.AppTheme_Orange_700
        }
    }
}
