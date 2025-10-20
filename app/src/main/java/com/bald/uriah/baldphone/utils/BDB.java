/*
 * Copyright 2019 Uriah Shaul Mandel
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

package com.bald.uriah.baldphone.utils;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.bald.uriah.baldphone.activities.BaldActivity;

/**
 * Builder for {@link BDialog}
 */
public class BDB {
    public Context context;

    @NonNull
    CharSequence title = "";
    CharSequence subText;
    CharSequence[] options;
    int inputType;
    BDialog.DialogBoxListener positiveButtonListener = BDialog.DialogBoxListener.EMPTY;
    BDialog.DialogBoxListener negativeButtonListener = BDialog.DialogBoxListener.EMPTY;
    @NonNull
    BDialog.StartingIndexChooser startingIndexChooser = () -> 0;
    @Nullable
    BaldActivity baldActivityToAutoDismiss;
    @Nullable
    View extraView;
    @Nullable
    CharSequence negativeCustomText;
    @Nullable
    CharSequence positiveCustomText;
    int flags;

    private BDB() {
    }

    public static BDB from(@Nullable Context context) {
        BDB bdb = new BDB();
        bdb.context = context;
        if (context instanceof BaldActivity) {
            bdb.baldActivityToAutoDismiss = (BaldActivity) context;
        }
        return bdb;
    }

    public BDB setTitle(CharSequence title) {
        this.title = title;
        return this;
    }

    public BDB setTitle(@StringRes int titleId) {
        return setTitle(context.getText(titleId));
    }

    public BDB setSubText(CharSequence subText) {
        this.subText = subText;
        return this;
    }

    public BDB setSubText(@StringRes int subTextId) {
        return setSubText(context.getText(subTextId));
    }

    public BDB setOptions(CharSequence... options) {
        flags |= BDialog.FLAG_OPTIONS;
        this.options = options;
        return this;
    }

    public BDB setOptions(@StringRes int... options) {
        final CharSequence[] charSequences = new CharSequence[options.length];
        for (int i = 0; i < options.length; i++)
            charSequences[i] = context.getText(options[i]);

        return setOptions(charSequences);
    }

    public BDB setPositiveButtonListener(@Nullable BDialog.DialogBoxListener dialogBoxListener) {
        this.positiveButtonListener = dialogBoxListener;
        return addFlag(BDialog.FLAG_POSITIVE);
    }

    public BDB setNegativeButtonListener(@Nullable BDialog.DialogBoxListener dialogBoxListener) {
        this.negativeButtonListener = dialogBoxListener;
        return addFlag(BDialog.FLAG_NEGATIVE);
    }

    public BDB setInputType(int inputType) {
        this.inputType = inputType;
        return addFlag(BDialog.FLAG_INPUT);
    }

    public BDB setOptionsStartingIndex(BDialog.StartingIndexChooser startingIndexChooser) {
        this.startingIndexChooser = startingIndexChooser;
        return this;
    }

    public BDB setExtraView(@Nullable View extraView) {
        this.extraView = extraView;
        return this;
    }

    public BDB setBaldActivityToAutoDismiss(BaldActivity baldActivityToAutoDismiss) {
        this.baldActivityToAutoDismiss = baldActivityToAutoDismiss;
        return this;
    }

    public BDB setNegativeCustomText(@StringRes int negativeCustomText) {
        this.negativeCustomText = context.getText(negativeCustomText);
        return addFlag(BDialog.FLAG_CUSTOM_NEGATIVE);
    }

    public BDB setPositiveCustomText(@StringRes int positiveCustomText) {
        this.positiveCustomText = context.getText(positiveCustomText);
        return addFlag(BDialog.FLAG_CUSTOM_POSITIVE);
    }

    public BDB addFlag(int flags) {
        this.flags |= flags;
        return this;
    }

    public BDialog show() {
        return BDialog.newInstance(this);
    }
}