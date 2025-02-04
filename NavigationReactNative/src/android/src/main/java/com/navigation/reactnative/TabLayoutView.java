package com.navigation.reactnative;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.tabs.TabLayout;

public class TabLayoutView extends TabLayout implements TabView {
    boolean bottomTabs;
    int defaultTextColor;
    int selectedTintColor;
    int unselectedTintColor;
    int defaultRippleColor;
    private boolean layoutRequested = false;
    private boolean measured = false;
    private OnTabSelectedListener tabSelectedListener;

    public TabLayoutView(Context context) {
        super(context);
        ViewCompat.setLayoutDirection(this, ViewCompat.LAYOUT_DIRECTION_LTR);
        AppBarLayout.LayoutParams params = new AppBarLayout.LayoutParams(AppBarLayout.LayoutParams.MATCH_PARENT, AppBarLayout.LayoutParams.WRAP_CONTENT);
        params.setScrollFlags(0);
        setLayoutParams(params);
        if (getTabTextColors() != null)
            selectedTintColor = unselectedTintColor = defaultTextColor = getTabTextColors().getDefaultColor();
        setSelectedTabIndicatorColor(defaultTextColor);
        defaultRippleColor = getTabRippleColor() != null ? getTabRippleColor().getColorForState(new int[]{ android.R.attr.state_pressed }, Color.WHITE) : Color.WHITE;
    }

    public void setScrollable(boolean scrollable) {
        setTabMode(scrollable ? TabLayout.MODE_SCROLLABLE : TabLayout.MODE_FIXED);
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        super.setLayoutParams(params);
        if (params instanceof CollapsingToolbarLayout.LayoutParams) {
            ((CollapsingToolbarLayout.LayoutParams) params).gravity = Gravity.BOTTOM;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.setVisibility(this.getTabCount() > 0 ? View.VISIBLE : View.INVISIBLE);
        TabBarPagerView tabBar = getTabBar();
        if (bottomTabs && tabBar != null) {
            setupWithViewPager(tabBar);
            tabBar.populateTabs();
        }
    }

    private TabBarPagerView getTabBar() {
        for(int i = 0; getParent() != null && i < ((ViewGroup) getParent()).getChildCount(); i++) {
            View child = ((ViewGroup) getParent()).getChildAt(i);
            if (child instanceof TabBarPagerView)
                return (TabBarPagerView) child;
        }
        return null;
    }

    @Override
    public void setupWithViewPager(@Nullable final ViewPager viewPager) {
        super.setupWithViewPager(viewPager);
        setVisibility(View.VISIBLE);
        if (tabSelectedListener != null)
            removeOnTabSelectedListener(tabSelectedListener);
        tabSelectedListener = new OnTabSelectedListener() {
            @Override
            public void onTabSelected(Tab tab) {
            }

            @Override
            public void onTabUnselected(Tab tab) {
            }

            @Override
            public void onTabReselected(Tab tab) {
                if (viewPager != null)
                    ((TabBarPagerView) viewPager).scrollToTop();
            }
        };
        addOnTabSelectedListener(tabSelectedListener);
        if (!measured) {
            measure(
                MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY));
            layout(getLeft(), getTop(), getRight(), getBottom());
            measured = true;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (getParent() instanceof CollapsingBarView) {
            CollapsingBarView parent = (CollapsingBarView) getParent();
            for(int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                if (child instanceof ToolbarView) {
                    if (child.getLayoutParams() instanceof CollapsingToolbarLayout.LayoutParams)
                        ((CollapsingToolbarLayout.LayoutParams) child.getLayoutParams()).setMargins(0, 0, 0, h);
                }
            }
        }
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
        if (!layoutRequested) {
            layoutRequested = true;
            post(measureAndLayout);
        }
    }

    private final Runnable measureAndLayout = () -> {
        layoutRequested = false;
        measure(
            MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY));
        layout(getLeft(), getTop(), getRight(), getBottom());
    };

    @Override
    public void setTitle(int index, CharSequence title) {
        TabLayout.Tab tab = getTabAt(index);
        if (tab != null)
            tab.setText(title);
    }

    public void setIcon(int index, Drawable icon) {
        TabLayout.Tab tab = getTabAt(index);
        if (tab != null)
            tab.setIcon(icon);
    }

    @Override
    public void setTestID(int index, String testID) {
        TabLayout.Tab tab = getTabAt(index);
        if (tab != null)
            tab.view.setTag(testID);
    }

    @Override
    public BadgeDrawable getBadgeIcon(int index) {
        TabLayout.Tab tab = getTabAt(index);
        return tab != null ? tab.getOrCreateBadge() : null;
    }

    @Override
    public void removeBadgeIcon(int index) {
        TabLayout.Tab tab = getTabAt(index);
        if (tab != null)
            tab.removeBadge();
    }
}
