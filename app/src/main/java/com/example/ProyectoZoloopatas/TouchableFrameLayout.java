package com.example.ProyectoZoloopatas;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

/**
 * Contenedor táctil para el mapa que evita que el NestedScrollView padre
 * capture los gestos de arrastre, zoom y desplazamiento del mapa.
 */
public class TouchableFrameLayout extends FrameLayout {

    private NestedScrollView nestedScrollView;

    public TouchableFrameLayout(@NonNull Context context) {
        super(context);
    }

    public TouchableFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TouchableFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setNestedScrollView(NestedScrollView scrollView) {
        this.nestedScrollView = scrollView;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                // Bloquear el scroll del NestedScrollView para que el mapa reciba todos los gestos
                if (nestedScrollView != null) {
                    nestedScrollView.requestDisallowInterceptTouchEvent(true);
                }
                getParent().requestDisallowInterceptTouchEvent(true);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Desbloquear el scroll del NestedScrollView al soltar el dedo
                if (nestedScrollView != null) {
                    nestedScrollView.requestDisallowInterceptTouchEvent(false);
                }
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        return super.dispatchTouchEvent(ev);
    }
}
