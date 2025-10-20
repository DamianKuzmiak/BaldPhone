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

package com.bald.uriah.baldphone.adapters;

import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import app.baldphone.neo.contacts.ContactInfoActivity;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.BaldActivity;
import com.bald.uriah.baldphone.activities.SOSActivity;
import com.bald.uriah.baldphone.activities.contacts.ShareActivity;
import com.bald.uriah.baldphone.fragments_and_dialogs.LetterChooserDialog;
import com.bald.uriah.baldphone.views.ModularRecyclerView;
import com.bumptech.glide.Glide;

public class ContactRecyclerViewAdapter extends ModularRecyclerView.ModularAdapter<ContactRecyclerViewAdapter.ViewHolder> {
    public final static String[] PROJECTION = {
            ContactsContract.Data.DISPLAY_NAME,
            ContactsContract.Data._ID,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Data.LOOKUP_KEY,
            ContactsContract.Data.STARRED};
    public static final int MODE_DEFAULT = 0;
    public static final int MODE_SOS = 1;
    public static final int MODE_SHARE = 2;
    private final BaldActivity activity;
    private final LayoutInflater layoutInflater;
    private final SparseIntArray letterToPosition;
    private final RecyclerView recyclerView;
    private final int mode;
    @ColorInt
    private final int textColorOnGold, textColorOnButton;
    private Cursor cursor;

    public ContactRecyclerViewAdapter(BaldActivity activity, Cursor cursor, RecyclerView recyclerView, @IntRange(from = MODE_DEFAULT, to = MODE_SHARE) int mode) {
        this.mode = mode;
        this.activity = activity;
        this.layoutInflater = LayoutInflater.from(activity);
        this.cursor = cursor;
        this.recyclerView = recyclerView;
        final TypedValue typedValue = new TypedValue();
        final Resources.Theme theme = activity.getTheme();
        theme.resolveAttribute(R.attr.bald_background, typedValue, true);
        letterToPosition = new SparseIntArray();
        theme.resolveAttribute(R.attr.bald_text_on_gold, typedValue, true);
        textColorOnGold = typedValue.data;
        theme.resolveAttribute(R.attr.bald_text_on_button, typedValue, true);
        textColorOnButton = typedValue.data;

        applyToCursor();
    }

    private void applyToCursor() {
        letterToPosition.clear();
        cursor.moveToFirst();
        String previousFirstLetter = null;
        String tmpThis;

        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            tmpThis = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME)).substring(0, 1).toUpperCase();
            if (!tmpThis.equals(previousFirstLetter)) {
                letterToPosition.append(tmpThis.charAt(0), i);
            }
            previousFirstLetter = tmpThis;
        }
    }

    public void changeCursor(@NonNull Cursor cursor) {
        this.cursor = cursor;
        applyToCursor();
        notifyDataSetChanged();

        if (getItemCount() > 0) {
            RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(activity) {
                @Override
                protected int getVerticalSnapPreference() {
                    return LinearSmoothScroller.SNAP_TO_START;
                }
            };
            smoothScroller.setTargetPosition(0);
            recyclerView.getLayoutManager().startSmoothScroll(smoothScroller);
        }

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = layoutInflater.inflate(R.layout.contact_row_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final @NonNull ViewHolder holder, final int position) {
        super.onBindViewHolder(holder, position);
        cursor.moveToPosition(position);

        holder.lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.LOOKUP_KEY));
        final String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
        final String letter = name.substring(0, 1).toUpperCase();

        int columnIndex1 = cursor.getColumnIndex(ContactsContract.Data.STARRED);
        holder.setFavorite(cursor.getInt(columnIndex1) == 1);

        holder.tv_contact_name.setText(name);

        if (position == 0) {
            holder.setLetter(name.substring(0, 1));
        } else {
            final boolean moved = cursor.moveToPosition(position - 1);
            if (moved) {
                final String prevNameFirstLetter = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME)).substring(0, 1).toUpperCase();
                if (!prevNameFirstLetter.equals(letter)) {
                    holder.setLetter(letter);
                } else
                    holder.setLetter(null);
            } else {
                holder.setLetter(null);
            }
            cursor.moveToPosition(position);
        }

        int columnIndex = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI);
        if (cursor.getString(columnIndex) != null) {
            holder.iv_contact_pic.setImageURI(Uri.parse(cursor.getString(columnIndex)));
        } else {
            Glide.with(holder.iv_contact_pic).load(R.drawable.face).into(holder.iv_contact_pic);
        }
    }

    @Override
    public int getItemCount() {
        return cursor.getCount();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final View line;
        final LinearLayout container, ll_contact_only;
        final TextView tv_contact_name;
        final TextView tv_letter;
        final ImageView iv_contact_pic;

        String lookupKey;
        private boolean expanded;
        private boolean favorite;

        public ViewHolder(final View itemView) {
            super(itemView);
            this.container = (LinearLayout) itemView;
            this.ll_contact_only = container.findViewById(R.id.ll_contact_only);
            this.tv_contact_name = container.findViewById(R.id.contact_name);
            this.iv_contact_pic = container.findViewById(R.id.profile_pic);
            this.tv_letter = container.findViewById(R.id.letter);
            this.ll_contact_only.setOnClickListener(this);
            this.line = container.findViewById(R.id.line);

            this.tv_letter.setOnClickListener((v) -> {
                final LetterChooserDialog letterChooserDialog =
                        new LetterChooserDialog(activity, letterToPosition,
                                (position -> {
                                    ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(position, 0);
                                }
                                )
                        );
                letterChooserDialog.show();
                activity.autoDismiss(letterChooserDialog);
            });

        }

        public void setLetter(final @Nullable String character) {
            if (character == null && expanded) {
                RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) this.container.getLayoutParams();
                this.container.setLayoutParams(layoutParams);
                this.line.setVisibility(View.GONE);
                this.tv_letter.setVisibility(View.GONE);
                expanded = false;
            } else if (character != null && expanded) {
                this.tv_letter.setText(character.toUpperCase());
            } else if (character != null) {
                final RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) this.container.getLayoutParams();
                this.container.setLayoutParams(layoutParams);
                this.line.setVisibility(View.VISIBLE);
                this.tv_letter.setVisibility(View.VISIBLE);
                this.tv_letter.setText(character.toUpperCase());
                expanded = true;
            }
        }

        public void setFavorite(final boolean f) {
            if (f == favorite)
                return;
            favorite = f;
//            tv_contact_name.setCompoundDrawablesRelativeWithIntrinsicBounds(
//                    0, 0, favorite ? R.drawable.star_gold : 0, 0
//            );
            ll_contact_only.setBackgroundResource(favorite ? R.drawable.style_for_buttons_rectangle_gold : R.drawable.style_for_buttons_rectangle);
            tv_contact_name.setTextColor(favorite ? textColorOnGold : textColorOnButton);
        }

        @Override
        public void onClick(View v) {
            if (lookupKey == null)
                throw new IllegalStateException("lookupKey cannot be null!");
            switch (mode) {
                case MODE_DEFAULT:
                    final Intent intent = new Intent(activity, ContactInfoActivity.class)
                            .putExtra(ContactInfoActivity.CONTACT_LOOKUP_KEY, lookupKey);
                    activity.startActivityForResult(intent, ContactInfoActivity.REQUEST_CHECK_CHANGE);
                    break;
                case MODE_SOS:
                    SOSActivity.PinHelper.pinContact(v.getContext(), lookupKey);
                    activity.finish();
                    break;
                case MODE_SHARE:
                    ((ShareActivity) activity).whatsappShare(lookupKey);
                    break;
            }
        }
    }
}