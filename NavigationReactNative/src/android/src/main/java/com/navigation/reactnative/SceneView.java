package com.navigation.reactnative;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.react.bridge.ReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import java.util.HashSet;

public class SceneView extends ViewGroup {
    protected String sceneKey;
    protected String enterAnim;
    protected String exitAnim;
    public final HashSet<SharedElementView> sharedElements = new HashSet<>();
    public SharedElementMotion sharedElementMotion;

    public SceneView(Context context) {
        super(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        View child = getChildAt(0);
        if (child != null && child.getClass().getSimpleName().contains("DrawerLayout")) {
            child.requestLayout();
            post(measureAndLayoutDrawer);
        }
    }

    private final Runnable measureAndLayoutDrawer = new Runnable() {
        @Override
        public void run() {
            View drawer = getChildAt(0);
            if (drawer == null) return;
            drawer.measure(
                MeasureSpec.makeMeasureSpec(drawer.getWidth(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(drawer.getHeight(), MeasureSpec.EXACTLY));
            drawer.layout(drawer.getLeft(), drawer.getTop(), drawer.getRight(), drawer.getBottom());
        }
    };

    protected void popped() {
        ReactContext reactContext = (ReactContext) getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(getId(),"onPopped", null);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
    }
}
