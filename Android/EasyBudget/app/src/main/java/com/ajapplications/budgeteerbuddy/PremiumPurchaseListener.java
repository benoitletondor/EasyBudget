/*
 *   Copyright 2015 Benoit LETONDOR
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

package com.ajapplications.budgeteerbuddy;

/**
 * Listener for in-app purchase buying flow
 *
 * @author Benoit LETONDOR
 */
public interface PremiumPurchaseListener
{
    /**
     * Called when the user cancel the purchase
     */
    void onUserCancelled();

    /**
     * Called when an error occurred during the iab flow
     *
     * @param error the error description
     */
    void onPurchaseError(String error);

    /**
     * Called on success
     */
    void onPurchaseSuccess();
}
