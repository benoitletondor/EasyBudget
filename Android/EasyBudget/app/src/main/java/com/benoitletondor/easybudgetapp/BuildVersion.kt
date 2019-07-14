/*
 *   Copyright 2019 Benoit LETONDOR
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

package com.benoitletondor.easybudgetapp

/**
 * Static class that references public versions of the app
 *
 * @author Benoit LETONDOR
 */
object BuildVersion {
    /**
     * First public release.
     * 02/12/2015
     */
    const val VERSION_1 = 20
    /**
     * Add proguard minify and update dependencies.
     * 09/12/2015
     */
    const val VERSION_1_0_1 = 21
    /**
     * Add a warning that decimal values are not supported by the app.
     * 14/12/2015
     */
    const val VERSION_1_0_2 = 22
    /**
     * Translations & bug fixes.
     * 14/12/2015
     */
    const val VERSION_1_0_3 = 23
    /**
     * Fix amount rounding & display. Add premium status in settings. Add opt-out from update pushes.
     * 23/12/2015
     */
    const val VERSION_1_1_3 = 28
    /**
     * Add daily reminder pushes opt-out from settings & premium iAP.
     * 30/12/2015
     */
    const val VERSION_1_2 = 35
    /**
     * Add AppTurbo promotion code.
     * 01/02/2016
     */
    const val VERSION_1_2_1 = 36
    /**
     * Add monthly report & fix rounding bug
     * 23/02/2O16
     */
    const val VERSION_1_3 = 39
    /**
     * Bug fixes for rounding, and AppCompat 23.2
     * 05/03/2016
     */
    const val VERSION_1_3_1 = 40
    /**
     * Android Nougat compatibility and bug fixes for premium users
     * 01/07/2016
     */
    const val VERSION_1_4 = 42
    /**
     * PlayStore promo codes and weekly, bi-weekly and yearly recurring entries
     * 21/05/2017
     */
    const val VERSION_1_5_2 = 45
    /**
     * First public release of 2.0
     * 01/07/2019
     */
    const val VERSION_2_0_10 = 63
    /**
     * Second public release of 2.0
     * 12/07/2019
     */
    const val VERSION_2_0_11 = 64
    /**
     * Third public release of 2.0
     * 13/07/2019
     */
    const val VERSION_2_0_12 = 65
    /**
     * Bug fix for new balance parsing
     * 13/07/2019
     */
    const val VERSION_2_0_13 = 66
}
