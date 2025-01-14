package com.navigation.reactnative;

import androidx.annotation.Nullable;

import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.ReactStylesDiffMap;
import com.facebook.react.uimanager.StateWrapper;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.ViewManagerDelegate;
import com.facebook.react.viewmanagers.NVTitleBarManagerDelegate;
import com.facebook.react.viewmanagers.NVTitleBarManagerInterface;

import java.util.Map;

import javax.annotation.Nonnull;

public class TitleBarViewManager extends ViewGroupManager<TitleBarView> implements NVTitleBarManagerInterface<TitleBarView> {
    private final ViewManagerDelegate<TitleBarView> delegate;

    public TitleBarViewManager() {
        delegate = new NVTitleBarManagerDelegate<>(this);
    }

    @Nullable
    @Override
    protected ViewManagerDelegate<TitleBarView> getDelegate() {
        return delegate;
    }

    @Nonnull
    @Override
    public String getName() {
        return "NVTitleBar";
    }

    @Nonnull
    @Override
    protected TitleBarView createViewInstance(@Nonnull ThemedReactContext reactContext) {
        return new TitleBarView(reactContext);
    }

    @Override
    public Object updateState(
            TitleBarView view, ReactStylesDiffMap props, StateWrapper stateWrapper) {
        view.getFabricViewStateManager().setStateWrapper(stateWrapper);
        return null;
    }
}
