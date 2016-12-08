package com.calculator.aa.ui;

import javax.swing.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

final class SpinnerWheelListener implements MouseWheelListener {

    private final JSpinner Spinner;

    SpinnerWheelListener(JSpinner spinner) {
        Spinner = spinner;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (!Spinner.isEnabled() || e.getScrollType() != MouseWheelEvent.WHEEL_UNIT_SCROLL) {
            return;
        }

        SpinnerModel sm = Spinner.getModel();
        Object next;

        next = e.getWheelRotation() < 0 ? sm.getNextValue() : sm.getPreviousValue();

        if (next != null) {
            Spinner.setValue(next);
        }
    }
}
