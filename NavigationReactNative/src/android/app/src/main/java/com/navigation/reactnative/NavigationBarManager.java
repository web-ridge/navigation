package com.navigation.reactnative;

import android.view.MenuItem;
import android.view.View;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.Map;

import javax.annotation.Nonnull;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

public class NavigationBarManager extends ViewGroupManager<NavigationBarView> {
    @Nonnull
    @Override
    public String getName() {
        return "NVNavigationBar";
    }

    @Nonnull
    @Override
    protected NavigationBarView createViewInstance(@Nonnull ThemedReactContext reactContext) {
        return new NavigationBarView(reactContext);
    }

    @Override
    public int getChildCount(NavigationBarView parent) {
        return parent.titleViews.size();
    }

    @Override
    public View getChildAt(NavigationBarView parent, int index) {
        return parent.titleViews.get(index);
    }

    @Override
    public void addView(NavigationBarView parent, View child, int index) {
        Toolbar.LayoutParams layoutParams = new Toolbar.LayoutParams(
                Toolbar.LayoutParams.MATCH_PARENT,
                Toolbar.LayoutParams.MATCH_PARENT
        );
        parent.toolbar.addView(child, index, layoutParams);
        parent.titleViews.add(index, child);
    }

    @Override
    public void removeViewAt(NavigationBarView parent, int index) {
        parent.toolbar.removeViewAt(index);
        parent.titleViews.remove(index);
    }

    @ReactProp(name = "title")
    public void setTitle(NavigationBarView view, @Nullable String title) {
        view.setTitle(title);
    }

    @ReactProp(name = "logo")
    public void setLogo(NavigationBarView view, @Nullable ReadableMap logo) {
        view.setLogoSource(logo);
    }

    @ReactProp(name = "navIcon")
    public void setNavIcon(NavigationBarView view, @Nullable ReadableMap navIcon) {
        view.setNavIconSource(navIcon);
    }

    @ReactProp(name = "overflowIcon")
    public void setOverflowIcon(NavigationBarView view, @Nullable ReadableMap overflowIcon) {
        view.setOverflowIconSource(overflowIcon);
    }

    @ReactProp(name = "menuItems")
    public void setMenuItems(NavigationBarView view, @Nullable ReadableArray menuItems) {
        view.setMenuItems(menuItems);
    }

    @Override
    public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.<String, Object>builder()
                .put("onIconClicked", MapBuilder.of("registrationName", "onIconClicked"))
                .put("onActionSelected", MapBuilder.of("registrationName", "onActionSelected"))
                .build();
    }

    @Nullable
    @Override
    public Map<String, Object> getExportedViewConstants() {
        return MapBuilder.<String, Object>of(
                "ShowAsAction",
                MapBuilder.of(
                        "never", MenuItem.SHOW_AS_ACTION_NEVER,
                        "always", MenuItem.SHOW_AS_ACTION_ALWAYS,
                        "ifRoom", MenuItem.SHOW_AS_ACTION_IF_ROOM));
    }
}
