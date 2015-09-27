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
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

@SuppressWarnings("unused")
@SuppressLint("ViewConstructor")
public class MaterialScrollBar extends RelativeLayout {

    private View background;
    private View handle;
    int handleColour;
    private int handleOffColour = Color.parseColor("#9c9c9c");
    private boolean hidden = true;
    private int hideDuration = 2500;
    private boolean hide = true;
    private RecyclerView recyclerView;
    private Indicator indicator;
    private int textColour = ContextCompat.getColor(getContext(), android.R.color.white);
    private boolean lightOnTouch;
    private boolean totallyHidden = false;
    private Handler mUIHandler = new Handler(Looper.getMainLooper());

    private Runnable mFadeBar = new Runnable() {
        @Override
        public void run() {
            fadeOut();
        }
    };

    /**
     * For testing only. Should not generally be accessed.
     */
    public boolean getHidden() {
        return hidden;
    }

    /**
     * For testing only. Should not generally be accessed.
     */
    public String getIndicatorText() {
        return (String) indicator.textView.getText();
    }

    /**
     * @param context      The app's context
     * @param recyclerView The recyclerView to which you wish to link the scrollBar
     * @param lightOnTouch Should the handle always be coloured or should it light up on touch and turn grey when released
     */
    public MaterialScrollBar(Context context, RecyclerView recyclerView, boolean lightOnTouch) {
        super(context);

        background = new View(context);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(Utils.getDP(8, this), LayoutParams.MATCH_PARENT);
        lp.addRule(ALIGN_PARENT_RIGHT);
        background.setLayoutParams(lp);

        background.setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray));
        background.setAlpha(0.4F);

        handle = new View(context);
        lp = new RelativeLayout.LayoutParams(Utils.getDP(8, this), Utils.getDP(48, this));
        lp.addRule(ALIGN_PARENT_RIGHT);
        handle.setLayoutParams(lp);

        this.lightOnTouch = lightOnTouch;
        int colourToSet;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            handleColour = fetchAccentColour(context);
        } else {
            handleColour = Color.parseColor("#9c9c9c");
        }
        if (lightOnTouch) {
            colourToSet = Color.parseColor("#9c9c9c");
        } else {
            colourToSet = handleColour;
        }
        handle.setBackgroundColor(colourToSet);

        addView(background);
        addView(handle);

        setId(R.id.reservedNamedId);
        if (recyclerView.getParent() instanceof RelativeLayout) {
            LayoutParams layoutParams = new LayoutParams(Utils.getDP(20, this), ViewGroup.LayoutParams.MATCH_PARENT);
            layoutParams.addRule(ALIGN_RIGHT, recyclerView.getId());
            layoutParams.addRule(ALIGN_TOP, recyclerView.getId());
            layoutParams.addRule(ALIGN_BOTTOM, recyclerView.getId());
            ((ViewGroup) recyclerView.getParent()).addView(this, layoutParams);
        } else {
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(Utils.getDP(20, this), ViewGroup.LayoutParams.MATCH_PARENT);
            layoutParams.gravity = Gravity.RIGHT;
            ((ViewGroup) recyclerView.getParent()).addView(this, layoutParams);
        }
        recyclerView.addOnScrollListener(new ScrollListener(this));
        this.recyclerView = recyclerView;

        setTouchIntercept();

        TranslateAnimation anim = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f);
        anim.setFillAfter(true);
        startAnimation(anim);
    }

    private void setTouchIntercept() {
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!totallyHidden) {
                    if (event.getAction() != MotionEvent.ACTION_UP) {
                        recyclerView.scrollToPosition((int) (recyclerView.getAdapter().getItemCount() * (event.getY() / (getHeight() - handle.getHeight()))));
                        recyclerView.onScrolled(0, 0);
                        if (indicator != null && indicator.getVisibility() == INVISIBLE) {
                            indicator.setVisibility(VISIBLE);
                        }

                        if (lightOnTouch) {
                            handle.setBackgroundColor(handleColour);
                        }

                        mUIHandler.removeCallbacks(mFadeBar);
                        fadeIn();
                    } else {
                        if (indicator != null && indicator.getVisibility() == VISIBLE) {
                            if (Build.VERSION.SDK_INT <= 12) {
                                indicator.clearAnimation();
                            }
                            indicator.setVisibility(INVISIBLE);
                        }

                        if (lightOnTouch) {
                            handle.setBackgroundColor(handleOffColour);
                        }

                        if (hide) {
                            mUIHandler.removeCallbacks(mFadeBar);
                            mUIHandler.postDelayed(mFadeBar, hideDuration);
                        }
                    }
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Provides the ability to programmatically set the hide duration of the scrollbar.
     *
     * @param duration for the bar to remain visible after inactivity before hiding.
     */
    public MaterialScrollBar setHideDuration(int duration) {
        this.hideDuration = duration;
        return this;
    }

    /**
     * Provides the ability to programmatically set the colour of the scrollbar handle.
     *
     * @param colour to set the handle.
     */
    public MaterialScrollBar setHandleColour(String colour) {
        handleColour = Color.parseColor(colour);
        setHandleColour();
        return this;
    }

    /**
     * Provides the ability to programmatically set the colour of the scrollbar handle.
     *
     * @param colour to set the handle.
     */
    public MaterialScrollBar setHandleColour(int colour) {
        handleColour = colour;
        setHandleColour();
        return this;
    }

    /**
     * Provides the ability to programmatically set the colour of the scrollbar handle.
     *
     * @param colourResId to set the handle.
     */
    public MaterialScrollBar setHandleColourRes(int colourResId) {
        handleColour = ContextCompat.getColor(getContext(), colourResId);
        setHandleColour();
        return this;
    }

    private void setHandleColour() {
        if (indicator != null) {
            ((GradientDrawable) indicator.getBackground()).setColor(handleColour);
        }
        if (!lightOnTouch) {
            handle.setBackgroundColor(handleColour);
        }
    }

    /**
     * Provides the ability to programmatically set the colour of the scrollbar handle when unpressed. Only applies if lightOnTouch is true.
     *
     * @param colour to set the handle when unpressed.
     */
    public MaterialScrollBar setHandleOffColour(String colour) {
        handleOffColour = Color.parseColor(colour);
        if (lightOnTouch) {
            handle.setBackgroundColor(handleOffColour);
        }
        return this;
    }

    /**
     * Provides the ability to programmatically set the colour of the scrollbar handle when unpressed. Only applies if lightOnTouch is true.
     *
     * @param colour to set the handle when unpressed.
     */
    public MaterialScrollBar setHandleOffColour(int colour) {
        handleOffColour = colour;
        if (lightOnTouch) {
            handle.setBackgroundColor(handleOffColour);
        }
        return this;
    }

    /**
     * Provides the ability to programmatically set the colour of the scrollbar handle when unpressed. Only applies if lightOnTouch is true.
     *
     * @param colourResId to set the handle when unpressed.
     */
    public MaterialScrollBar setHandleOffColourRes(int colourResId) {
        handleOffColour = ContextCompat.getColor(getContext(), colourResId);
        if (lightOnTouch) {
            handle.setBackgroundColor(handleOffColour);
        }
        return this;
    }

    /**
     * Provides the ability to programmatically set the colour of the scrollbar.
     *
     * @param colour to set the bar.
     */
    public MaterialScrollBar setBarColour(String colour) {
        background.setBackgroundColor(Color.parseColor(colour));
        return this;
    }

    /**
     * Provides the ability to programmatically set the colour of the scrollbar.
     *
     * @param colour to set the bar.
     */
    public MaterialScrollBar setBarColour(int colour) {
        background.setBackgroundColor(colour);
        return this;
    }

    /**
     * Provides the ability to programmatically set the colour of the scrollbar.
     *
     * @param colourResId to set the bar.
     */
    public MaterialScrollBar setBarColourRes(int colourResId) {
        background.setBackgroundColor(ContextCompat.getColor(getContext(), colourResId));
        return this;
    }

    /**
     * Provides the ability to programmatically set the text colour of the indicator. Will do nothing if there is no section indicator.
     *
     * @param colour to set the text of the indicator.
     */
    public MaterialScrollBar setTextColour(int colour) {
        textColour = colour;
        if (indicator != null) {
            indicator.setTextColour(textColour);
        }
        return this;
    }


    /**
     * Provides the ability to programmatically set the text colour of the indicator. Will do nothing if there is no section indicator.
     *
     * @param colourResId to set the text of the indicator.
     */
    public MaterialScrollBar setTextColourRes(int colourResId) {
        textColour = ContextCompat.getColor(getContext(), colourResId);
        if (indicator != null) {
            indicator.setTextColour(textColour);
        }
        return this;
    }

    /**
     * Provides the ability to programmatically set the text colour of the indicator. Will do nothing if there is no section indicator.
     *
     * @param colour to set the text of the indicator.
     */
    public MaterialScrollBar setTextColour(String colour) {
        textColour = Color.parseColor(colour);
        if (indicator != null) {
            indicator.setTextColour(textColour);
        }
        return this;
    }

    /**
     * Provides the ability to programmatically alter whether the scrollbar
     * should hide after a period of inactivity or not.
     *
     * @param hide sets whether the bar should hide or not.
     */
    public MaterialScrollBar setAutoHide(Boolean hide) {
        if (!hide) {
            mUIHandler.removeCallbacks(mFadeBar);
            TranslateAnimation anim = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f);
            anim.setFillAfter(true);
            startAnimation(anim);
        }
        this.hide = hide;
        return this;
    }

    /**
     * Removes any indicator.
     */
    public MaterialScrollBar removeIndicator() {
        this.indicator = null;
        return this;
    }

    /**
     * Adds an indicator which accompanies this scroll bar.
     */
    public MaterialScrollBar addIndicator(Indicator indicator) {
        indicator.testAdapter(recyclerView.getAdapter());
        this.indicator = indicator;
        indicator.linkToScrollBar(this);
        indicator.setTextColour(textColour);
        return this;
    }

    /**
     * Allows the developer to set a custom bar thickness.
     *
     * @param thickness The desired bar thickness.
     */
    public MaterialScrollBar setBarThickness(int thickness) {
        thickness = Utils.getDP(thickness, this);
        LayoutParams layoutParams = (LayoutParams) handle.getLayoutParams();
        layoutParams.width = thickness;
        handle.setLayoutParams(layoutParams);

        layoutParams = (LayoutParams) background.getLayoutParams();
        layoutParams.width = thickness;
        background.setLayoutParams(layoutParams);

        if (indicator != null) {
            LayoutParams lp = (LayoutParams) indicator.getLayoutParams();
            lp.setMargins(0, 0, handle.getLayoutParams().width, 0);
            indicator.setLayoutParams(lp);
        }
        return this;
    }

    //Fetch accent colour on devices running Lollipop or newer.
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private int fetchAccentColour(Context context) {
        TypedValue typedValue = new TypedValue();

        TypedArray a = context.obtainStyledAttributes(typedValue.data, new int[]{android.R.attr.colorAccent});
        int colour = a.getColor(0, 0);

        a.recycle();

        return colour;
    }

    /**
     * Animates the bar out of view
     */
    private void fadeOut() {
        if (!hidden) {
            TranslateAnimation anim = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f);
            anim.setDuration(500);
            anim.setFillAfter(true);
            hidden = true;
            startAnimation(anim);
        }
    }

    /**
     * Animates the bar into view
     */
    private void fadeIn() {
        if (hidden && hide && !totallyHidden) {
            hidden = false;
            TranslateAnimation anim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f);
            anim.setDuration(500);
            anim.setFillAfter(true);
            startAnimation(anim);
        }
    }

    /**
     * Hide or unhide the scrollBar.
     */
    public void setScrollBarHidden(boolean hidden) {
        totallyHidden = hidden;
        fadeOut();
    }

    private class ScrollListener extends RecyclerView.OnScrollListener {

        MaterialScrollBar materialScrollBar;

        ScrollListener(MaterialScrollBar msb) {
            materialScrollBar = msb;
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            float scroll = calculateScrollProgress(recyclerView) * (materialScrollBar.getHeight() - handle.getHeight());
            if (scroll >= 0) {
                handle.setY(scroll);
                if (indicator != null && indicator.getVisibility() == VISIBLE) {
                    indicator.setScroll(scroll);
                }
            }
        }

        public float calculateScrollProgress(RecyclerView recyclerView) {
            RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
            int itemPerLines = 1;
            if (manager instanceof GridLayoutManager) {
                itemPerLines = ((GridLayoutManager) manager).getSpanCount();
            }

            LinearLayoutManager layoutManager = (LinearLayoutManager) manager;
            int lastFullyVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition();
            if (lastFullyVisiblePosition == RecyclerView.NO_POSITION) {
                return -1;
            }

            View visibleChild = recyclerView.getChildAt(0);
            if (visibleChild == null) {
                return 0;
            }
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(visibleChild);
            int itemHeight = holder.itemView.getHeight();
            int recyclerHeight = recyclerView.getHeight();
            int itemsInWindow = (recyclerHeight / itemHeight) * itemPerLines;

            int numItemsInList = recyclerView.getAdapter().getItemCount();
            int numScrollableSectionsInList = numItemsInList - itemsInWindow;
            int indexOfLastFullyVisibleItemInFirstSection = numItemsInList - numScrollableSectionsInList - 1;

            int currentSection = lastFullyVisiblePosition - indexOfLastFullyVisibleItemInFirstSection;
            if (indicator != null && indicator.getVisibility() == VISIBLE) {
                indicator.textView.setText(indicator.getTextElement(currentSection, recyclerView.getAdapter()));
            }
            return (float) currentSection / numScrollableSectionsInList;
        }

        @Override
        public void onScrollStateChanged(final RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);

            if (hide) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mUIHandler.removeCallbacks(mFadeBar);
                    mUIHandler.postDelayed(mFadeBar, hideDuration);
                } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    if (recyclerView.canScrollVertically(1) || recyclerView.canScrollVertically(-1) || recyclerView.canScrollHorizontally(1) || recyclerView.canScrollHorizontally(-1)) {
                        mUIHandler.removeCallbacks(mFadeBar);
                        materialScrollBar.fadeIn();
                    }
                }
            }
        }
    }
}