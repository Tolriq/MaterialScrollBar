/*
 * Copyright © 2015, Turing Technologies, an unincorporated organisation of Wynne Plaga
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.turingtechnologies.materialscrollbar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;

import java.util.Locale;

/**
 * Indicator which should be used when only one character will be displayed at a time.
 */
@SuppressLint("ViewConstructor")
public class AlphabetIndicator extends Indicator {

    public AlphabetIndicator(Context c) {
        super(c);
    }

    @Override
    String getTextElement(Integer currentSection, RecyclerView.Adapter adapter) {
        String character = ((INameableAdapter) adapter).getCharacterForElement(currentSection).toString();
        if (TextUtils.isEmpty(character)) {
            return null;
        }
        return character.toUpperCase(Locale.getDefault());
    }

    @Override
    int getIndicatorHeight() {
        return 100;
    }

    @Override
    int getIndicatorWidth() {
        return 100;
    }

    @Override
    void testAdapter(RecyclerView.Adapter adapter) {
        if (!(adapter instanceof INameableAdapter)) {
            throw new adapterNotSetupForIndicatorException("INameableAdapter");
        }
    }

    @Override
    int getTextSize() {
        return 50;
    }

}